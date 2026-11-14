package com.companion.yogawellness.personalisation

import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.PoseSuitability
import com.companion.yogawellness.data.db.PracticeSessionEntity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil

/** Parts of the day the suggestion rules are written for. */
enum class DayWindow(val startHour: Int, val endHour: Int) {
    MORNING(5, 10), DAYTIME(10, 17), EARLY_EVENING(17, 21), NIGHT(21, 5);

    companion object {
        fun of(hour: Int): DayWindow = when (hour) {
            in 5 until 10 -> MORNING
            in 10 until 17 -> DAYTIME
            in 17 until 21 -> EARLY_EVENING
            else -> NIGHT
        }
    }
}

/** What Home suggests right now. Every pose in [poses] is RECOMMENDED or OK for the band. */
data class SuggestedRoutine(
    val title: String,
    /** One line on why this, now. */
    val reason: String,
    val poses: List<AsanaCodexItem>,
    val minutes: Int,
    val window: DayWindow,
    val calmBecauseOfStress: Boolean
)

/**
 * Rule-based routine suggestion (time of day + age band + latest stress), plus the user's own usual
 * practice time once there are enough sessions to say so. General guidance only.
 */
object RoutineSuggester {

    /** Stress at or above this (out of 10) turns any suggestion into the calm evening set. */
    const val HIGH_STRESS = 7

    /** Sessions needed before a "you usually practise around…" line is shown. */
    const val MIN_SESSIONS_FOR_PATTERN = 5

    const val TIP = "Practise on a light stomach — leave a gap after a heavy meal."

    private const val DESK_BREAK_POSES = 3
    private const val SET_POSES = 4

    fun suggest(
        now: LocalTime,
        band: AgeBand,
        latestStress: Int? = null,
        items: List<AsanaCodexItem> = AsanaCatalog32.items
    ): SuggestedRoutine {
        val window = DayWindow.of(now.hour)
        val stressed = latestStress != null && latestStress >= HIGH_STRESS
        val older = band == AgeBand.AGE_51_64 || band == AgeBand.AGE_65_PLUS
        fun suitable(c: AsanaCategory) =
            items.filter { it.category == c && PoseSuitability.adviceFor(it, band).suitability.isSuggested }
        val evening = suitable(AsanaCategory.EVENING).take(SET_POSES)

        if (stressed || window == DayWindow.NIGHT) {
            return routine(
                "Evening calm",
                if (stressed) "Your last check-in showed high stress, so here is a calm set whatever the time."
                else "Late in the day: slow, quiet poses only — nothing vigorous before bed.",
                evening, window, stressed
            )
        }
        return when (window) {
            DayWindow.MORNING -> {
                // 51+: a longer warm-up first (Mountain, then Cat-Cow), then the gentler morning poses.
                val warmUp = if (older) listOf("mountain", "cat_cow").mapNotNull { id -> items.firstOrNull { it.id == id } } else emptyList()
                val main = suitable(AsanaCategory.MORNING) + suitable(AsanaCategory.DESK)
                val poses = (warmUp + main).distinctBy { it.id }.take(if (older) SET_POSES + 1 else SET_POSES)
                routine(
                    "Morning set",
                    if (older) "A longer warm-up and gentle poses to wake the body." else "Wake the whole body before the day starts.",
                    poses.ifEmpty { evening }, window, false
                )
            }
            DayWindow.DAYTIME -> routine(
                "Desk break",
                if (band == AgeBand.AGE_65_PLUS) "A few minutes of seated and supported moves." else "Two to five minutes to undo sitting.",
                suitable(AsanaCategory.DESK).take(DESK_BREAK_POSES).ifEmpty { evening }, window, false
            )
            else -> {
                val poses = (suitable(AsanaCategory.STANDING) + suitable(AsanaCategory.HIPS)).take(SET_POSES)
                if (poses.size < 2) routine(
                    "Evening calm", "A gentle set suits your age range best at this time.", evening, window, false
                ) else routine("Standing and hips", "Moderate effort while the body is warm from the day.", poses, window, false)
            }
        }
    }

    private fun routine(title: String, reason: String, poses: List<AsanaCodexItem>, w: DayWindow, stressed: Boolean) =
        SuggestedRoutine(title, reason, poses, estimateMinutes(poses), w, stressed)

    /** Each pose held for its recommended time on both sides, with a short transition. */
    fun estimateMinutes(poses: List<AsanaCodexItem>): Int =
        ceil(poses.sumOf { it.recommendedHoldSec * 2 + 20 } / 60.0).toInt().coerceAtLeast(1)

    /**
     * The hour the user most often starts a session, or null with fewer than
     * [MIN_SESSIONS_FOR_PATTERN] sessions — a few data points are not a pattern.
     */
    fun usualPracticeHour(sessions: List<PracticeSessionEntity>, zone: ZoneId = ZoneId.systemDefault()): Int? {
        if (sessions.size < MIN_SESSIONS_FOR_PATTERN) return null
        val hours = sessions.map { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).hour }
        // Most frequent hour; a tie goes to the more recent habit.
        val counts = hours.groupingBy { it }.eachCount()
        val top = counts.values.max()
        return hours.last { counts[it] == top }
    }

    /** "7 AM", "12 PM", "6 PM". */
    fun formatHour(hour: Int): String = when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
