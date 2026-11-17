package dev.openbehindyou.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.openbehindyou.R
import dev.openbehindyou.detector.DetectionReason
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.ui.MainActivity

/**
 * Status + alert notifications. There is intentionally no overlay: Android
 * requires SYSTEM_ALERT_WINDOW for a true cross-app floating pill, and this app
 * does not request that permission. A persistent notification with a pill-shaped
 * custom view supplies the always-visible indicator; high-importance alerts
 * supply the transient heads-up banner.
 */
object GuardNotifications {
    const val CHANNEL_STATUS = "guard_status_v1"
    const val CHANNEL_ALERTS = "guard_alerts_v1"
    const val STATUS_ID = 4_101
    const val ALERT_ID = 4_102

    private const val OPEN_REQUEST = 7_201
    private const val STOP_REQUEST = 7_202

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.status_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.status_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alert_channel_description)
            setShowBadge(true)
            enableLights(true)
            lightColor = context.getColor(R.color.status_alert)
            // Haptic feedback is issued explicitly so it also works when the
            // channel's sound/vibration switches are changed by the user.
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(statusChannel, alertChannel))
    }

    fun buildStatus(context: Context, snapshot: GuardSnapshot): Notification {
        val active = snapshot.active
        val dot = when {
            !active -> R.drawable.dot_idle
            snapshot.errorRes != null -> R.drawable.dot_idle
            snapshot.starting -> R.drawable.dot_idle
            snapshot.level == SafetyLevel.ALERT -> R.drawable.dot_alert
            snapshot.level == SafetyLevel.CAUTION -> R.drawable.dot_caution
            else -> R.drawable.dot_safe
        }
        val state = when {
            !active -> context.getString(R.string.status_notification_state_idle)
            snapshot.errorRes != null -> context.getString(R.string.status_notification_state_issue)
            snapshot.starting -> context.getString(R.string.status_notification_state_starting)
            snapshot.level == SafetyLevel.ALERT -> context.getString(R.string.status_notification_state_alert)
            snapshot.level == SafetyLevel.CAUTION -> context.getString(R.string.status_notification_state_caution)
            else -> context.getString(R.string.status_notification_state_safe)
        }
        val title = when {
            !active -> context.getString(R.string.status_notification_title_stopped)
            snapshot.starting -> context.getString(R.string.status_notification_title_starting)
            else -> context.getString(R.string.status_notification_title_live)
        }
        val detail = when {
            snapshot.errorRes != null -> context.getString(snapshot.errorRes)
            snapshot.starting -> context.getString(R.string.status_detail_starting)
            !active -> context.getString(R.string.status_detail_off)
            snapshot.level == SafetyLevel.ALERT -> context.getString(R.string.status_detail_alert)
            snapshot.level == SafetyLevel.CAUTION &&
                snapshot.reason == DetectionReason.MOVEMENT_DETECTED ->
                context.getString(R.string.status_detail_caution_motion)

            snapshot.level == SafetyLevel.CAUTION -> context.getString(R.string.status_detail_caution_secondary)
            !snapshot.primaryPresent -> context.getString(R.string.status_detail_no_face)
            else -> context.getString(R.string.status_detail_safe)
        }
        val accent = when {
            !active -> context.getColor(R.color.status_idle)
            snapshot.errorRes != null -> context.getColor(R.color.status_caution)
            snapshot.starting -> context.getColor(R.color.status_idle)
            snapshot.level == SafetyLevel.ALERT -> context.getColor(R.color.status_alert)
            snapshot.level == SafetyLevel.CAUTION -> context.getColor(R.color.status_caution)
            else -> context.getColor(R.color.status_safe)
        }

        val openIntent = openAppPendingIntent(context)
        val views = RemoteViews(context.packageName, R.layout.notification_guard).apply {
            setImageViewResource(R.id.notification_dot, dot)
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_detail, detail)
            setTextViewText(R.id.notification_state, state)
            setTextColor(R.id.notification_state, accent)
            setOnClickPendingIntent(R.id.notification_root, openIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(detail)
            .setColor(accent)
            .setContentIntent(openIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOngoing(active)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (active) {
            builder.addAction(
                0,
                context.getString(R.string.status_notification_stop),
                stopGuardPendingIntent(context),
            )
        }
        return builder.build()
    }

    fun buildAlert(context: Context, reason: DetectionReason): Notification {
        val detail = alertReasonText(context, reason)
        return NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(context.getString(R.string.alert_notification_title))
            .setContentText(detail)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    detail + "\n\n" + context.getString(R.string.alert_notification_expanded),
                ),
            )
            .setColor(context.getColor(R.color.status_alert))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent(context))
            .setAutoCancel(true)
            .setTimeoutAfter(12_000L)
            .setOnlyAlertOnce(false)
            .addAction(
                0,
                context.getString(R.string.alert_notification_stop),
                stopGuardPendingIntent(context),
            )
            .build()
    }

    @SuppressLint("MissingPermission")
    fun notifyStatus(context: Context, snapshot: GuardSnapshot) {
        // POST_NOTIFICATIONS is optional by design: the guard still works and
        // vibrates, while the UI surfaces the notification limitation. Calls
        // are made safe against revocations and OEM notification managers.
        runCatching {
            NotificationManagerCompat.from(context).notify(STATUS_ID, buildStatus(context, snapshot))
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyAlert(context: Context, reason: DetectionReason) {
        runCatching {
            NotificationManagerCompat.from(context).notify(ALERT_ID, buildAlert(context, reason))
        }
    }

    fun cancelAlert(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(ALERT_ID)
        }
    }

    fun cancelAll(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    fun alertReasonText(context: Context, reason: DetectionReason): String = when (reason) {
        DetectionReason.LOOKING_OVER_SHOULDER -> context.getString(R.string.alert_reason_over_shoulder)
        DetectionReason.SECONDARY_FACE_NEARBY -> context.getString(R.string.alert_reason_secondary)
        DetectionReason.MOVEMENT_DETECTED -> context.getString(R.string.alert_reason_motion)
        else -> context.getString(R.string.alert_reason_generic)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            OPEN_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun stopGuardPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GuardService::class.java).setAction(GuardService.ACTION_STOP)
        return PendingIntent.getService(
            context,
            STOP_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
