package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY;
import static com.clevertap.android.sdk.Constants.WZRK_PUSH_ID;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

@RestrictTo(Scope.LIBRARY_GROUP)
public class PushNotificationUtil {

    public static String getAccountIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_ACCT_ID_KEY, defaultValue) : defaultValue;
    }

    public static String getPushIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_PUSH_ID, defaultValue) : defaultValue;
    }

    /**
     * Returns the names of all push types
     *
     * @return list
     */
    public static ArrayList<String> getAll() {
        ArrayList<String> list = new ArrayList<>();
        for (PushType pushType : PushType.values()) {
            list.add(pushType.name());
        }
        return list;
    }

    public static PushType[] getPushTypes(ArrayList<String> types) {
        PushType[] pushTypes = new PushType[0];
        if (types != null && !types.isEmpty()) {
            pushTypes = new PushType[types.size()];
            for (int i = 0; i < types.size(); i++) {
                pushTypes[i] = PushType.valueOf(types.get(i));
            }
        }
        return pushTypes;
    }

    private PushNotificationUtil() {

    }

    public static String buildPushNotificationRenderedListenerKey(String accountId, String pushId){
        return accountId+"_"+pushId;
    }

    private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

    public static String getRandomString(final int sizeOfRandomString) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public static Comparator<StatusBarNotification> postTimeComparator = new Comparator<StatusBarNotification>() {
        @Override
        public int compare(StatusBarNotification sbn1, StatusBarNotification sbn2) {
            // compare the post times
            return Long.compare(sbn1.getPostTime(), sbn2.getPostTime());
        }
    };

    public static long calculateTimeOutAfter(long postTime, long timeoutDurationLeft) {
        final long currentTime = System.currentTimeMillis();

        return timeoutDurationLeft - (currentTime - postTime);
    }

}