package com.companion.yogawellness.personalisation

/**
 * The one personal detail the app asks for. Only the band is ever stored — never a birthdate or an
 * exact age — and it stays on this device (see [com.companion.yogawellness.data.AppPreferences.ageBand]).
 * It drives suggestions and gentler defaults; it never changes how a pose is scored.
 */
enum class AgeBand(val label: String) {
    UNDER_18("Under 18"),
    AGE_18_30("18 to 30"),
    AGE_31_50("31 to 50"),
    AGE_51_64("51 to 64"),
    AGE_65_PLUS("65 and over");

    companion object {
        /** Parses a stored name; anything unrecognised reads as "not chosen yet". */
        fun fromStored(name: String?): AgeBand? = entries.firstOrNull { it.name == name }
    }
}
