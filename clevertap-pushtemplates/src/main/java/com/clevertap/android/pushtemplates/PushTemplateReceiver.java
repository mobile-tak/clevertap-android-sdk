package com.clevertap.android.pushtemplates;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.RemoteInput;

import com.clevertap.android.pushtemplates.content.PendingIntentFactory;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;

import com.clevertap.android.sdk.interfaces.NotificationHandler;
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService;
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.clevertap.android.pushtemplates.content.PendingIntentFactoryKt.MANUAL_CAROUSEL_CONTENT_PENDING_INTENT;
import static com.clevertap.android.pushtemplates.content.PendingIntentFactoryKt.MANUAL_CAROUSEL_DISMISS_PENDING_INTENT;
import static com.clevertap.android.pushtemplates.content.PendingIntentFactoryKt.MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT;
import static com.clevertap.android.pushtemplates.content.PendingIntentFactoryKt.MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT;
import static com.clevertap.android.pushtemplates.content.PendingIntentFactoryKt.PRODUCT_DISPLAY_CONTENT_PENDING_INTENT;
import static com.clevertap.android.sdk.pushnotification.CTNotificationIntentService.TYPE_BUTTON_CLICK;

public class PushTemplateReceiver extends BroadcastReceiver {
    boolean clicked1 = true, clicked2 = true, clicked3 = true, clicked4 = true, clicked5 = true, img1 = false, img2 = false, img3 = false, buynow = true, bigimage = true, cta1 = true, cta2 = true, cta3 = true, cta4 = true, cta5 = true, close = true;

    private CleverTapAPI cleverTapAPI;

    private RemoteViews contentViewBig, contentViewSmall, contentViewRating, contentViewManualCarousel;
    private String pt_id;
    private TemplateType templateType;
    private String pt_title;
    private String pt_msg;
    private String pt_msg_summary;
    private String pt_img_small;
    private String pt_large_icon;
    private String pt_rating_default_dl;
    private String pt_title_clr, pt_msg_clr;
    private ArrayList<String> imageList = new ArrayList<>();
    private ArrayList<String> deepLinkList = new ArrayList<>();
    private ArrayList<String> bigTextList = new ArrayList<>();
    private ArrayList<String> smallTextList = new ArrayList<>();
    private ArrayList<String> priceList = new ArrayList<>();
    private String pt_bg;
    private String channelId;
    private int smallIcon = 0;
    private int pt_dot = 0;
    private boolean requiresChannelId;
    private NotificationManager notificationManager;
    private String pt_product_display_action;
    private String pt_product_display_action_clr;
    private String pt_product_display_linear;
    private String pt_big_img_alt;
    private Bitmap pt_small_icon;
    private Bitmap pt_dot_sep;
    private String pt_small_icon_clr;
    private String pt_product_display_action_text_clr;
    private String pt_big_img;
    private String pt_meta_clr;
    private boolean pt_dismiss_intent;
    private String pt_rating_toast;
    private String pt_subtitle;
    private String pID;
    private CleverTapInstanceConfig config;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        Utils.createSilentNotificationChannel(context);

