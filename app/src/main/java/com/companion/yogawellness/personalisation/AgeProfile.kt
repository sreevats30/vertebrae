package com.companion.yogawellness.personalisation

import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.scoring.Asana

/**
 * Everything an [AgeBand] personalises, in one place, so Learn, Home and Practice never repeat the
 * mapping. Age changes what is *suggested* and how long Chair Pose may be held — never how a pose
 * is scored: joint targets and scoring maths are identical for everyone.
 *
 * These are conservative starting values, not clinical rules. They should be reviewed by a
 * qualified yoga teacher or physician before release.
 */
data class AgeProfile(
    val band: AgeBand,
    /** Endurance cap for Utkatasana (Chair Pose), in seconds. Never above [DEFAULT_UTKATASANA_CAP_SEC]. */
    val utkatasanaCapSeconds: Int,
    val suggestedSessionMinutes: IntRange,
    /** Whether the PHQ-9 / GAD-7 self-reflection questionnaires are offered. */
    val wellnessQuestionnairesEnabled: Boolean,
    /** Learn categories to lead with, most relevant first. */
    val preferredCategories: List<AsanaCategory>
) {
    companion object {
        /** The cap every adult band below 51 keeps, and the fallback before a band is chosen. */
        val DEFAULT_UTKATASANA_CAP_SEC: Int = Asana.UTKATASANA.maxHoldSec ?: 30

        private val PROFILES: Map<AgeBand, AgeProfile> = listOf(
            AgeProfile(
                AgeBand.UNDER_18, DEFAULT_UTKATASANA_CAP_SEC, 10..20, wellnessQuestionnairesEnabled = false,
                preferredCategories = listOf(AsanaCategory.MORNING, AsanaCategory.STANDING, AsanaCategory.BALANCE)
            ),
            AgeProfile(
                AgeBand.AGE_18_30, DEFAULT_UTKATASANA_CAP_SEC, 15..30, wellnessQuestionnairesEnabled = true,
                preferredCategories = AsanaCategory.entries.toList()
            ),
            AgeProfile(
                AgeBand.AGE_31_50, DEFAULT_UTKATASANA_CAP_SEC, 15..30, wellnessQuestionnairesEnabled = true,
                preferredCategories = listOf(AsanaCategory.DESK, AsanaCategory.MORNING, AsanaCategory.EVENING)
            ),
            AgeProfile(
                AgeBand.AGE_51_64, 20, 15..25, wellnessQuestionnairesEnabled = true,
                preferredCategories = listOf(AsanaCategory.DESK, AsanaCategory.HIPS, AsanaCategory.EVENING)
            ),
            AgeProfile(
                // "Seated and supported work" has no category of its own; the Desk set is the seated one.
                AgeBand.AGE_65_PLUS, 15, 10..20, wellnessQuestionnairesEnabled = true,
                preferredCategories = listOf(AsanaCategory.EVENING, AsanaCategory.HIPS, AsanaCategory.DESK)
            )
        ).associateBy { it.band }

        fun forBand(band: AgeBand): AgeProfile = PROFILES.getValue(band)

        /** PHQ-9 / GAD-7 are offered unless the band turns them off (on while no band is chosen yet). */
        fun questionnairesEnabledFor(band: AgeBand?): Boolean = band?.let { forBand(it).wellnessQuestionnairesEnabled } ?: true

        /** Chair Pose cap for [band]; the standard 30 s while no band is chosen yet. Never raised above it. */
        fun utkatasanaCapFor(band: AgeBand?): Int =
            (band?.let { forBand(it).utkatasanaCapSeconds } ?: DEFAULT_UTKATASANA_CAP_SEC)
                .coerceIn(1, DEFAULT_UTKATASANA_CAP_SEC)
    }
}
