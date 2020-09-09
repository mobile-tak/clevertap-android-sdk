package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.BuildConfig;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * loads providers
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PushProviders implements CTPushProviderListener {

    private final ArrayList<CTPushProvider> availableProviders = new ArrayList<>();
    private final CleverTapInstanceConfig config;
    private final Context context;

    private PushProviders(Context context, CleverTapInstanceConfig config) {
        this.config = config;
        this.context = context;
    }

    /**
     * Factory method to load push providers.
     *
     * @return A PushProviders class with the loaded providers.
     */
    @NonNull
    public static PushProviders load(Context context, CleverTapInstanceConfig config) {
        PushProviders providers = new PushProviders(context, config);
        providers.init();
        return providers;
    }

    /**
     * Loads all the plugins that are currently supported by the device.
     */
    private void init() {
        List<CTPushProvider> providers = createProviders();

        if (providers.isEmpty()) {
            log("No push providers found!. Make sure to install at least one push provider");
            return;
        }

        for (CTPushProvider provider : providers) {
            if (!isValid(provider)) {
                log("Invalid Provider: " + provider.getClass());
                continue;
            }

            if (!provider.isSupported()) {
                log("Unsupported Provider: " + provider.getClass());
                continue;
            }

            if (provider.isAvailable()) {
                log("Available Provider: " + provider.getClass());
                availableProviders.add(provider);
            } else {
                log("Unavailable Provider: " + provider.getClass());
            }
        }
    }

    private boolean isValid(CTPushProvider provider) {

        if (BuildConfig.VERSION_CODE < provider.minSDKSupportVersionCode()) {
            log("Provider: %s version %s does not match the SDK version %s. Make sure all Airship dependencies are the same version.");
            return false;
        }
        switch (provider.getPushType()) {
            case FCM:
            case HPS:
            case XPS:
            case BPS:
                if (provider.getPlatform() != PushConstants.ANDROID_PLATFORM) {
                    log("Invalid Provider: " + provider.getClass() +
                            " delivery is only available for Android platforms." + provider.getPushType());
                    return false;
                }
                break;
            case ADM:
                if (provider.getPlatform() != PushConstants.AMAZON_PLATFORM) {
                    log("Invalid Provider: " +
                            provider.getClass() +
                            " ADM delivery is only available for Amazon platforms." + provider.getPushType());
                    return false;
                }
                break;
        }

        return true;
    }

    /**
     * Creates the list of push providers.
     *
     * @return The list of push providers.
     */
    @NonNull
    private List<CTPushProvider> createProviders() {
        List<CTPushProvider> providers = new ArrayList<>();

        for (PushConstants.PushType pushType : config.getAllowedPushTypes()) {
            CTPushProvider pushProvider = null;
            try {
                Class<?> providerClass = Class.forName(pushType.getClassName());
                pushProvider = (CTPushProvider) providerClass.newInstance();
                pushProvider.setCTPushListener(this);
                log("Found provider:" + providerClass);
            } catch (InstantiationException e) {
                log("Unable to create provider " + pushType.getClassName());
            } catch (IllegalAccessException e) {
                log("Unable to create provider " + pushType.getClassName());
            } catch (ClassNotFoundException e) {
                log("Unable to create provider " + pushType.getClassName());
            } catch (Exception e) {
                log("Unable to create provider " + pushType.getClassName());
            }

            if (pushProvider == null) {
                continue;
            }

            providers.add(pushProvider);
        }

        return providers;
    }

    @NonNull
    public ArrayList<PushConstants.PushType> getAvailablePushTypes() {
        ArrayList<PushConstants.PushType> pushTypes = new ArrayList<>();
        for (CTPushProvider pushProvider : availableProviders) {
            pushTypes.add(pushProvider.getPushType());
        }
        return pushTypes;
    }

    public ArrayList<CTPushProvider> availableProviders() {
        return availableProviders;
    }

    public boolean isNotificationSupported() {
        for (PushConstants.PushType pushType : getAvailablePushTypes()) {
            if (PushUtils.getCachedToken(context, config, pushType) != null)
                return true;
        }
        return false;
    }

    @Override
    public Context context() {
        return context;
    }

    public void log(String message) {
        log("", message);
    }

    @Override
    public void log(String tag, String message) {
        config.getLogger().verbose(getDefaultSuffix(tag), message);
    }

    @Override
    public void log(String tag, String message, Throwable throwable) {
        config.getLogger().verbose(getDefaultSuffix(tag), message, throwable);
    }

    private String getDefaultSuffix(String tag) {
        return "[" + PushConstants.LOG_TAG + ":" + config.getAccountId() + "]" + (!TextUtils.isEmpty(tag) ? ": " + tag : "");
    }
}