        if (intent.getExtras() != null) {
            final Bundle extras = intent.getExtras();
            cleverTapAPI = CleverTapAPI
                    .getGlobalInstance(context, extras.getString(Constants.WZRK_ACCT_ID_KEY));
            pt_id = intent.getStringExtra(PTConstants.PT_ID);
            pID = extras.getString(Constants.WZRK_PUSH_ID);
            pt_msg = extras.getString(PTConstants.PT_MSG);
            pt_msg_summary = extras.getString(PTConstants.PT_MSG_SUMMARY);
            pt_msg_clr = extras.getString(PTConstants.PT_MSG_COLOR);
            pt_title = extras.getString(PTConstants.PT_TITLE);
            pt_title_clr = extras.getString(PTConstants.PT_TITLE_COLOR);
            pt_img_small = extras.getString(PTConstants.PT_SMALL_IMG);
            pt_large_icon = extras.getString(PTConstants.PT_NOTIF_ICON);
            pt_bg = extras.getString(PTConstants.PT_BG);
            pt_rating_default_dl = extras.getString(PTConstants.PT_DEFAULT_DL);
            imageList = Utils.getImageListFromExtras(extras);
            deepLinkList = Utils.getDeepLinkListFromExtras(extras);
            bigTextList = Utils.getBigTextFromExtras(extras);
            smallTextList = Utils.getSmallTextFromExtras(extras);
            priceList = Utils.getPriceFromExtras(extras);
            pt_product_display_action = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION);
            pt_product_display_action_clr = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_COLOUR);
            pt_product_display_linear = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_LINEAR);
            notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            channelId = extras.getString(Constants.WZRK_CHANNEL_ID, "");
            pt_big_img_alt = extras.getString(PTConstants.PT_BIG_IMG_ALT);
            pt_small_icon_clr = extras.getString(PTConstants.PT_SMALL_ICON_COLOUR);
            pt_product_display_action_text_clr = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR);
            requiresChannelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
            pt_dismiss_intent = extras.getBoolean(PTConstants.PT_DISMISS_INTENT, false);
            pt_rating_toast = extras.getString(PTConstants.PT_RATING_TOAST);
            pt_subtitle = extras.getString(PTConstants.PT_SUBTITLE);
            setKeysFromDashboard(extras);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelIdError = null;
                if (channelId.isEmpty()) {
                    channelIdError = "Unable to render notification, channelId is required but not provided in the notification payload: " + extras.toString();
                } else if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                    channelIdError = "Unable to render notification, channelId: " + channelId + " not registered by the app.";
                }
                if (channelIdError != null) {
                    PTLog.verbose(channelIdError);
                    return;
                }
            }

            if (pt_id != null) {
                templateType = TemplateType.fromString(pt_id);
            }

            if (cleverTapAPI != null) {
                try {
                    this.config = cleverTapAPI.getCoreState().getConfig();
                    Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
                    task.execute("PushTemplateReceiver#renderNotification", new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            try {
                                if (pt_dismiss_intent) {
                                    Utils.deleteSilentNotificationChannel(context);
                                    Utils.deleteImageFromStorage(context, intent);
                                    return null;
                                }
                                if (templateType != null) {
                                    switch (templateType) {
                                        case RATING:
                                            handleRatingNotification(context, extras);
                                            break;
                                        case FIVE_ICONS:
                                            handleFiveCTANotification(context, extras);
                                            break;
                                        case PRODUCT_DISPLAY:
                                            handleProductDisplayNotification(context, extras);
                                            break;
                                        case INPUT_BOX:
                                            handleInputBoxNotification(context, extras, intent);
                                            break;
                                        case MANUAL_CAROUSEL:
                                            handleManualCarouselNotification(context, extras);
                                            break;
                                    }
                                }
                            } catch (Throwable t) {
                                PTLog.verbose("Couldn't render notification: " + t.getLocalizedMessage());
                            }
                            return null;
                        }
                    });
                } catch (Exception e) {
                    PTLog.verbose("Couldn't render notification: " + e.getLocalizedMessage());
                }

            } else {
                PTLog.verbose("clevertap instance is null, not running PushTemplateReceiver#renderNotification");
            }
        }
    }

    private void handleManualCarouselNotification(Context context, Bundle extras) {
        try {
            /**
             * Android 12 requires to recreate remote views to update notification
             * but recreating remote views creates performance issue and notification lag,
             * hence using below method to get remote views from existing notification
             */
            if (VERSION.SDK_INT >= VERSION_CODES.M){
                int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
                Notification notification = Utils.getNotificationById(context, notificationId);
                if (notification!=null) {
                    contentViewManualCarousel = notification.bigContentView;
                    contentViewSmall = notification.contentView;
                }
                setCustomContentViewBasicKeys(contentViewManualCarousel, context);

                final boolean rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE);

                imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST);
                deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST);

                int currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT);
                int newPosition;
                if (rightSwipe) {
                    contentViewManualCarousel.showNext(R.id.carousel_image);
                    contentViewManualCarousel.showNext(R.id.carousel_image_right);
                    contentViewManualCarousel.showNext(R.id.carousel_image_left);
                    if (currPosition == imageList.size() - 1) {
                        newPosition = 0;
                    } else {
                        newPosition = currPosition + 1;
                    }
                } else {
                    contentViewManualCarousel.showPrevious(R.id.carousel_image);
                    contentViewManualCarousel.showPrevious(R.id.carousel_image_right);
                    contentViewManualCarousel.showPrevious(R.id.carousel_image_left);
                    if (currPosition == 0) {
                        newPosition = imageList.size() - 1;
                    } else {
                        newPosition = currPosition - 1;
                    }
                }
                String dl = "";

                if (deepLinkList != null && deepLinkList.size() == imageList.size()) {
                    dl = deepLinkList.get(newPosition);
                } else if (deepLinkList != null && deepLinkList.size() == 1) {
                    dl = deepLinkList.get(0);
                } else if (deepLinkList != null && deepLinkList.size() > newPosition) {
                    dl = deepLinkList.get(newPosition);
                } else if (deepLinkList != null && deepLinkList.size() < newPosition) {
                    dl = deepLinkList.get(0);
                }

                extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition);
                extras.remove(PTConstants.PT_RIGHT_SWIPE);
                extras.putString(Constants.DEEP_LINK_KEY,dl);
                extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition);

                contentViewManualCarousel.setOnClickPendingIntent(R.id.rightArrowPos0,  PendingIntentFactory
                        .getPendingIntent(context,notificationId, extras,false,
                                MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT,null));

                contentViewManualCarousel.setOnClickPendingIntent(R.id.leftArrowPos0, PendingIntentFactory
                        .getPendingIntent(context,notificationId, extras,false,
                                MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT,null));


                PendingIntent pIntent = PendingIntentFactory.getPendingIntent(context,notificationId,extras,true,
                        MANUAL_CAROUSEL_CONTENT_PENDING_INTENT,null
                );//setPendingIntent(context, notificationId, extras, launchIntent, dl);

                NotificationCompat.Builder notificationBuilder;
                if (notification!=null)
                {
                    notificationBuilder = new Builder(context, notification);
                } else {
                    notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, PTConstants.PT_SILENT_CHANNEL_ID, context);
                }

                //NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, PTConstants.PT_SILENT_CHANNEL_ID, context);
            /*Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);*/

                PendingIntent dIntent = PendingIntentFactory.getPendingIntent(context,notificationId,extras,false,
                        MANUAL_CAROUSEL_DISMISS_PENDING_INTENT,null);

                setSmallIcon(context);

                //if (VERSION.SDK_INT < VERSION_CODES.S) {
                setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewManualCarousel,
                        pt_title, pIntent, dIntent);

                notification = notificationBuilder.build();
                //}

                notificationManager.notify(notificationId, notification);
            }else{
                extras.putString(Constants.EXTRAS_FROM,"PTReceiver");
                NotificationHandler notificationHandler = CleverTapAPI.getNotificationHandler();
                if (notificationHandler!=null)
                {
                    notificationHandler.onMessageReceived(context,extras,"FCM");
                }
            }
        } catch (Throwable t) {
            PTLog.verbose("Error creating manual carousel notification ", t);
        }
    }

    private void handleInputBoxNotification(Context context, Bundle extras, Intent intent) {

        //Fetch Remote Input
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
        PendingIntent dIntent;
        dIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent);
        config = extras.getParcelable("config");

        if (remoteInput != null) {
            //Fetch Reply
            CharSequence reply = remoteInput.getCharSequence(
                    PTConstants.PT_INPUT_KEY);

            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);

            if (reply != null) {

                PTLog.verbose("Processing Input from Input Template");
                extras.putString(PTConstants.PT_INPUT_KEY, reply.toString());
                Utils.raiseCleverTapEvent(context, config, extras, PTConstants.PT_INPUT_KEY);
                //Update the notification to show that the reply was received.
                final NotificationCompat.Builder repliedNotification;
                if (requiresChannelId) {
                    repliedNotification = new NotificationCompat.Builder(context, PTConstants.PT_SILENT_CHANNEL_ID);
                } else {
                    repliedNotification = new NotificationCompat.Builder(context);
                }
                setSmallIcon(context);
                repliedNotification.setSmallIcon(smallIcon)
                        .setContentTitle(pt_title)
                        .setContentText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                        .setTimeoutAfter(PTConstants.PT_INPUT_TIMEOUT)
                        .setDeleteIntent(dIntent)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true);

                setStandardViewBigImageStyle(pt_big_img_alt, extras, context, repliedNotification);

                Notification notification = repliedNotification.build();
                notificationManager.notify(notificationId, notification);

                /* Check if Auto Open key is present and not empty, if not present then show feedback and
                auto kill in 3 secs. If present, then launch the App with Dl or Launcher activity.
                The launcher activity will get the reply in extras under the key "pt_reply" */
                if (VERSION.SDK_INT < VERSION_CODES.S && (extras.getString(PTConstants.PT_INPUT_AUTO_OPEN) != null
                                || extras.getBoolean(PTConstants.PT_INPUT_AUTO_OPEN))) {
                    //adding delay for launcher
                    try {
                        Thread.sleep(PTConstants.PT_INPUT_TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Intent launchIntent;

                    if (extras.containsKey(Constants.DEEP_LINK_KEY) && extras.getString(Constants.DEEP_LINK_KEY)!=null) {
                        launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY)));
                        Utils.setPackageNameFromResolveInfoList(context, launchIntent);
                    } else {
                        launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        if (launchIntent == null) {
                            return;
                        }
                    }

                    launchIntent.putExtras(extras);

                    //adding reply to extra
                    launchIntent.putExtra("pt_reply", reply);

                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    context.startActivity(launchIntent);
                }

            } else {
                PTLog.verbose("PushTemplateReceiver: Input is Empty");
            }
        }

    }

    private void handleRatingNotification(Context context, Bundle extras) {
        try {
            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);

            if (extras.getBoolean(PTConstants.DEFAULT_DL, false)) {
                this.config = extras.getParcelable("config");
                notificationManager.cancel(notificationId);
                Intent launchIntent;
                Class clazz = null;
                try {
                    clazz = Class
                            .forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
                } catch (ClassNotFoundException ex) {
                    PTLog.debug("No Intent Service found");
                }

                boolean isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils
                        .isServiceAvailable(context, clazz);
                if (isPTIntentServiceAvailable) {
                    launchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                    launchIntent.setPackage(context.getPackageName());
                    launchIntent.putExtra(Constants.KEY_CT_TYPE, TYPE_BUTTON_CLICK);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra("dl", pt_rating_default_dl);
                    context.startService(launchIntent);
                } else {
                    launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pt_rating_default_dl));
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    Utils.raiseNotificationClicked(context, extras, this.config);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_rating_default_dl);
                    context.startActivity(launchIntent);
                }

                return;
            }

            String pt_dl_clicked = deepLinkList.get(0);

            if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 1);
                if (deepLinkList.size() > 0) {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            }
            if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 2);
                if (deepLinkList.size() > 1) {
                    pt_dl_clicked = deepLinkList.get(1);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            }
            if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 3);
                if (deepLinkList.size() > 2) {
                    pt_dl_clicked = deepLinkList.get(2);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            }
            if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 4);
                if (deepLinkList.size() > 3) {
                    pt_dl_clicked = deepLinkList.get(3);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            }
            if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 5);
                if (deepLinkList.size() > 4) {
                    pt_dl_clicked = deepLinkList.get(4);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            }

            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                Notification notification = Utils.getNotificationById(context, notificationId);
                if (notification != null) {
                    contentViewRating = notification.bigContentView;
                    contentViewSmall = notification.contentView;
                }
                //Set RemoteViews again
                /*contentViewRating = new RemoteViews(context.getPackageName(), R.layout.rating);
                setCustomContentViewBasicKeys(contentViewRating, context);

                contentViewSmall = new RemoteViews(context.getPackageName(),
                        R.layout.content_view_small_single_line_msg);

                setCustomContentViewBasicKeys(contentViewSmall, context);

                setCustomContentViewTitle(contentViewRating, pt_title);
                setCustomContentViewTitle(contentViewSmall, pt_title);

                setCustomContentViewMessage(contentViewRating, pt_msg);
                setCustomContentViewMessage(contentViewSmall, pt_msg);

                setCustomContentViewMessageSummary(contentViewRating, pt_msg_summary);

                setCustomContentViewTitleColour(contentViewRating, pt_title_clr);
                setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

                setCustomContentViewMessageColour(contentViewRating, pt_msg_clr);
                setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

                setCustomContentViewExpandedBackgroundColour(contentViewRating, pt_bg);
                setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);*/

                //String pt_dl_clicked = deepLinkList.get(0);

                if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                    /*contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);*/
                    //extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 1);
                    //Utils.raiseNotificationClicked(context, extras, config);
                    clicked1 = false;

                    /*if (deepLinkList.size() > 0) {
                        pt_dl_clicked = deepLinkList.get(0);
                    }*/
                } else {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_outline);
                }
                if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                    /*contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);*/
                    //extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 2);
                    //Utils.raiseNotificationClicked(context, extras, config);
                    clicked2 = false;
                    /*if (deepLinkList.size() > 1) {
                        pt_dl_clicked = deepLinkList.get(1);
                    } else {
                        pt_dl_clicked = deepLinkList.get(0);
                    }*/
                } else {
                    contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline);
                }
                if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                    /*contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
                    contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);*/
                    //extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 3);
                    //Utils.raiseNotificationClicked(context, extras, config);
                    clicked3 = false;
                    /*if (deepLinkList.size() > 2) {
                        pt_dl_clicked = deepLinkList.get(2);
                    } else {
                        pt_dl_clicked = deepLinkList.get(0);
                    }*/
                } else {
                    contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
                }
                if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled);
                    //contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);
                    //extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 4);
                    //Utils.raiseNotificationClicked(context, extras, config);
                    clicked4 = false;
                    /*if (deepLinkList.size() > 3) {
                        pt_dl_clicked = deepLinkList.get(3);
                    } else {
                        pt_dl_clicked = deepLinkList.get(0);
                    }*/
                } else {
                    contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
                }
                if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled);
                    contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_filled);
                    //extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 5);
                    //Utils.raiseNotificationClicked(context, extras, config);
                    clicked5 = false;
                    /*if (deepLinkList.size() > 4) {
                        pt_dl_clicked = deepLinkList.get(4);
                    } else {
                        pt_dl_clicked = deepLinkList.get(0);
                    }*/
                } else {
                    contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);
                }

                /*if (6 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                    //Thread.sleep(1000);
                    notificationManager.cancel(notificationId);
                    Utils.raiseNotificationClicked(context, extras, config);

                    setToast(context, pt_rating_toast);

                    if (VERSION.SDK_INT < VERSION_CODES.S)
                    {
                        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        context.sendBroadcast(it);
                    }

                    Intent launchIntent;
                    if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                        launchIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(extras.getString(Constants.DEEP_LINK_KEY)));
                        com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, launchIntent);
                    } else {
                        launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        if (launchIntent == null) {
                            return;
                        }
                    }

                    launchIntent.putExtras(extras);
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                } else {*/ // rating is clicked
                    extras.putString(Constants.DEEP_LINK_KEY, pt_dl_clicked);
                    contentViewRating.setOnClickPendingIntent(R.id.tVRatingConfirmation,
                            LaunchPendingIntentFactory.getActivityIntent(extras,context));


                /*contentViewRating.setOnClickPendingIntent(R.id.star1, PendingIntentFactory.getPendingIntent(context,
                        notificationId, extras, false, RATING_CLICK1_PENDING_INTENT, null));
                contentViewRating.setOnClickPendingIntent(R.id.star2, PendingIntentFactory.getPendingIntent(context,
                        notificationId, extras, false, RATING_CLICK2_PENDING_INTENT, null));
                contentViewRating.setOnClickPendingIntent(R.id.star3, PendingIntentFactory.getPendingIntent(context,
                        notificationId, extras, false, RATING_CLICK3_PENDING_INTENT, null));
                contentViewRating.setOnClickPendingIntent(R.id.star4, PendingIntentFactory.getPendingIntent(context,
                        notificationId, extras, false, RATING_CLICK4_PENDING_INTENT, null));
                contentViewRating.setOnClickPendingIntent(R.id.star5, PendingIntentFactory.getPendingIntent(context,
                        notificationId, extras, false, RATING_CLICK5_PENDING_INTENT, null));*/

                setSmallIcon(context);

                NotificationCompat.Builder notificationBuilder;
                if (notification!=null)
                {
                    notificationBuilder = new Builder(context, notification);
                } else {
                    notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId,
                            PTConstants.PT_SILENT_CHANNEL_ID, context);
                }
                Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
                PendingIntent dIntent;
                dIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent);

                if (notificationManager != null) {
                    //Use the Builder to build notification
                    notificationBuilder.setSmallIcon(smallIcon)
                            .setCustomContentView(contentViewSmall)
                            .setCustomBigContentView(contentViewRating)
                            .setContentTitle(pt_title)
                            .setDeleteIntent(dIntent)
                            .setAutoCancel(true);

                    notification = notificationBuilder.build();

                    notificationManager.notify(notificationId, notification);
                }

                if (VERSION.SDK_INT < VERSION_CODES.S)
                    {
                        Utils.raiseNotificationClicked(context, extras, this.config);
                        handleRatingDeepLink(context, extras, notificationId, pt_dl_clicked,this.config);
                    }
                } else{
                    extras.putString(Constants.EXTRAS_FROM,"PTReceiver");
                Bundle clonedExtras = (Bundle) extras.clone();
                NotificationHandler notificationHandler = CleverTapAPI.getNotificationHandler();
                    if (notificationHandler!=null)
                    {
                        notificationHandler.onMessageReceived(context,extras,"FCM");
                        clonedExtras.putString(Constants.DEEP_LINK_KEY, pt_dl_clicked);
                        Utils.raiseNotificationClicked(context, clonedExtras, this.config);
                        handleRatingDeepLink(context, clonedExtras, notificationId, pt_dl_clicked,this.config);
                    }
            }
            //}
        } catch (Throwable t) {
            PTLog.verbose("Error creating rating notification ", t);
        }
    }

    private void handleRatingDeepLink(final Context context, final Bundle extras, final int notificationId,
            final String pt_dl_clicked, final CleverTapInstanceConfig config) throws InterruptedException {
            Thread.sleep(1000);
            notificationManager.cancel(notificationId);

            setToast(context, pt_rating_toast,config);

            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);

            Intent launchIntent;
            if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                launchIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(extras.getString(Constants.DEEP_LINK_KEY)));
                com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, launchIntent);
            } else {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (launchIntent == null) {
                    return;
                }
            }
            launchIntent.putExtras(extras);
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_dl_clicked);
            launchIntent.removeExtra(Constants.WZRK_ACTIONS);
            launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
    }

    private void handleProductDisplayNotification(Context context, Bundle extras) {
        try {
            /*if (buynow == extras.getBoolean(PTConstants.PT_BUY_NOW, false)) {
                notificationManager.cancel(notificationId);
                String dl = extras.getString(PTConstants.PT_BUY_NOW_DL, deepLinkList.get(0));
                config = extras.getParcelable("config");
                notificationManager.cancel(notificationId);
                Intent launchIntent;

                Class clazz = null;
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
                } catch (ClassNotFoundException ex) {
                    PTLog.debug("No Intent Service found");
                }

                boolean isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz);
                if (isPTIntentServiceAvailable) {
                    launchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra("dl", dl);
                    launchIntent.setPackage(context.getPackageName());
                    launchIntent.putExtra(PTConstants.PT_TYPE, TYPE_BUTTON_CLICK);
                    context.startService(launchIntent);
                } else {
                    launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl);
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    Utils.raiseNotificationClicked(context, extras, config);
                    context.startActivity(launchIntent);
                }
                return;
            }*/
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
                Notification notification = Utils.getNotificationById(context, notificationId);
                if (notification != null) {
                    contentViewBig = notification.bigContentView;
                    contentViewSmall = notification.contentView;
                }
            boolean isLinear = false;
            if (pt_product_display_linear == null || pt_product_display_linear.isEmpty()) {
                //contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_template);
                //contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small_single_line_msg);
            } else {
                isLinear = true;
                //contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_expanded);
                //contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_collapsed);
            }

            setCustomContentViewBasicKeys(contentViewBig, context);
            if (!isLinear) {
                setCustomContentViewBasicKeys(contentViewSmall, context);
            }

            int currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION);

            contentViewBig.setDisplayedChild(R.id.carousel_image, currentPosition);
            imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST);
            deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST);
            bigTextList = extras.getStringArrayList(PTConstants.PT_BIGTEXT_LIST);
            smallTextList = extras.getStringArrayList(PTConstants.PT_SMALLTEXT_LIST);
            priceList = extras.getStringArrayList(PTConstants.PT_PRICE_LIST);

            String dl = deepLinkList.get(currentPosition);
            if (!isLinear)
            {
                contentViewBig.setTextViewText(R.id.title, bigTextList.get(currentPosition));
            } else {
                contentViewBig.setTextViewText(R.id.product_name, bigTextList.get(currentPosition));
            }
            contentViewBig.setTextViewText(R.id.msg, smallTextList.get(currentPosition));
            contentViewBig.setTextViewText(R.id.product_price, priceList.get(currentPosition));
            extras.remove(PTConstants.PT_CURRENT_POSITION);

            Bundle bundleBuyNow = (Bundle) extras.clone();
            bundleBuyNow.putBoolean(PTConstants.PT_IMAGE_1, true);
            bundleBuyNow.putInt(PTConstants.PT_NOTIF_ID, notificationId);
            bundleBuyNow.putString(PTConstants.PT_BUY_NOW_DL, dl);
            bundleBuyNow.putBoolean(PTConstants.PT_BUY_NOW, true);

            contentViewBig.setOnClickPendingIntent(R.id.product_action,
                    PendingIntentFactory.getCtaLaunchPendingIntent(context,
                            bundleBuyNow, dl, notificationId));

                NotificationCompat.Builder notificationBuilder;
                if (notification!=null)
                {
                    notificationBuilder = new Builder(context, notification);
                } else {
                    notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId,
                            PTConstants.PT_SILENT_CHANNEL_ID, context);
                }

            PendingIntent pIntent;
            Bundle bundleLaunchIntent = (Bundle) extras.clone();
            bundleLaunchIntent.putString(Constants.DEEP_LINK_KEY,dl);
            pIntent = PendingIntentFactory.getPendingIntent(context,notificationId,bundleLaunchIntent,true,
                    PRODUCT_DISPLAY_CONTENT_PENDING_INTENT,null
            );

            if (notificationManager != null) {
                //Use the Builder to build notification
                Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
                PendingIntent dIntent;
                dIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent);
                setSmallIcon(context);

                setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewBig, pt_title, pIntent,
                        dIntent);

                notification = notificationBuilder.build();

                notificationManager.notify(notificationId, notification);
            }
        } else{
                extras.putString(Constants.EXTRAS_FROM,"PTReceiver");
                NotificationHandler notificationHandler = CleverTapAPI.getNotificationHandler();
                if (notificationHandler!=null)
                {
                    notificationHandler.onMessageReceived(context,extras,"FCM");
                }
            }

        } catch (Throwable t) {
            PTLog.verbose("Error creating product display notification ", t);
        }
    }

    private void handleFiveCTANotification(Context context, Bundle extras) {
        String dl = null;
        int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
        extras.putString(Constants.DEEP_LINK_KEY, dl);

        if (close == extras.getBoolean("close")) {
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + "close");
            notificationManager.cancel(notificationId);
            //context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
        Utils.raiseNotificationClicked(context, extras, config);
    }

    private PendingIntent setPendingIntent(Context context, int notificationId, Bundle extras, Intent launchIntent, String dl) {
        launchIntent.putExtras(extras);
        launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId);
        if (dl != null) {
            launchIntent.putExtra(PTConstants.DEFAULT_DL, true);
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl);
        }
        launchIntent.removeExtra(Constants.WZRK_ACTIONS);
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flagsLaunchPendingIntent |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                launchIntent, flagsLaunchPendingIntent);
    }

    private void setNotificationBuilderBasics(NotificationCompat.Builder notificationBuilder, RemoteViews contentViewSmall, RemoteViews contentViewBig, String pt_title, PendingIntent pIntent, PendingIntent dIntent) {
        notificationBuilder.setSmallIcon(smallIcon)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setContentTitle(Html.fromHtml(pt_title))
                .setDeleteIntent(dIntent)
                .setContentIntent(pIntent).setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
    }


    private NotificationCompat.Builder setBuilderWithChannelIDCheck(boolean requiresChannelId, String channelId, Context context) {
        if (requiresChannelId) {
            return new NotificationCompat.Builder(context, channelId);
        } else {
            return new NotificationCompat.Builder(context);
        }
    }

    private void setCustomContentViewBasicKeys(RemoteViews contentView, Context context) {
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context));
        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context));
        if (pt_subtitle != null && !pt_subtitle.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle));
            }
        } else {
            contentView.setViewVisibility(R.id.subtitle, View.GONE);
            contentView.setViewVisibility(R.id.sep_subtitle, View.GONE);
        }
        if (pt_meta_clr != null && !pt_meta_clr.isEmpty()) {
            contentView.setTextColor(R.id.app_name, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.timestamp, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.subtitle, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            setDotSep(context);
        }
    }

    private void setCustomContentViewMessageSummary(RemoteViews contentView, String pt_msg_summary) {
        if (pt_msg_summary != null && !pt_msg_summary.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary));
            }
        }
    }

    private void setCustomContentViewMessageColour(RemoteViews contentView, String pt_msg_clr) {
        if (pt_msg_clr != null && !pt_msg_clr.isEmpty()) {
            contentView.setTextColor(R.id.msg, Utils.getColour(pt_msg_clr, PTConstants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewTitleColour(RemoteViews contentView, String pt_title_clr) {
        if (pt_title_clr != null && !pt_title_clr.isEmpty()) {
            contentView.setTextColor(R.id.title, Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewExpandedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_big, "setBackgroundColor", Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE));
        }
    }

    private void setCustomContentViewCollapsedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_small, "setBackgroundColor", Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE));
        }
    }

    private void setCustomContentViewMessage(RemoteViews contentView, String pt_msg) {
        if (pt_msg != null && !pt_msg.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg));
            }
        }
    }

    private void setCustomContentViewTitle(RemoteViews contentView, String pt_title) {
        if (pt_title != null && !pt_title.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title));
            }
        }
    }

    private void setStandardViewBigImageStyle(String imgUrl, Bundle extras, Context context, NotificationCompat.Builder notificationBuilder) {
        NotificationCompat.Style bigPictureStyle;
        if (imgUrl != null && imgUrl.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmap(imgUrl, false, context);

                if (bpMap == null)
                    throw new Exception("Failed to fetch big picture!");


                bigPictureStyle = new NotificationCompat.BigPictureStyle()
                        .setSummaryText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                        .bigPicture(bpMap);

            } catch (Throwable t) {
                bigPictureStyle = new NotificationCompat.BigTextStyle()
                        .bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK));
                PTLog.verbose("Falling back to big text notification, couldn't fetch big picture", t);
            }
        } else {
            bigPictureStyle = new NotificationCompat.BigTextStyle()
                    .bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK));
        }

        notificationBuilder.setStyle(bigPictureStyle);

    }

    private void setSmallIcon(Context context) {
        Bundle metaData;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
            String x = Utils._getManifestStringValueForKey(metaData, Constants.LABEL_NOTIFICATION_ICON);
            if (x == null) throw new IllegalArgumentException();
            smallIcon = context.getResources().getIdentifier(x, "drawable", context.getPackageName());
            if (smallIcon == 0) throw new IllegalArgumentException();
        } catch (Throwable t) {
            smallIcon = Utils.getAppIconAsIntId(context);
        }
        try {
            pt_small_icon = Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting small icon color");
        }

    }

    private void setKeysFromDashboard(Bundle extras) {
        if (pt_title == null || pt_title.isEmpty()) {
            pt_title = extras.getString(Constants.NOTIF_TITLE);
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            pt_msg = extras.getString(Constants.NOTIF_MSG);
        }
        if (pt_msg_summary == null || pt_msg_summary.isEmpty()) {
            pt_msg_summary = extras.getString(Constants.WZRK_MSG_SUMMARY);
        }
        if (pt_big_img == null || pt_big_img.isEmpty()) {
            pt_big_img = extras.getString(Constants.WZRK_BIG_PICTURE);
        }
        if (pt_rating_default_dl == null || pt_rating_default_dl.isEmpty()) {
            pt_rating_default_dl = extras.getString(Constants.DEEP_LINK_KEY);
        }
        if (pt_meta_clr == null || pt_meta_clr.isEmpty()) {
            pt_meta_clr = extras.getString(Constants.WZRK_COLOR);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR);
        }
        if (pt_subtitle == null || pt_subtitle.isEmpty()) {
            pt_subtitle = extras.getString(Constants.WZRK_SUBTITLE);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR);
        }
    }

    private void setToast(Context context, String message,
            final CleverTapInstanceConfig config) {
        if (message != null && !message.isEmpty()) {
            Utils.showToast(context, message,config);
        }
    }

    private void setDotSep(Context context) {
        try {
            pt_dot = context.getResources().getIdentifier(PTConstants.PT_DOT_SEP, "drawable", context.getPackageName());
            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting dot sep color");
        }
    }

}
