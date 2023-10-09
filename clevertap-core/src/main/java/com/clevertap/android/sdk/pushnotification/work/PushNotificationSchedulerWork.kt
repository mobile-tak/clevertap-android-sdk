package com.clevertap.android.sdk.pushnotification.work


import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil

class PushNotificationSchedulerWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    val tag = "PushNotificationSchedulerWork"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {

        Logger.d(tag, "initiating push notification scheduler work...")

        try {
            val context = applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotifications = notificationManager.activeNotifications


            // Logic to re-trigger older notifications to maintain the group.
            for (notification in activeNotifications) {
                reTriggerNotification(context, notification, notification.notification.channelId, notificationManager)
            }
        } catch (e: Exception) {
            Logger.d(tag, "scheduling failed with error: $e")
            return Result.failure()
        }

        Logger.d(tag, "scheduling finished")
        return Result.success()
    }

    @SuppressLint("NotificationTrampoline")
    fun reTriggerNotification(context: Context?, notification: StatusBarNotification, channelId: String?, nm: NotificationManager) {
        val nb = NotificationCompat.Builder(context!!, channelId!!)
        val n = notification.notification
        // Code copied from Notification renderer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            n.color != null
            nb.color = n.color
            nb.setColorized(true)
        }

        val grpKey = PushNotificationUtil.getRandomString(10)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val postTime = notification.postTime
            val timeoutLeft = n.timeoutAfter
            val timeOutAfter = PushNotificationUtil.calculateTimeOutAfter(postTime, timeoutLeft)
            Logger.d(tag, "timeout before: " + timeoutLeft / 1000 / 60 + " timeout after: " + timeOutAfter / 1000 / 60)
            nb.setTimeoutAfter(timeOutAfter)
        }
        // uncommon
        //
        nb
                .setContentText(n.extras.getString(Constants.NOTIF_MSG))
                .setContentIntent(n.contentIntent)
                .setAutoCancel(true)
                .setSmallIcon(n.icon)
                .setShowWhen(false)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setGroup(grpKey)
        val contentView = n.contentView
        nb.setContent(contentView)
                .setCustomContentView(contentView)
                .setCustomBigContentView(contentView)
                .setCustomHeadsUpContentView(contentView)

        // set priority build and notify
        nb.priority = NotificationCompat.PRIORITY_MAX

        try {
            val notificationId = (Math.random() * 100).toInt()
            val notif = nb.build()
            nm.cancel(notification.id)
            nm.notify(notificationId, notif)
        } catch (e: Exception) {
            Logger.d(tag, "re trigger notification failed with error: $e")
        }
    }


}
