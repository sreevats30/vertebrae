package com.companion.yogawellness.data

import com.companion.yogawellness.personalisation.PracticeReminder
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.companion.yogawellness.personalisation.AgeBand
import android.content.Context
import androidx.core.content.edit
import com.companion.yogawellness.scoring.ToleranceProfile

/** Small key-value settings kept in private app storage (never backed up, see manifest). */
class AppPreferences(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var toleranceProfile: ToleranceProfile
        get() = sp.getString(KEY_PROFILE, null)
            ?.let { name -> ToleranceProfile.entries.firstOrNull { it.name == name } }
            ?: ToleranceProfile.STANDARD
        set(value) = sp.edit { putString(KEY_PROFILE, value.name) }

    var useFrontCamera: Boolean
        get() = sp.getBoolean(KEY_FRONT_CAMERA, true)
        set(value) = sp.edit { putBoolean(KEY_FRONT_CAMERA, value) }

    var voiceCoachEnabled: Boolean
        get() = sp.getBoolean(KEY_VOICE, true)
        set(value) = sp.edit { putBoolean(KEY_VOICE, value) }

    /** Time the first-run consent & safety screen was accepted (0 = not yet). */
    var consentAcceptedAtMs: Long
        get() = sp.getLong(KEY_CONSENT, 0L)
        set(value) = sp.edit { putLong(KEY_CONSENT, value) }

    val hasConsent: Boolean get() = consentAcceptedAtMs > 0L

    /** Name of the seeded demo persona currently loaded, if any. */
    var demoPersona: String?
        get() = sp.getString(KEY_PERSONA, null)
        set(value) = sp.edit { if (value == null) remove(KEY_PERSONA) else putString(KEY_PERSONA, value) }

    /**
     * The user's own age band, chosen at onboarding and changeable in Settings. `null` until then,
     * which is also what an install from before this setting existed reads — so those users see
     * onboarding once. Demo personas have fixed bands of their own and never write here.
     */
    var ageBand: AgeBand?
        get() = AgeBand.fromStored(sp.getString(KEY_AGE_BAND, null))
        set(value) {
            sp.edit { if (value == null) remove(KEY_AGE_BAND) else putString(KEY_AGE_BAND, value.name) }
            _ageBandState.value = value
        }

    val hasAgeBand: Boolean get() = ageBand != null

    private val _ageBandState = MutableStateFlow(ageBand)

    /** [ageBand] as a stream, so screens react when it is changed in Settings. */
    val ageBandState: StateFlow<AgeBand?> = _ageBandState.asStateFlow()

    /** Optional daily practice reminder (local notification). Off until the user turns it on. */
    var reminderEnabled: Boolean
        get() = sp.getBoolean(KEY_REMINDER_ON, false)
        set(value) = sp.edit { putBoolean(KEY_REMINDER_ON, value) }

    /** Hour of the day (0–23) for the reminder. */
    var reminderHour: Int
        get() = sp.getInt(KEY_REMINDER_HOUR, PracticeReminder.DEFAULT_HOUR)
        set(value) = sp.edit { putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)) }

    /** Delete All My Data: every setting goes, the age band included. */
    fun clearAll() {
        sp.edit { clear() }
        _ageBandState.value = null
    }

    companion object {
        const val FILE = "yoga_wellness_prefs"
        private const val KEY_PROFILE = "tolerance_profile"
        private const val KEY_FRONT_CAMERA = "front_camera"
        private const val KEY_VOICE = "voice_coach"
        private const val KEY_CONSENT = "consent_accepted_at"
        private const val KEY_PERSONA = "demo_persona"
        private const val KEY_AGE_BAND = "age_band"
        private const val KEY_REMINDER_ON = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
    }
}
