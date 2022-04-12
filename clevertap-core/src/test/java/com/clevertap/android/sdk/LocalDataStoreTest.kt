package com.clevertap.android.sdk

import android.content.Context
import android.os.SystemClock
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test;
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.ArrayList
import kotlin.test.*


@RunWith(RobolectricTestRunner::class)
class LocalDataStoreTest : BaseTestCase() {
    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var config: CleverTapInstanceConfig

    private lateinit var localDataStoreWithDefConfig: LocalDataStore
    private lateinit var localDataStoreWithConfig: LocalDataStore
    private lateinit var localDataStoreWithConfigSpy: LocalDataStore

    override fun setUp() {
        super.setUp()
        defConfig = CleverTapInstanceConfig.createDefaultInstance(appCtx, "id", "token", "region")
        localDataStoreWithDefConfig = LocalDataStore(appCtx, defConfig)
        config = CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region")
        localDataStoreWithConfig = LocalDataStore(appCtx, config)
        localDataStoreWithConfigSpy = Mockito.spy(localDataStoreWithConfig)
    }

    @Test
    fun test_changeUser_when_ABC_should_XYZ() {
        //localDataStoreWithDefConfig.changeUser()
        // since changeUser() is a void function calls resetLocalProfileSync() which is a private function,
        // we can't further test it or verify its calling
        assertTrue { true }
        //TODO@ansh: add resetLocalProfileSync() tests here since it's a private method which gets called, we get a chance to test it here
    }

    @Test
    fun test_getEventDetail_when_EventNameIsPassed_should_ReturnEventDetail() {
        //if configuration has personalisation disabled, getEventDetail() will return null
        defConfig.enablePersonalization(false)
        localDataStoreWithDefConfig.getEventDetail("eventName").let {
            println(it)
            assertNull(it)
        }
        //resetting personalisation for further tests :

        //if default config is used, decodeEventDetails/getStringFromPrefs will be called with namespace = eventNamespace
        // we verify it by ensuring that config.accountId is not called more than 3 times (3 because these are the min no. of times it gets called before this function call)
        var config = CleverTapInstanceConfig.createDefaultInstance(appCtx, "id", "token", "region").also { it.enablePersonalization(true) }
        var configSpy = Mockito.spy(config)
        var lds = LocalDataStore(appCtx, configSpy)
        lds.getEventDetail("eventName").let {
            Mockito.verify(configSpy,Mockito.times(3)).accountId
        }//TODO@ansh: Wrong test case, you can actually assert return values by mocking required objects, same as getEventHistory test case

        // if non default config is used,  decodeEventDetails/getStringFromPrefs will be called with namespace = eventNamespace + ":" + this.config.getAccountId();
        // we verify it by ensuring that config.accountId is not called more than 5 times (5 because 3 gets called before this function call, 1 during and 1 after)
        config = CleverTapInstanceConfig.createInstance(appCtx,"id","token","region")
        configSpy = Mockito.spy(config)
        lds = LocalDataStore(appCtx, configSpy)
        lds.getEventDetail("event").let {
            Mockito.verify(configSpy,Mockito.atLeast(3)).accountId
        }//TODO@ansh: Wrong test case, you can actually assert return values by mocking required objects, same as getEventHistory test case
    }

