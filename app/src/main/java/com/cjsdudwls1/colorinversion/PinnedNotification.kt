package com.cjsdudwls1.colorinversion

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build

/**
 * 상태바(알림창)에 고정되는 색상 반전 알림.
 * 알림 본문이나 버튼을 누르면 바로 켜짐/꺼짐이 바뀝니다.
 */
object PinnedNotification {

    private const val CHANNEL_ID = "pinned_toggle"
    private const val NOTIFICATION_ID = 1
    private const val PREFS = "settings"
    private const val KEY_ENABLED = "pinned_notification"
    private const val REQ_TAP = 20
    private const val REQ_OPEN = 21

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).apply()
        StatusSync.updateAll(context)
    }

    /** Android 13+ 에서 알림 권한이 있는지. 그 이하에서는 항상 true. */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun refresh(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!isEnabled(context) || !canPost(context)) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
        )

        val permitted = Inversion.hasPermission(context)
        val state = Inversion.readState(context)
        val on = state == true
        val title = context.getString(if (on) R.string.notif_title_on else R.string.notif_title_off)
        val text = context.getString(
            when {
                !permitted -> R.string.notif_text_setup
                on -> R.string.notif_text_on
                else -> R.string.notif_text_off
            }
        )
        val tap = StatusSync.tapIntent(context, REQ_TAP)
        val actionLabel = context.getString(
            when {
                !permitted -> R.string.notif_action_setup
                on -> R.string.notif_action_off
                else -> R.string.notif_action_on
            }
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_tile), actionLabel, tap
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_tile),
                    context.getString(R.string.notif_action_open),
                    StatusSync.openAppIntent(context, REQ_OPEN),
                ).build()
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
