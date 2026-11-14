package com.companion.yogawellness.motor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.wellness.SessionWithPoses
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Keeps each session's [MentalWellnessReport] without touching the Room schema: one JSON string in
 * SharedPreferences per session, keyed by the session's `startedAtMs` (the same value the Room row
 * holds, so History can find it).
 *
 * Sessions recorded before this feature (and the seeded demo personas) have no stored report. For
 * those [reportFor] rebuilds one deterministically from the motor metrics Room already keeps
 * (stability = sway, hold durations, alignment), flagged [MentalWellnessReport.estimated].
 */
class WellnessHistoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<Long, MentalWellnessReport>()

    fun save(report: MentalWellnessReport) {
        cache[report.sessionStartedAtMs] = report
        try {
            prefs.edit().putString(key(report.sessionStartedAtMs), WellnessReportJson.encode(report)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not save wellness report", e)
        }
    }

    fun stored(startedAtMs: Long): MentalWellnessReport? {
        cache[startedAtMs]?.let { return it }
        val json = prefs.getString(key(startedAtMs), null) ?: return null
        return try {
            WellnessReportJson.decode(json).also { cache[startedAtMs] = it }
        } catch (e: Exception) {
            Log.w(TAG, "Unreadable wellness report", e)
            null
        }
    }

    /** Stored report if there is one, else a deterministic estimate (null for manual logs). */
    fun reportFor(item: SessionWithPoses): MentalWellnessReport? {
        stored(item.session.startedAtMs)?.let { return it }
        if (item.session.source == PracticeSessionEntity.SOURCE_MANUAL) return null
        return cache.getOrPut(item.session.startedAtMs) { estimateFrom(item) }
    }

    companion object {
        private const val PREFS = "wellness_reports"
        private const val TAG = "WellnessHistoryStore"
        private fun key(startedAtMs: Long) = "r_$startedAtMs"

        /**
         * Rebuilds a report from saved Room metrics. Deterministic: the same session always gives
         * the same report (a small seeded variation keeps cards from looking identical).
         */
        fun estimateFrom(item: SessionWithPoses): MentalWellnessReport {
            val s = item.session
            val poses = item.poses
            val rnd = java.util.Random(s.startedAtMs xor (s.id * 1_000_003L))
            fun jitter(scale: Double) = (rnd.nextDouble() - 0.5) * scale

            val stabilities = poses.mapNotNull { it.meanStability }
            val stability = if (stabilities.isEmpty()) 75.0 else stabilities.average()
            val align = s.meanScore ?: 70.0
            val target = MotorFeatureExtractor.DEFAULT_TARGET_HOLD_SEC
            val holdRatio = if (poses.isEmpty()) 0.7
            else poses.map { (it.bestGraceHoldSec / target).coerceIn(0.0, 1.0) }.average()
            val earlyExits = poses.count { it.bestGraceHoldSec < target * MotorFeatureExtractor.EARLY_EXIT_RATIO }
            val completed = poses.count { it.bestGraceHoldSec >= target }

            // Stability (0..100, from SwayStabilityTracker) is the stored sway signal.
            val sway = ((85.0 - stability) / 40.0 + jitter(0.08)).coerceIn(0.0, 1.0)
            // Alignment quality stands in for motor control: poor alignment ~ shakier, jerkier.
            val tremor = ((80.0 - align) / 45.0 + jitter(0.15)).coerceIn(0.0, 1.0) * 0.8
            val jerk = ((78.0 - align) / 50.0 + jitter(0.15)).coerceIn(0.0, 1.0)
            val exitRate = earlyExits / maxOf(1.0, (earlyExits + completed).toDouble())
            val e = EmotionEstimator.score(
                tremor = tremor, tremorRatio = 1.0 + tremor * 2.0, sway = sway, jerk = jerk,
                holdRatio = holdRatio, earlyExitRate = exitRate, earlyExits = earlyExits,
                slowness = 0.0, swayDrift = (jitter(0.3)).coerceIn(0.0, 0.3), live = false, inHold = true
            )
            val start = (e.stress + 8 + jitter(16.0)).roundToInt().coerceIn(0, 100)
            val end = (e.stress - 4 + jitter(10.0)).roundToInt().coerceIn(0, 100)
            return EmotionEstimator.toReport(s.startedAtMs, s.endedAtMs, e, start, end, estimated = true)
        }
    }
}

/** org.json is built into Android; no extra dependency. */
object WellnessReportJson {
    fun encode(r: MentalWellnessReport): String = JSONObject().apply {
        put("v", 1)
        put("startedAtMs", r.sessionStartedAtMs)
        put("createdAtMs", r.createdAtMs)
        put("calm", r.calm); put("stress", r.stress); put("fatigue", r.fatigue); put("focus", r.focus)
        put("calmReason", r.calmReason); put("stressReason", r.stressReason)
        put("fatigueReason", r.fatigueReason); put("focusReason", r.focusReason)
        put("headline", r.headline)
        put("state", r.state.name)
        put("startStress", r.startStress ?: JSONObject.NULL)
        put("endStress", r.endStress ?: JSONObject.NULL)
        put("estimated", r.estimated)
        put("suggestions", JSONArray().apply {
            r.suggestions.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("why", it.why)) }
        })
    }.toString()

    fun decode(json: String): MentalWellnessReport {
        val o = JSONObject(json)
        val arr = o.optJSONArray("suggestions") ?: JSONArray()
        return MentalWellnessReport(
            sessionStartedAtMs = o.getLong("startedAtMs"),
            createdAtMs = o.optLong("createdAtMs"),
            calm = o.getInt("calm"), stress = o.getInt("stress"), fatigue = o.getInt("fatigue"), focus = o.getInt("focus"),
            calmReason = o.optString("calmReason"), stressReason = o.optString("stressReason"),
            fatigueReason = o.optString("fatigueReason"), focusReason = o.optString("focusReason"),
            headline = o.optString("headline"),
            state = runCatching { MindState.valueOf(o.optString("state")) }.getOrDefault(MindState.SETTLING),
            suggestions = (0 until arr.length()).map {
                val s = arr.getJSONObject(it)
                SuggestedAsana(s.optString("id"), s.optString("name"), s.optString("why"))
            },
            startStress = if (o.isNull("startStress")) null else o.optInt("startStress"),
            endStress = if (o.isNull("endStress")) null else o.optInt("endStress"),
            estimated = o.optBoolean("estimated", false)
        )
    }
}