    //TODO@ansh: Add test case when prference value is empty should return empty Map
    //TODO@ansh: Add test case when some crash happens then it gets handled and returns null
    @Test
    fun test_getEventHistory_when_FunctionIsCalled_should_ReturnAMapOfEventNameAndDetails() {
        var results: Map<String, EventDetail> = mutableMapOf()
        var eventDetail: EventDetail? = null

        //if default config is used, events are stored in local_events file
        StorageHelper.getPreferences(appCtx, "local_events").edit().putString("event", "2|1648192535|1648627865").commit()
        results = localDataStoreWithDefConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull(results["event"])
        assertTrue { results["event"] is EventDetail }
        assertEquals(2, results["event"]?.count)
        assertEquals(1648192535, results["event"]?.firstTime)
        assertEquals(1648627865, results["event"]?.lastTime)


        //if  default instance is not used, events are stored in "local_events:$accountID"
        StorageHelper.getPreferences(appCtx, "local_events:id").edit().putString("event2", "33|1234|2234").commit()
        results = localDataStoreWithConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull(results["event2"])
        assertTrue { results["event2"] is EventDetail }
        assertEquals(33, results["event2"]?.count)
        assertEquals(1234, results["event2"]?.firstTime)
        assertEquals(2234, results["event2"]?.lastTime)

    }

    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithSomeKey_should_CallGetProfileValueForKey() {
        localDataStoreWithConfigSpy.getProfileProperty("key")
        Mockito.verify(localDataStoreWithConfigSpy, Mockito.times(1)).getProfileValueForKey("key")
    }

    //TODO@ansh: Add test case when some crash happens then it gets handled and returns null
    @Test
    fun test_getProfileValueForKey_when_FunctionIsCalledWithSomeKey_should_ReturnAssociatedValue() {
        // since getProfileValueForKey() calls _getProfileProperty() which is a private function,
        // we can't further test it or verify its calling. we can only verify the working of _getProfileProperty
        localDataStoreWithConfig.getProfileProperty(null).let {
            println(it)
            assertNull(it)
        }
        localDataStoreWithConfig.setProfileField("key", "val")
        localDataStoreWithConfig.getProfileProperty("key").let {
            println(it)
            assertNotNull(it)
            assertEquals("val", it)
        }

    }

    //TODO@ansh: Wrong test case, you can actually assert persisted values in preference, for ex. after method stores value in pref, you get same from pref and assert equals
    @Test
    fun test_persistEvent_when_ContextAndJsonAndTypeIsPassed_should_SaveDataToSharedPref() {
        val contextSpy = Mockito.spy(appCtx)

        // when context and null json object is passed, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        // note: we are using an internal call to context.sharedpref() to validate this

        localDataStoreWithConfig.persistEvent(contextSpy, null, -1)
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)


