package com.companion.yogawellness.personalisation

import android.content.Context
import androidx.core.content.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.data.AppPreferences
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.data.db.WellnessDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The age band lives in [AppPreferences] (there is no profile table to put it on), so the
 * "upgrade keeps everything and the band is null" promise is tested here rather than as a Room
 * migration: the database schema does not change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgeBandStorageTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun cleanSlate() {
        context.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }

    @Test
    fun freshInstallHasNoAgeBand() {
        val prefs = AppPreferences(context)
        assertNull(prefs.ageBand)
        assertFalse(prefs.hasAgeBand)
    }

    /** An install from before this setting: consent and other settings are there, the band is not. */
    @Test
    fun existingInstallKeepsItsSettingsAndReadsNull() {
        context.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE).edit(commit = true) {
            putLong("consent_accepted_at", 1_700_000_000_000L)
            putBoolean("voice_coach", false)
            putString("tolerance_profile", "BEGINNER")
        }
        val prefs = AppPreferences(context)
        assertNull(prefs.ageBand)
        assertTrue(prefs.hasConsent)
        assertFalse(prefs.voiceCoachEnabled)
        assertEquals("BEGINNER", prefs.toleranceProfile.name)
    }

    @Test
    fun everyBandSurvivesARestart() {
        for (band in AgeBand.entries) {
            AppPreferences(context).ageBand = band
            // A second instance stands in for the next launch.
            assertEquals(band, AppPreferences(context).ageBand)
        }
    }

    @Test
    fun bandCanBeChangedAndCleared() {
        val prefs = AppPreferences(context)
        prefs.ageBand = AgeBand.AGE_18_30
        prefs.ageBand = AgeBand.AGE_65_PLUS
        assertEquals(AgeBand.AGE_65_PLUS, prefs.ageBand)
        prefs.ageBand = null
        assertNull(AppPreferences(context).ageBand)
    }

    /** Only the band's name is written — nothing more specific than the band exists to store. */
    @Test
    fun onlyTheBandNameIsStored() {
        AppPreferences(context).ageBand = AgeBand.AGE_51_64
        val raw = context.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE).all
        assertEquals(mapOf("age_band" to "AGE_51_64"), raw)
    }

    @Test
    fun unrecognisedStoredValueReadsAsNotChosen() {
        context.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE).edit(commit = true) {
            putString("age_band", "AGE_200_PLUS")
        }
        assertNull(AppPreferences(context).ageBand)
        assertNull(AgeBand.fromStored(null))
        assertNull(AgeBand.fromStored(""))
    }

    @Test
    fun deleteAllMyDataClearsTheAgeBand() = runTest {
        val prefs = AppPreferences(context)
        prefs.consentAcceptedAtMs = 1L
        prefs.ageBand = AgeBand.AGE_31_50
        val db = Room.inMemoryDatabaseBuilder(context, WellnessDatabase::class.java).allowMainThreadQueries().build()
        try {
            WellnessRepository(db, prefs, privateDirs = emptyList()).deleteAllData()
        } finally {
            db.close()
        }
        assertNull(AppPreferences(context).ageBand)
        assertFalse(AppPreferences(context).hasConsent)
    }

    @Test
    fun fiveBandsWithReadableLabels() {
        assertEquals(
            listOf("Under 18", "18 to 30", "31 to 50", "51 to 64", "65 and over"),
            AgeBand.entries.map { it.label }
        )
    }
}
