package com.companion.yogawellness.motor

import com.companion.yogawellness.codex.AsanaCatalog32
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class MindState(val label: String) {
    CALM_FOCUSED("Calm & Focused"),
    STRESS("Stress Detected"),
    FATIGUE("Fatigue Detected"),
    DISTRACTED("Focus Drifting"),
    SETTLING("Settling In")
}

/** Live or session-level emotional read-out. Scores are 0..100. */
data class EmotionEstimate(
    val calm: Int,
    val stress: Int,
    val fatigue: Int,
    val focus: Int,
    val state: MindState,
    /** Short motor cue for the live chip, e.g. "Elevated Tremor", "Steady Hold". */
    val cue: String,
    val calmReason: String,
    val stressReason: String,
    val fatigueReason: String,
    val focusReason: String
) {
    /** Score shown next to [state] on the chip. */
    val headlineScore: Int
        get() = when (state) {
            MindState.STRESS -> stress
            MindState.FATIGUE -> fatigue
            MindState.DISTRACTED -> 100 - focus
            MindState.CALM_FOCUSED, MindState.SETTLING -> calm
        }

    /** "Stress Detected (74%) • Elevated Tremor" */
    val chipText: String get() = "${state.label} ($headlineScore%) • $cue"
}

data class SuggestedAsana(val id: String, val name: String, val why: String)

/** One session's Mental Wellness & Motor Report (stored as JSON by [WellnessHistoryStore]). */
data class MentalWellnessReport(
    /** Matches PracticeSessionEntity.startedAtMs, the key History looks it up by. */
    val sessionStartedAtMs: Long,
    val createdAtMs: Long,
    val calm: Int,
    val stress: Int,
    val fatigue: Int,
    val focus: Int,
    val calmReason: String,
    val stressReason: String,
    val fatigueReason: String,
    val focusReason: String,
    val headline: String,
    val state: MindState,
    val suggestions: List<SuggestedAsana>,
    /** Stress in the first vs. last third of the session (null when too short to compare). */
    val startStress: Int? = null,
    val endStress: Int? = null,
    /** True when rebuilt from saved session metrics rather than measured live. */
    val estimated: Boolean = false
) {
    val trendLine: String?
        get() {
            val a = startStress ?: return null
            val b = endStress ?: return null
            return when {
                a - b >= 12 -> "Started tense (stress $a%), ended calmer ($b%)"
                b - a >= 12 -> "Tension built up during practice (stress $a% → $b%)"
                else -> "Stress stayed steady through the session (~$b%)"
            }
        }
}

/**
 * Weighted, deterministic, explainable rules: motor features -> Calm / Stress / Fatigue / Focus.
 * Every score carries the plain-English motor evidence behind it, so the logic is visible.
 *
 * Tuned so deliberately shaking the hands in a pose pushes the tremor index to ~1 within 2–3 s,
 * which floors Stress at ~70% and flips the live chip to "Stress Detected • Elevated Tremor".
 */
object EmotionEstimator {

    private fun pct(v: Double) = (v * 100).roundToInt().coerceIn(0, 100)
    private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

    fun estimate(s: MotorFeatureSnapshot): EmotionEstimate {
        if (!s.valid) {
            return EmotionEstimate(
                calm = 50, stress = 0, fatigue = 0, focus = 50, state = MindState.SETTLING,
                cue = if (s.phoneMoving) "Phone Moving" else "Reading Motion",
                calmReason = "Collecting motor data…", stressReason = "Collecting motor data…",
                fatigueReason = "Collecting motor data…", focusReason = "Collecting motor data…"
            )
        }
        val earlyExitRate = (s.earlyExits / max(1.0, (s.earlyExits + s.holdsCompleted).toDouble())).coerceIn(0.0, 1.0)
        val slowness = if (s.transitionSpeed > 0) MotorFeatureExtractor.ramp(0.45 - s.transitionSpeed, 0.0, 0.3) else 0.0
        return score(
            tremor = s.tremorIndex, tremorRatio = s.tremorRatio, sway = s.swayIndex, jerk = s.jerkIndex,
            holdRatio = s.holdRatio, earlyExitRate = earlyExitRate, earlyExits = s.earlyExits,
            slowness = slowness, swayDrift = 0.0, live = true, inHold = s.inHold
        )
    }

