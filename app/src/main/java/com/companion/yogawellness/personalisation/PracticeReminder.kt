package com.companion.yogawellness.personalisation

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.companion.yogawellness.MainActivity
import com.companion.yogawellness.R
import com.companion.yogawellness.YogaWellnessApp
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Optional daily practice reminder: a local notification at the user's chosen hour. Off by default.
 * Scheduled with AlarmManager (inexact, battery-friendly) — no network, no extra library. The only
 * permissions are POST_NOTIFICATIONS (asked when the user switches it on, Android 13+) and
 * RECEIVE_BOOT_COMPLETED (to re-arm it after a restart).
 */
object PracticeReminder {
    const val CHANNEL_ID = "practice_reminders"
    private const val REQUEST_CODE = 4416
    private const val NOTIFICATION_ID = 7

    /** Default hour when the user has no practice pattern yet. */
    const val DEFAULT_HOUR = 7

    /** The next time [hour]:00 comes round, strictly after [now]. */
    fun nextTrigger(now: LocalDateTime, hour: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(hour.coerceIn(0, 23), 0)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Arms or cancels the alarm to match the stored setting. Safe to call any time. */
    fun sync(context: Context) {
        val prefs = (context.applicationContext as YogaWellnessApp).preferences
        if (prefs.reminderEnabled) schedule(context, prefs.reminderHour) else cancel(context)
    }

    fun schedule(context: Context, hour: Int) {
        val at = nextTrigger(LocalDateTime.now(), hour).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms(context).setInexactRepeating(AlarmManager.RTC_WAKEUP, at, AlarmManager.INTERVAL_DAY, pending(context))
    }

    fun cancel(context: Context) {
        alarms(context).cancel(pending(context))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun alarms(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, PracticeReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal const val ACTION_REMIND = "com.companion.yogawellness.action.PRACTICE_REMINDER"

    internal fun post(context: Context) {
        if (!canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Practice reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A daily nudge at the time you chose in Settings."
            }
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_practice)
            .setContentTitle("Time for a little practice")
            .setContentText("Your suggestion for now is ready. A few minutes is plenty.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post: say nothing.
        }
    }
}

/** Fires the daily reminder, and re-arms it after a reboot or an app update. */
class PracticeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = (context.applicationContext as YogaWellnessApp).preferences
        when (intent.action) {
            PracticeReminder.ACTION_REMIND -> if (prefs.reminderEnabled) PracticeReminder.post(context)
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> PracticeReminder.sync(context)
        }
    }
}