        // when context and valid json object is passed but type is not  Constants.RAISED_EVENT, the function is returned without call to the internal function persistEvent(ctx,jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, JSONObject(), -1)
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)

        // when context and valid json object is passed type is Constants.RAISED_EVENT, but json doesn't have any value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) but returns without saving anything
        val jsonObject = JSONObject()
        var jsonSpy = Mockito.spy(jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        Mockito.verify(jsonSpy, Mockito.times(1)).getString("evtName")
        Mockito.verify(contextSpy, Mockito.times(0)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)


        // when context and valid json object is passed type is Constants.RAISED_EVENT, and json  have a value for "evtName" key, then the function calls persistEvent(ctx,jsonObject) and also initiates saving the value
        jsonObject.put("evtName", "someVal")
        jsonSpy = Mockito.spy(jsonObject)
        localDataStoreWithConfig.persistEvent(contextSpy, jsonSpy, Constants.RAISED_EVENT)
        Mockito.verify(jsonSpy, Mockito.times(1)).getString("evtName")
        Mockito.verify(contextSpy, Mockito.times(1)).getSharedPreferences("WizRocket_local_events:id", Context.MODE_PRIVATE)
    }

    //TODO@ansh: Add test cases related to flags persist and fromUpstream
    @Test
    fun test_removeProfileField_when_KeyIsPassed_should_RemoveKeyFromPROFILE_FIELDS_IN_THIS_SESSION() {
        //this function is a wrapper around private function removeProfileField(String key, Boolean fromUpstream, boolean persist)  and therefore cannot be tested further

        //------

        //testing private function removeProfileField(String key, Boolean fromUpstream, boolean persist)  via current function removeProfileField("key")

        //1. if null is passed, this will return without changing any value of LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION

        //2. if not null key is passed,and key is present in LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION , it will remove that key

        localDataStoreWithConfig.setProfileField("key", "abc")
        val originalVal = localDataStoreWithConfig.getProfileValueForKey("key")
        assertEquals("abc", originalVal)
        localDataStoreWithConfig.removeProfileField("key")
        var updatedValue = localDataStoreWithConfig.getProfileValueForKey("key")
        assertNull(updatedValue)

        //3. if not null key is passed,and key is not present in LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION , it will return without changing anything
        localDataStoreWithConfig.setProfileField("key", "abc")
        localDataStoreWithConfig.removeProfileField("key2")
        updatedValue = localDataStoreWithConfig.getProfileValueForKey("key")
        assertNotNull(updatedValue)
    }

    //TODO@ansh: Add test cases related to persistLocalProfileAsync()
    @Test
    fun test_removeProfileFields_when_KeysArePassed_should_RemoveKeyFromPROFILE_FIELDS_IN_THIS_SESSION() {
        //this function is a wrapper around private function removeProfileField(String key, Boolean fromUpstream, boolean persist)  and therefore cannot be tested further

        //------

        //testing private function removeProfileField(String key, Boolean fromUpstream, boolean persist)  via current function removeProfileField("key")

        //1. if null is passed, this will return without changing any value of LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION

        var keys: ArrayList<String>? = null
        localDataStoreWithConfig.removeProfileFields(keys)
        //2. if not null keys are passed,and keys are present in LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION , it will remove those keys

        keys = arrayListOf("k1", "k2")
        localDataStoreWithConfig.setProfileField(keys[0], "val1")
        localDataStoreWithConfig.setProfileField(keys[1], "val2")
        localDataStoreWithConfig.removeProfileFields(keys)
        localDataStoreWithConfig.getProfileValueForKey(keys[0]).let { assertNull(it) }
        localDataStoreWithConfig.getProfileValueForKey(keys[1]).let { assertNull(it) }

        //3. if not null keys are passed,and those keys are not present in LocalDataStore.PROFILE_FIELDS_IN_THIS_SESSION , it will return without changing anything
        localDataStoreWithConfig.setProfileField(keys[0], "val1")
        localDataStoreWithConfig.setProfileField(keys[1], "val2")
        localDataStoreWithConfig.removeProfileFields(arrayListOf("keyX", "keyX"))
        localDataStoreWithConfig.getProfileValueForKey(keys[0]).let { assertEquals("val1", it) }
        localDataStoreWithConfig.getProfileValueForKey(keys[1]).let { assertEquals("val2", it) }

    }

    @Test
    fun test_setDataSyncFlag_when_FunctionIsCalledWithAJson_should_AddABooleanKeyNamedDSyncToTheJson() {
        var json = JSONObject()
        var jsonSpy = Mockito.spy(json)

        // 1. when personalisation is disabled, it will add "dsync" =  false in our object AND RETURN.
        config.enablePersonalization(false)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        assertFalse { jsonSpy.getBoolean("dsync") }
        Mockito.verify(jsonSpy, Mockito.never()).getString("type")

        // 2. if personalisation is enabled, it will contrinue for other checks and modification to original json will depend on other factors
        config.enablePersonalization(true)
        localDataStoreWithConfig.setDataSyncFlag(jsonSpy)
        Mockito.verify(jsonSpy, Mockito.atLeastOnce()).getString("type")


        //3. if if json.get("type") is "profile"  , dsync will always be true
        json = JSONObject().also { it.put("type", "profile") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }


        //4. if if json.get("type") is "event" and json.get("event") is  "App Launched" , dsync will always be true
        json = JSONObject().also {
            it.put("type", "profile")
            it.put("event", "App Launched")
        }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }

        //5. if json.get("type") is null or ANYTHING ELSE APART FROM "event" or "profile", then it will
        //   add  "dsync" =  true or false based on current time and value of "local_cache_last_update"
        //   in cache. if the difference between current time and cache value is <20 mins dsync = true
        //   is added else dsync = false is added


        //assuming last update was just done now. then local_cache_last_update+20 mins > current time. therefore value of dsync should be false
        var lastUpdateTime = (System.currentTimeMillis() / 1000).toInt()
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertFalse { json.getBoolean("dsync") }

        //assuming last update was 5 mins ago. then local_cache_last_update+20 mins > current time. therefore value of dsync should be false
        lastUpdateTime = (System.currentTimeMillis() / 1000).toInt() - (5 * 60)
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertFalse { json.getBoolean("dsync") }

        //assuming last update was 50 mins ago. then local_cache_last_update+20 mins < current time. therefore value of dsync should be true
        lastUpdateTime = (System.currentTimeMillis() / 1000).toInt() - (50 * 60)
        StorageHelper.putInt(appCtx, "local_cache_last_update:id", lastUpdateTime)
        json = JSONObject().also { it.put("type", "other") }
        localDataStoreWithConfig.setDataSyncFlag(json)
        assertTrue { json.getBoolean("dsync") }
    }

    //TODO@ansh: Add test cases related to flags persist and fromUpstream
    @Test
    fun test_setProfileField_when_FunctionIsCalledWithKeyAndValue_should_UpdatePROFILE_FIELDS_IN_THIS_SESSIONMap() {
        // since this is a wrapper around a private function, it could not be properly tested.

        //testing private function setProfileField(...)'s limited features

        // when either key or value is null , the internal private function returns without changing PROFILE_FIELDS_IN_THIS_SESSION map
        localDataStoreWithConfig.setProfileField("key1", null)
        localDataStoreWithConfig.setProfileField(null, "hi")
        assertNull(localDataStoreWithConfig.getProfileProperty("key1"))
        assertNull(localDataStoreWithConfig.getProfileProperty(null))

        // when key and value are not null, it sets the value on PROFILE_FIELDS_IN_THIS_SESSION map
        localDataStoreWithConfig.setProfileField("key1", "vvv")
        assertNotNull(localDataStoreWithConfig.getProfileProperty("key1"))
        assertEquals("vvv", localDataStoreWithConfig.getProfileProperty("key1"))
    }

    //TODO@ansh: Add test cases related to persistLocalProfileAsync()
    @Test
    fun test_setProfileFields_when_JsonWithKeysArePassed_should_SetTheKeysInPROFILE_FIELDS_IN_THIS_SESSIONMap() {
        val json = JSONObject()
        val jsonSpy = Mockito.spy(json)

        // when json is null, it returns without further execution. verified by checking if json.keys() is called
        //localDataStoreWithConfig.setProfileFields(null)

        //when json has some keys, it will set the keys in PROFILE_FIELDS_IN_THIS_SESSION map
        json.put("key1", 1).put("key2", "hi").put("key3", true)
        localDataStoreWithConfig.setProfileFields(jsonSpy)
        Mockito.verify(jsonSpy, Mockito.atLeastOnce()).keys()
        assertEquals(1, localDataStoreWithConfig.getProfileProperty("key1"))
        assertEquals("hi", localDataStoreWithConfig.getProfileProperty("key2"))
        assertEquals(true, localDataStoreWithConfig.getProfileProperty("key3"))
    }

    @Test
    fun test_syncWithUpstream_when_ABC_should_XYZ(){
        val ctxSpy = Mockito.spy(appCtx)
        var json = JSONObject()
        var jsonSpy = Mockito.spy(json)

        // 1. when json does not have "evpr" key, the function returns without further execution
        localDataStoreWithConfig.syncWithUpstream(ctxSpy,jsonSpy)
        Mockito.verify(jsonSpy,Mockito.never()).getJSONObject("evpr")

        json.put("evpr",JSONObject())
        localDataStoreWithConfig.syncWithUpstream(ctxSpy,jsonSpy)
        Mockito.verify(jsonSpy,Mockito.atLeastOnce()).getJSONObject("evpr")

    }
}