    /** Builds the post-session report from the extractor's whole-session aggregate. */
    fun report(sessionStartedAtMs: Long, agg: MotorSessionAggregate, nowMs: Long = System.currentTimeMillis()): MentalWellnessReport {
        val earlyExitRate = (agg.earlyExits / max(1.0, (agg.earlyExits + agg.holdsCompleted).toDouble())).coerceIn(0.0, 1.0)
        val slowness = if (agg.meanTransitionSpeed > 0) MotorFeatureExtractor.ramp(0.45 - agg.meanTransitionSpeed, 0.0, 0.3) else 0.0
        val drift = (agg.lateSway - agg.earlySway).coerceIn(0.0, 1.0)
        // The session mean dilutes sustained shaking; blend in its 75th percentile.
        val tremor = 0.5 * agg.meanTremorIndex + 0.5 * agg.p75TremorIndex
        val e = score(
            tremor = tremor, tremorRatio = agg.peakTremorRatio, sway = agg.meanSwayIndex, jerk = agg.meanJerkIndex,
            holdRatio = agg.holdRatio, earlyExitRate = earlyExitRate, earlyExits = agg.earlyExits,
            slowness = slowness, swayDrift = drift, live = false, inHold = true
        )
        val third = agg.stressTimeline.size / 3
        val start = if (third >= 1) agg.stressTimeline.take(third).map { it.second }.average().roundToInt() else null
        val end = if (third >= 1) agg.stressTimeline.takeLast(third).map { it.second }.average().roundToInt() else null
        return toReport(sessionStartedAtMs, nowMs, e, start, end, estimated = false)
    }

    internal fun toReport(
        startedAtMs: Long, nowMs: Long, e: EmotionEstimate, start: Int?, end: Int?, estimated: Boolean
    ): MentalWellnessReport = MentalWellnessReport(
        sessionStartedAtMs = startedAtMs,
        createdAtMs = nowMs,
        calm = e.calm, stress = e.stress, fatigue = e.fatigue, focus = e.focus,
        calmReason = e.calmReason, stressReason = e.stressReason,
        fatigueReason = e.fatigueReason, focusReason = e.focusReason,
        headline = headlineFor(e),
        state = e.state,
        suggestions = suggestionsFor(e),
        startStress = start, endStress = end,
        estimated = estimated
    )

