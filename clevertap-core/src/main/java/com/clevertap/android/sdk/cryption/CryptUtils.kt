package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.*
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.io.File
import java.util.*

object CryptUtils {

    /**
     * This method migrates the encryption level of the stored data for the current account ID
     *
     * @param context - The Android context
     * @param config  - The [CleverTapInstanceConfig] object
     * @param cryptHandler - The [CryptHandler] object
     */
    @JvmStatic
    fun migrateEncryptionLevel(
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ) {

        val encryptionFlagStatus: Int
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            -1
        )
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level from $storedEncryptionLevel to $configEncryptionLevel"
        )

        encryptionFlagStatus = if (storedEncryptionLevel == -1 && configEncryptionLevel == 0)
            return
        else if (storedEncryptionLevel != configEncryptionLevel) {
            ENCRYPTION_FLAG_FAIL
        } else {
            StorageHelper.getInt(
                context,
                StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
                ENCRYPTION_FLAG_FAIL
            )
        }
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevel
        )

        if (encryptionFlagStatus == 3) {
            config.logger.verbose(
                config.accountId,
                "Encryption flag status is 3, no need to migrate"
            )
            cryptHandler.encryptionFlagStatus = 3
            return
        }

        // If configEncryptionLevel is one then encrypt otherwise decrypt
        migrateEncryption(
            configEncryptionLevel == 1,
            context,
            config,
            cryptHandler,
            encryptionFlagStatus,
            dbAdapter
        )
    }

    private fun migrateEncryption(
        encrypt: Boolean,
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        encryptionFlagStatus: Int,
        dbAdapter: DBAdapter
    ) {
        var cgkFlag = encryptionFlagStatus and ENCRYPTION_FLAG_CGK_SUCCESS
        if (cgkFlag == ENCRYPTION_FLAG_FAIL)
            cgkFlag = migrateCachedGuidsKeyPref(encrypt, config, context, cryptHandler)

        var knFlag = encryptionFlagStatus and ENCRYPTION_FLAG_KN_SUCCESS
        if (knFlag == ENCRYPTION_FLAG_FAIL)
            knFlag = migrateARPPreferenceFiles(encrypt, config, context, cryptHandler)

        var dbFlag = encryptionFlagStatus and ENCRYPTION_FLAG_DB_SUCCESS
        if (dbFlag == ENCRYPTION_FLAG_FAIL)
            dbFlag = migrateDBProfile(encrypt, config, cryptHandler, dbAdapter)

        val updatedFlagStatus = cgkFlag or knFlag or dbFlag

        config.logger.verbose(
            config.accountId,
            "Updating encryption flag status to $updatedFlagStatus"
        )
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
            updatedFlagStatus
        )
        cryptHandler.encryptionFlagStatus = updatedFlagStatus
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param context - The Android context
     * @param cryptHandler - The [CryptHandler] object
     * Returns the status for migration
     */
    private fun migrateCachedGuidsKeyPref(
        encrypt: Boolean,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler
    ): Int {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )
        val json =
            StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        val newGuidJsonObj = JSONObject()
        var migrationStatus = ENCRYPTION_FLAG_CGK_SUCCESS
        try {
            val i = cachedGuidJsonObj.keys()
            while (i.hasNext()) {
                val nextJSONObjKey = i.next()
                val key = nextJSONObjKey.substring(0, nextJSONObjKey.indexOf("_"))
                val identifier = nextJSONObjKey.substring(nextJSONObjKey.indexOf("_") + 1)
                var crypted: String? =
                    if (encrypt)
                        cryptHandler.encrypt(identifier, key)
                    else
                        cryptHandler.decrypt(identifier, KEY_ENCRYPTION_MIGRATION)
                if (crypted == null) {
                    config.logger.verbose(
                        config.accountId,
                        "Error migrating $identifier in Cached Guid Key Pref"
                    )
                    crypted = identifier
                    migrationStatus = ENCRYPTION_FLAG_FAIL
                }
                val cryptedKey = key + "_" + crypted
                newGuidJsonObj.put(cryptedKey, cachedGuidJsonObj[nextJSONObjKey])
            }
            if (cachedGuidJsonObj.length() > 0) {
                val cachedGuid = newGuidJsonObj.toString()
                StorageHelper.putString(
                    context,
                    StorageHelper.storageKeyWithSuffix(config, CACHED_GUIDS_KEY),
                    cachedGuid
                )
                config.logger.verbose(
                    config.accountId,
                    "setCachedGUIDs after migration:[$cachedGuid]"
                )
            }
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error migrating cached guids: $t")
            migrationStatus = ENCRYPTION_FLAG_FAIL
        }
        return migrationStatus
    }

    /**
     * This method migrates the encryption level of the value under the key k_n. This data is stored in the ARP related shared preference files
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param context - The Android context
     * @param cryptHandler - The [CryptHandler] object
     * @return - Returns the status for migration
     */
    private fun migrateARPPreferenceFiles(
        encrypt: Boolean,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler
    ): Int {
        config.logger.verbose(config.accountId, "Migrating encryption level for ARP related prefs")
        var migrationStatus = ENCRYPTION_FLAG_KN_SUCCESS
        try {
            // Gets all the files present in the shared_prefs directory
            val dataDir = context.applicationInfo.dataDir
            val prefsDir = File(dataDir, "shared_prefs")
            val path = CLEVERTAP_STORAGE_TAG + "_ARP:" + config.accountId
            for (prefName in Objects.requireNonNull(prefsDir.list())) {

                //Checks if the file name of the preference is an ARP file for the current accountID
                if (prefName.startsWith(path)) {
                    val prefFile = prefName.substring(0, prefName.length - 4)
                    val prefs = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
                    val value = prefs.getString(KEY_ENCRYPTION_k_n, "")

                    // If key k_n is present then it is encrypted/decrypted and persisted
                    if (value != "") {
                        var crypted: String? =
                            if (encrypt)
                                cryptHandler.encrypt(value!!, KEY_ENCRYPTION_k_n)
                            else
                                cryptHandler.decrypt(value!!, KEY_ENCRYPTION_MIGRATION)
                        if (crypted == null) {
                            config.logger.verbose(
                                config.accountId,
                                "Error migrating k_n in ARP Pref"
                            )
                            crypted = value
                            migrationStatus = ENCRYPTION_FLAG_FAIL
                        }
                        val editor = prefs.edit().putString(KEY_ENCRYPTION_k_n, crypted)
                        StorageHelper.persist(editor)
                    }
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Error migrating ARP Preference Files: $e")
            migrationStatus = ENCRYPTION_FLAG_FAIL
        }
        return migrationStatus
    }

    private fun migrateDBProfile(
        encrypt: Boolean,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ): Int {
        val piiKeys = arrayListOf(
            KEY_ENCRYPTION_NAME,
            KEY_ENCRYPTION_EMAIL,
            KEY_ENCRYPTION_IDENTITY,
            KEY_ENCRYPTION_PHONE
        )
        var migrationStatus = ENCRYPTION_FLAG_DB_SUCCESS
        val profile =
            dbAdapter.fetchUserProfileById(config.accountId) ?: return ENCRYPTION_FLAG_DB_SUCCESS
        try {
            for (piiKey in piiKeys) {
                if (profile.has(piiKey)) {
                    val value = profile[piiKey]
                    if (value is String) {
                        var crypted = if (encrypt)
                            cryptHandler.encrypt(value, piiKey)
                        else
                            cryptHandler.decrypt(value, piiKey)
                        if (crypted == null) {
                            crypted = value
                            migrationStatus = ENCRYPTION_FLAG_FAIL
                        }
                        profile.put(piiKey, crypted)
                    }
                }
            }
            if (dbAdapter.storeUserProfile(config.accountId, profile) == -1L)
                migrationStatus = ENCRYPTION_FLAG_FAIL
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Error migrating local DB profile: $e")
            migrationStatus = ENCRYPTION_FLAG_FAIL
        }
        return migrationStatus
    }

    @JvmStatic
    fun updateEncryptionFlagOnFailure(
        context: Context,
        config: CleverTapInstanceConfig,
        failedFlag: Int,
        cryptHandler: CryptHandler
    ) {

        val updatedEncryptionFlag =
            (failedFlag xor cryptHandler.encryptionFlagStatus) and cryptHandler.encryptionFlagStatus
        config.logger.verbose(
            config.accountId,
            "Updating encryption flag status after error in encryption to $updatedEncryptionFlag"
        )
        StorageHelper.putInt(
            context, StorageHelper.storageKeyWithSuffix(
                config,
                KEY_ENCRYPTION_FLAG_STATUS
            ),
            updatedEncryptionFlag
        )
        cryptHandler.encryptionFlagStatus = updatedEncryptionFlag

    }
}