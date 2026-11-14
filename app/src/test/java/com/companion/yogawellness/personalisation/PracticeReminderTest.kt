package com.companion.yogawellness.personalisation

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PracticeReminderTest {

    private val app get() = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    @Before
    fun clean() {
        app.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE).edit(commit = true) { clear() }
        PracticeReminder.cancel(app)
    }

    @Test
    fun nextTriggerIsTheNextOccurrence() {
        val now = LocalDateTime.of(2026, 11, 14, 9, 30)
        assertEquals(LocalDateTime.of(2026, 11, 14, 18, 0), PracticeReminder.nextTrigger(now, 18))
        assertEquals(LocalDateTime.of(2026, 11, 15, 7, 0), PracticeReminder.nextTrigger(now, 7))
        assertEquals(LocalDateTime.of(2026, 11, 15, 9, 0), PracticeReminder.nextTrigger(LocalDateTime.of(2026, 11, 14, 9, 0), 9))
    }

    @Test
    fun offByDefaultWithNoAlarm() {
        assertFalse(app.preferences.reminderEnabled)
        assertEquals(PracticeReminder.DEFAULT_HOUR, app.preferences.reminderHour)
        PracticeReminder.sync(app)
        assertNull(alarms.nextScheduledAlarm)
    }

    @Test
    fun turningItOnArmsADailyAlarmAndOffCancelsIt() {
        app.preferences.reminderEnabled = true
        app.preferences.reminderHour = 18
        PracticeReminder.sync(app)
        val alarm = alarms.nextScheduledAlarm
        assertNotNull(alarm)
        assertEquals(AlarmManager.INTERVAL_DAY, alarm!!.interval)

        app.preferences.reminderEnabled = false
        PracticeReminder.sync(app)
        assertNull(alarms.nextScheduledAlarm)
    }

    @Test
    fun firingPostsALocalNotificationOnlyWhenAllowed() {
        app.preferences.reminderEnabled = true
        val remind = Intent(PracticeReminder.ACTION_REMIND)
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PracticeReminderReceiver().onReceive(app, remind)
        assertEquals(0, notifications.allNotifications.size)

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PracticeReminderReceiver().onReceive(app, remind)
        assertEquals(1, notifications.allNotifications.size)
        assertEquals("Time for a little practice", shadowOf(notifications.allNotifications.single()).contentTitle)
    }

    @Test
    fun aDisabledReminderNeverPosts() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PracticeReminderReceiver().onReceive(app, Intent(PracticeReminder.ACTION_REMIND))
        assertEquals(0, notifications.allNotifications.size)
    }

    @Test
    fun bootReArmsAnEnabledReminder() {
        app.preferences.reminderEnabled = true
        PracticeReminderReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNotNull(alarms.nextScheduledAlarm)
    }

    @Test
    fun deleteAllMyDataTurnsTheReminderOff() {
        app.preferences.reminderEnabled = true
        app.preferences.reminderHour = 20
        runBlocking { app.repository.deleteAllData() }
        assertFalse(app.preferences.reminderEnabled)
        assertEquals(PracticeReminder.DEFAULT_HOUR, app.preferences.reminderHour)
    }

    /** The privacy promise survives the new permissions: the merged manifest still has no network access. */
    @Test
    fun mergedManifestHasNoNetworkPermission() {
        val merged = File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
            .takeIf { it.exists() }
            ?: File("build/intermediates").walkTopDown().first { it.name == "AndroidManifest.xml" && it.path.contains("merged_manifest") }
        val xml = merged.readText()
        assertFalse("INTERNET in merged manifest", xml.contains("android.permission.INTERNET"))
        assertFalse(xml.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue(xml.contains("android.permission.POST_NOTIFICATIONS"))
    }
}