    internal fun score(
        tremor: Double, tremorRatio: Double, sway: Double, jerk: Double,
        holdRatio: Double, earlyExitRate: Double, earlyExits: Int,
        slowness: Double, swayDrift: Double, live: Boolean, inHold: Boolean
    ): EmotionEstimate {
        // ---- Stress: tremor-led, with agitation (jerk) and sway --------------------------------
        var stressV = 0.55 * tremor + 0.25 * jerk + 0.20 * sway
        // Strong tremor alone is enough evidence: it floors stress so the demo flip is decisive.
        if (tremor >= 0.5) stressV = max(stressV, 0.55 + 0.35 * tremor)
        val stress = pct(stressV)

        // ---- Calm: the absence of tremor, jerk and sway ----------------------------------------
        val calm = pct(1.0 - (0.45 * tremor + 0.30 * jerk + 0.25 * sway))

        // ---- Focus: steady base + sustained holds ----------------------------------------------
        val focus = pct(0.45 * (1 - sway) + 0.35 * holdRatio + 0.20 * (1 - earlyExitRate) - 0.15 * tremor)

        // ---- Fatigue: short holds, early exits, slow transitions, sway drifting up -------------
        val fatigue = pct(0.35 * (1 - holdRatio) + 0.25 * earlyExitRate + 0.20 * slowness + 0.20 * swayDrift)

        val state = when {
            stress >= 55 -> MindState.STRESS
            fatigue >= 55 -> MindState.FATIGUE
            focus < 45 -> MindState.DISTRACTED
            calm >= 60 && focus >= 55 -> MindState.CALM_FOCUSED
            else -> MindState.SETTLING
        }

        val cue = when {
            tremor >= 0.4 -> "Elevated Tremor"
            sway >= 0.5 -> "High Sway"
            jerk >= 0.5 -> "Jerky Transitions"
            earlyExits >= 2 && holdRatio < 0.6 -> "Early Exits"
            inHold -> "Steady Hold"
            else -> "Smooth Flow"
        }

        val tremorTxt = when {
            tremor >= 0.4 -> "Elevated wrist/ankle micro-tremor (${f1(tremorRatio)}× baseline)"
            tremor >= 0.15 -> "Mild micro-tremor (${f1(tremorRatio)}× baseline)"
            else -> "Low micro-jitter"
        }
        val jerkTxt = when {
            jerk >= 0.5 -> "rushed, jerky transitions"
            jerk >= 0.2 -> "slightly abrupt transitions"
            else -> "smooth transition velocity"
        }
        val swayTxt = when {
            sway >= 0.5 -> "high postural sway in hip/shoulder midpoint"
            sway >= 0.2 -> "moderate postural sway"
            else -> "stable centre of mass"
        }
        val holdTxt = when {
            earlyExits >= 2 -> "$earlyExits early pose exits"
            holdRatio >= 0.85 -> "holds sustained to target"
            holdRatio >= 0.5 -> "holds partly sustained"
            else -> "short holds"
        }

        val stressReason = "$tremorTxt + $jerkTxt"
        val calmReason = if (calm >= 60) "${tremorTxt.replaceFirstChar { it.uppercase() }} + $jerkTxt"
        else "Calm reduced by ${listOfNotNull(
            "tremor".takeIf { tremor >= 0.15 }, "abrupt movement".takeIf { jerk >= 0.2 }, "sway".takeIf { sway >= 0.2 }
        ).ifEmpty { listOf("mixed motor signals") }.joinToString(", ")}"
        val focusReason = "${swayTxt.replaceFirstChar { it.uppercase() }} + $holdTxt"
        val fatigueReason = buildList {
            if (holdRatio < 0.6) add("holds ended at ${pct(holdRatio)}% of target")
            if (earlyExitRate >= 0.3) add("frequent early exits")
            if (slowness >= 0.3) add("slowing transitions")
            if (swayDrift >= 0.15) add("sway increased late in the session")
        }.ifEmpty { listOf(if (live) "No fatigue markers so far" else "No fatigue markers: holds and tempo stayed even") }
            .joinToString(" + ").replaceFirstChar { it.uppercase() }

        return EmotionEstimate(calm, stress, fatigue, focus, state, cue, calmReason, stressReason, fatigueReason, focusReason)
    }

    private fun headlineFor(e: EmotionEstimate): String = when (e.state) {
        MindState.STRESS -> "Your movement showed signs of tension (stress ${e.stress}%)"
        MindState.FATIGUE -> "Your body showed signs of tiredness (fatigue ${e.fatigue}%)"
        MindState.DISTRACTED -> "Your balance suggests a wandering mind (focus ${e.focus}%)"
        MindState.CALM_FOCUSED -> "Calm and focused practice (calm ${e.calm}%, focus ${e.focus}%)"
        MindState.SETTLING -> "A balanced session (calm ${e.calm}%)"
    }

    /** 2–3 poses from the 32-pose catalog, picked for the highest need. */
    fun suggestionsFor(e: EmotionEstimate): List<SuggestedAsana> {
        val plan: List<Pair<String, String>> = when {
            e.stress >= 45 && e.stress >= e.fatigue -> listOf(
                "childs_pose" to "Grounds the body and slows the breath to ease tension",
                "seated_forward_bend" to "A quiet forward fold that calms an over-active system",
                "corpse_relaxation" to "Lets residual muscle tremor settle completely"
            )
            e.fatigue >= 45 -> listOf(
                "corpse_relaxation" to "Full rest to recover from tiredness",
                "bound_angle" to "Gentle, supported hip opener with little effort",
                "bridge_pose" to "Light back-body activation to lift energy"
            )
            e.focus < 55 -> listOf(
                "mountain" to "Re-centres your stance and attention",
                "tree_pose" to "Single-leg balance that trains steady focus",
                "childs_pose" to "A pause to reset before trying again"
            )
            else -> listOf(
                "cat_cow" to "Keep the smooth, breath-led movement you showed",
                "tree_pose" to "Your steadiness is ready for a balance challenge",
                "corpse_relaxation" to "Close the practice and lock in the calm"
            )
        }
        return plan.mapNotNull { (id, why) ->
            AsanaCatalog32.byId(id)?.let { SuggestedAsana(id, "${it.sanskritName} · ${it.englishName}", why) }
        }
    }
}

/**
 * Smooths live scores over ~1 s so the chip does not flicker, and adds hysteresis to the state so
 * it flips within 3–4 s of a real change but not on single-frame noise.
 */
class LiveEmotionSmoother(private val tauSec: Double = 0.8) {
    private var calm = 50.0
    private var stress = 0.0
    private var fatigue = 0.0
    private var focus = 50.0
    private var lastMs = -1L
    private var shownState = MindState.SETTLING
    private var pendingState: MindState? = null
    private var pendingSinceMs = 0L

    fun reset() {
        calm = 50.0; stress = 0.0; fatigue = 0.0; focus = 50.0; lastMs = -1L
        shownState = MindState.SETTLING; pendingState = null
    }

    fun update(raw: EmotionEstimate, nowMs: Long): EmotionEstimate {
        val dt = if (lastMs < 0) 1.0 else ((nowMs - lastMs) / 1000.0).coerceIn(0.0, 1.0)
        lastMs = nowMs
        val a = (dt / (tauSec + dt)).coerceIn(0.0, 1.0)
        calm += a * (raw.calm - calm)
        stress += a * (raw.stress - stress)
        fatigue += a * (raw.fatigue - fatigue)
        focus += a * (raw.focus - focus)
        val candidate = stateFor(stress.toInt(), fatigue.toInt(), focus.toInt(), calm.toInt(), raw.state)
        if (candidate != shownState) {
            if (pendingState != candidate) { pendingState = candidate; pendingSinceMs = nowMs }
            // Stress shows fast (demo trigger); everything else needs a little longer to stick.
            val need = if (candidate == MindState.STRESS) 600L else 1500L
            if (nowMs - pendingSinceMs >= need) { shownState = candidate; pendingState = null }
        } else {
            pendingState = null
        }
        // Reasons and cue come from the latest raw estimate; while a shown state is still easing
        // off, say so rather than pairing "Stress" with a calm cue.
        val calmCue = raw.cue == "Steady Hold" || raw.cue == "Smooth Flow"
        val cue = if (calmCue && shownState != MindState.CALM_FOCUSED && shownState != MindState.SETTLING) "Easing Off" else raw.cue
        return raw.copy(
            calm = calm.toInt(), stress = stress.toInt(), fatigue = fatigue.toInt(), focus = focus.toInt(),
            state = shownState, cue = cue
        )
    }

    private fun stateFor(stress: Int, fatigue: Int, focus: Int, calm: Int, raw: MindState): MindState = when {
        raw == MindState.SETTLING && calm == 50 && stress == 0 -> MindState.SETTLING
        stress >= 55 -> MindState.STRESS
        fatigue >= 55 -> MindState.FATIGUE
        focus < 45 -> MindState.DISTRACTED
        calm >= 60 && focus >= 55 -> MindState.CALM_FOCUSED
        else -> MindState.SETTLING
    }
}
