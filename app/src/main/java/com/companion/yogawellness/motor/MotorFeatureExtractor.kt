package com.companion.yogawellness.motor

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseLandmarkIndex as L
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.scoring.HoldStatus
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * One immutable reading of the user's motor control, published every processed frame.
 * Distances are in torso lengths (shoulder-midpoint to hip-midpoint), so standing 4 ft or 8 ft
 * from the camera gives the same numbers. Every `*Index` is 0 (calm/steady) .. 1 (strong signal).
 */
data class MotorFeatureSnapshot(
    val timestampMs: Long,
    /** False until there are ~1.5 s of usable landmark frames. */
    val valid: Boolean,
    /** True while the phone itself is moving: tremor and sway are not accumulated then. */
    val phoneMoving: Boolean,
    // Tremor / micro-jitter (high-frequency velocity residual of wrists, ankles, nose)
    val tremorRms: Double,
    val tremorBaseline: Double,
    val tremorIndex: Double,
    // Postural sway (centre of mass vs. ankle base of support, EMA-smoothed)
    val swayVelocity: Double,
    val swayRange: Double,
    val swayIndex: Double,
    // Smoothness: RMS jerk of EMA-smoothed joints while moving
    val jerkRms: Double,
    val jerkIndex: Double,
    /** Mean body speed while moving (torso lengths / s); 0 when no transition seen yet. */
    val transitionSpeed: Double,
    // Hold stability
    val holdRatio: Double,
    val currentHoldSec: Double,
    val targetHoldSec: Double,
    val earlyExits: Int,
    val holdsCompleted: Int,
    val inHold: Boolean
) {
    val tremorRatio: Double get() = if (tremorBaseline > 0) tremorRms / tremorBaseline else 1.0

    companion object {
        val EMPTY = MotorFeatureSnapshot(
            0L, false, false, 0.0, MotorFeatureExtractor.TREMOR_BASELINE_DEFAULT, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, MotorFeatureExtractor.DEFAULT_TARGET_HOLD_SEC, 0, 0, false
        )
    }
}

/** Whole-session motor summary, used to build the post-session report. */
data class MotorSessionAggregate(
    val analysedSec: Double,
    val meanTremorIndex: Double,
    /** 75th percentile of the tremor index: sustained shaking is not diluted by calm stretches. */
    val p75TremorIndex: Double,
    val peakTremorRatio: Double,
    val meanSwayIndex: Double,
    val meanJerkIndex: Double,
    val meanTransitionSpeed: Double,
    val holdRatio: Double,
    val earlyExits: Int,
    val holdsCompleted: Int,
    /** Sway index in the first and last third of the session (fatigue drift). */
    val earlySway: Double,
    val lateSway: Double,
    /** (seconds since start, stress 0..100) every ~2 s, for the pre vs. post comparison. */
    val stressTimeline: List<Pair<Double, Int>>
) {
    val hasData: Boolean get() = analysedSec >= 3.0
}

/**
 * Turns the MediaPipe landmark stream into four motor-control signals (tremor, sway, smoothness,
 * hold stability) over rolling 5 s windows. Pure Kotlin, single-threaded: call [update] from the
 * pipeline coroutine, never the camera thread. Read [snapshot] from anywhere (it is immutable).
 *
 * Guardrails:
 *  - Tremor comes only from landmarks; the accelerometer just gates out phone movement.
 *  - Raw coordinates are EMA-filtered (alpha 0.3) before any velocity / jerk derivative, and jerk is
 *    taken on a 5 Hz resample so 30 fps keypoint flicker is not amplified into fake agitation.
 *  - Everything is divided by the per-frame torso length: no calibration delay.
 */
class MotorFeatureExtractor(
    private val windowSec: Double = 5.0,
    private val alpha: Double = 0.3,
    /** Tremor uses a shorter window so the live chip reacts to shaking within ~3 s. */
    private val tremorWindowSec: Double = 3.0
) {
    @Volatile
    var snapshot: MotorFeatureSnapshot = MotorFeatureSnapshot.EMPTY
        private set

    // ---- per-point state -----------------------------------------------------------------------
    private class PointTrack {
        var hasPrev = false
        var px = 0.0; var py = 0.0          // previous raw position (torso units)
        var vEmaX = 0.0; var vEmaY = 0.0    // EMA of raw velocity (low-frequency part)
        var sx = 0.0; var sy = 0.0          // EMA-smoothed position
        var hasSmooth = false
        fun reset() { hasPrev = false; hasSmooth = false; vEmaX = 0.0; vEmaY = 0.0 }
    }

    private val tremorTracks = HashMap<Int, PointTrack>()
    private val jointTracks = HashMap<Int, PointTrack>()
    private val tremorWindows = HashMap<Int, RollingMean>()

    private val swayWindow = ArrayDeque<DoubleArray>() // [t, x, y]
    private var swaySx = Double.NaN
    private var swaySy = Double.NaN

    // EMA-smoothed joints resampled at 5 Hz, for the v -> a -> j finite differences
    private var lastResampleT = Double.NaN
    private val jointHistory = ArrayDeque<Pair<Double, Map<Int, Pair<Double, Double>>>>()
    private val jerkWindow = RollingMean(windowSec)
    private val speedWindow = RollingMean(windowSec)
    private var lastSpeed = 0.0

    private var tremorBaseline = TREMOR_BASELINE_DEFAULT
    private var lastT = Double.NaN
    private var validSec = 0.0
    private var torsoEma = Double.NaN

    // ---- hold state ----------------------------------------------------------------------------
    private var currentLabel: AsanaLabel? = null
    private var currentBestHold = 0.0
    private var currentTarget = DEFAULT_TARGET_HOLD_SEC
    private var holdRatioSum = 0.0
    private var holdEpisodes = 0
    private var earlyExits = 0
    private var holdsCompleted = 0

    // ---- session aggregate ---------------------------------------------------------------------
    private var sessionStartT = Double.NaN
    private var aggSec = 0.0
    private var aggTremor = 0.0
    private var aggSway = 0.0
    private var aggJerk = 0.0
    private var aggSpeed = 0.0
    private var aggSpeedSec = 0.0
    private var peakTremorRatio = 0.0
    private val swayByTime = ArrayList<Pair<Double, Double>>()
    private val stressTimeline = ArrayList<Pair<Double, Int>>()
    private val tremorSamples = ArrayList<Double>()
    private var lastTimelineT = Double.NaN

    fun reset() {
        tremorTracks.clear(); jointTracks.clear(); tremorWindows.clear()
        swayWindow.clear(); swaySx = Double.NaN; swaySy = Double.NaN
        lastResampleT = Double.NaN; jointHistory.clear()
        jerkWindow.clear(); speedWindow.clear(); lastSpeed = 0.0
        tremorBaseline = TREMOR_BASELINE_DEFAULT
        lastT = Double.NaN; validSec = 0.0; torsoEma = Double.NaN
        currentLabel = null; currentBestHold = 0.0; currentTarget = DEFAULT_TARGET_HOLD_SEC
        holdRatioSum = 0.0; holdEpisodes = 0; earlyExits = 0; holdsCompleted = 0
        sessionStartT = Double.NaN; aggSec = 0.0; aggTremor = 0.0; aggSway = 0.0; aggJerk = 0.0
        aggSpeed = 0.0; aggSpeedSec = 0.0; peakTremorRatio = 0.0
        swayByTime.clear(); stressTimeline.clear(); tremorSamples.clear(); lastTimelineT = Double.NaN
        snapshot = MotorFeatureSnapshot.EMPTY
    }

    /**
     * Feed one processed frame. [lockedLabel] / [hold] / [holdCapSec] come from the pose pipeline
     * output; they drive hold-stability and decide when sway counts (only while a pose is held).
     */
    fun update(
        frame: PoseFrame,
        sensors: SensorSnapshot?,
        lockedLabel: AsanaLabel?,
        hold: HoldStatus,
        holdCapSec: Int?
    ): MotorFeatureSnapshot {
        val t = frame.timestampSec
        val dt = if (lastT.isNaN()) 0.0 else (t - lastT)
        lastT = t
        if (sessionStartT.isNaN()) sessionStartT = t

        updateHold(lockedLabel, hold, holdCapSec)

        // A gap (tracking lost, app paused) breaks every derivative: start them fresh.
        if (dt <= 0.0 || dt > MAX_GAP_SEC) {
            tremorTracks.values.forEach { it.reset() }
            jointTracks.values.forEach { it.reset() }
            jointHistory.clear(); lastResampleT = Double.NaN
        }
        val phoneMoving = (sensors?.motionSd ?: 0.0) > PHONE_MOTION_SD_MAX

        if (!frame.hasPose) return publish(frame.timestampMs, phoneMoving, lockedLabel != null)

        val w = frame.imageWidth.toDouble()
        val h = frame.imageHeight.toDouble()
        fun px(i: Int) = frame[i].x * w
        fun py(i: Int) = frame[i].y * h
        val msx = (px(L.LEFT_SHOULDER) + px(L.RIGHT_SHOULDER)) / 2
        val msy = (py(L.LEFT_SHOULDER) + py(L.RIGHT_SHOULDER)) / 2
        val mhx = (px(L.LEFT_HIP) + px(L.RIGHT_HIP)) / 2
        val mhy = (py(L.LEFT_HIP) + py(L.RIGHT_HIP)) / 2
        val torsoRaw = hypot(msx - mhx, msy - mhy)
        if (torsoRaw < MIN_TORSO_PX) return publish(frame.timestampMs, phoneMoving, lockedLabel != null)
        // Torso length itself flickers a little; a light EMA keeps the normaliser steady.
        torsoEma = if (torsoEma.isNaN()) torsoRaw else torsoEma + 0.1 * (torsoRaw - torsoEma)
        val torso = torsoEma
        val usableDt = dt > 0.0 && dt <= MAX_GAP_SEC

        // ---- 1. Tremor: high-frequency velocity residual ---------------------------------------
        if (usableDt && !phoneMoving) {
            for (idx in TREMOR_POINTS) {
                val lm = frame[idx]
                val tr = tremorTracks.getOrPut(idx) { PointTrack() }
                if (lm.visibility < MIN_VISIBILITY) { tr.reset(); continue }
                val x = px(idx) / torso
                val y = py(idx) / torso
                if (tr.hasPrev) {
                    val vx = (x - tr.px) / dt
                    val vy = (y - tr.py) / dt
                    // A jump of > 0.8 torso in one frame is a tracking glitch, not tremor.
                    if (hypot(x - tr.px, y - tr.py) < GLITCH_TORSO) {
                        tr.vEmaX += alpha * (vx - tr.vEmaX)
                        tr.vEmaY += alpha * (vy - tr.vEmaY)
                        // Residual = what the low-pass velocity cannot explain: oscillation, not travel.
                        val rx = vx - tr.vEmaX
                        val ry = vy - tr.vEmaY
                        tremorWindows.getOrPut(idx) { RollingMean(tremorWindowSec) }.add(t, rx * rx + ry * ry)
                    } else {
                        tr.vEmaX = 0.0; tr.vEmaY = 0.0
                    }
                }
                tr.px = x; tr.py = y; tr.hasPrev = true
            }
        } else if (phoneMoving) {
            tremorTracks.values.forEach { it.reset() }
        }

        // ---- 2. Postural sway: centre of mass relative to ankle base ---------------------------
        val ankleX = (px(L.LEFT_ANKLE) + px(L.RIGHT_ANKLE)) / 2
        val ankleY = (py(L.LEFT_ANKLE) + py(L.RIGHT_ANKLE)) / 2
        val comX = ((msx + mhx) / 2 - ankleX) / torso
        val comY = ((msy + mhy) / 2 - ankleY) / torso
        val anklesSeen = min(frame[L.LEFT_ANKLE].visibility, frame[L.RIGHT_ANKLE].visibility) >= MIN_VISIBILITY
        if (lockedLabel != null && !phoneMoving && anklesSeen) {
            if (swaySx.isNaN()) { swaySx = comX; swaySy = comY }
            swaySx += alpha * (comX - swaySx)
            swaySy += alpha * (comY - swaySy)
            swayWindow.addLast(doubleArrayOf(t, swaySx, swaySy))
            while (swayWindow.isNotEmpty() && t - swayWindow.first()[0] > windowSec) swayWindow.removeFirst()
        } else if (lockedLabel == null) {
            swayWindow.clear(); swaySx = Double.NaN; swaySy = Double.NaN
        }

        // ---- 3. Smoothness: EMA joints -> 5 Hz resample -> v, a, j ------------------------------
        val smoothed = HashMap<Int, Pair<Double, Double>>()
        for (idx in JOINT_POINTS) {
            val lm = frame[idx]
            val tr = jointTracks.getOrPut(idx) { PointTrack() }
            if (lm.visibility < MIN_VISIBILITY) continue
            val x = px(idx) / torso
            val y = py(idx) / torso
            if (!tr.hasSmooth) { tr.sx = x; tr.sy = y; tr.hasSmooth = true }
            tr.sx += alpha * (x - tr.sx)
            tr.sy += alpha * (y - tr.sy)
            smoothed[idx] = tr.sx to tr.sy
        }
        if (lastResampleT.isNaN() || t - lastResampleT >= RESAMPLE_SEC) {
            lastResampleT = t
            jointHistory.addLast(t to smoothed)
            while (jointHistory.size > 4) jointHistory.removeFirst()
            if (jointHistory.size == 4) computeJerk(t)
        }

        validSec += if (usableDt) dt else 0.0
        val snap = publish(frame.timestampMs, phoneMoving, lockedLabel != null)
        accumulate(snap, if (usableDt) dt else 0.0, t)
        return snap
    }

    private fun computeJerk(t: Double) {
        val (t0, p0) = jointHistory[0]
        val (t1, p1) = jointHistory[1]
        val (t2, p2) = jointHistory[2]
        val (t3, p3) = jointHistory[3]
        val common = p0.keys.intersect(p1.keys).intersect(p2.keys).intersect(p3.keys)
        if (common.size < 4) return
        val h = (t3 - t0) / 3.0
        if (h <= 0) return
        var jerkSq = 0.0
        var speed = 0.0
        for (k in common) {
            val (x0, y0) = p0[k]!!; val (x1, y1) = p1[k]!!; val (x2, y2) = p2[k]!!; val (x3, y3) = p3[k]!!
            // Third finite difference over the 5 Hz grid.
            val jx = (x3 - 3 * x2 + 3 * x1 - x0) / (h * h * h)
            val jy = (y3 - 3 * y2 + 3 * y1 - y0) / (h * h * h)
            jerkSq += jx * jx + jy * jy
            speed += hypot(x3 - x2, y3 - y2) / (t3 - t2).coerceAtLeast(1e-3)
        }
        speed /= common.size
        lastSpeed = speed
        // Only movement counts for smoothness: a still body's "jerk" is just residual flicker.
        if (speed >= MOVING_SPEED) {
            jerkWindow.add(t, sqrt(jerkSq / common.size))
            speedWindow.add(t, speed)
        }
    }

    private fun updateHold(label: AsanaLabel?, hold: HoldStatus, holdCapSec: Int?) {
        if (label != currentLabel) {
            currentLabel?.let { finishEpisode() }
            currentLabel = label
            currentBestHold = 0.0
            currentTarget = (holdCapSec?.toDouble() ?: DEFAULT_TARGET_HOLD_SEC).coerceAtMost(DEFAULT_TARGET_HOLD_SEC)
        }
        if (label != null) currentBestHold = max(currentBestHold, hold.bestGraceSec)
    }

    private fun finishEpisode() {
        // A lock that never got going (< 1 s held) is a pass-through, not an exit.
        if (currentBestHold < 1.0) return
        val ratio = (currentBestHold / currentTarget).coerceIn(0.0, 1.0)
        holdRatioSum += ratio
        holdEpisodes++
        if (ratio >= 0.999) holdsCompleted++ else if (ratio < EARLY_EXIT_RATIO) earlyExits++
    }

    private fun holdRatioNow(): Double {
        val live = if (currentLabel != null && currentBestHold >= 1.0) (currentBestHold / currentTarget).coerceIn(0.0, 1.0) else null
        val n = holdEpisodes + (if (live != null) 1 else 0)
        if (n == 0) return 1.0 // nothing to judge yet: assume steady
        return (holdRatioSum + (live ?: 0.0)) / n
    }

    private fun publish(tsMs: Long, phoneMoving: Boolean, inHold: Boolean): MotorFeatureSnapshot {
        // Tremor: mean of the two shakiest extremities (both wrists when the hands shake).
        val perPoint = tremorWindows.values.filter { it.count >= 5 }.map { sqrt(it.mean) }.sortedDescending()
        val tremor = if (perPoint.isEmpty()) 0.0 else perPoint.take(2).average()
        // A slowly adapting "calm" baseline: only quiet windows pull it, so shaking never raises it.
        if (perPoint.isNotEmpty() && tremor < tremorBaseline * 1.3) tremorBaseline += 0.01 * (tremor - tremorBaseline)
        tremorBaseline = tremorBaseline.coerceIn(TREMOR_BASELINE_MIN, TREMOR_BASELINE_DEFAULT * 2.0)
        val tremorIndex = ramp(tremor, tremorBaseline * 1.25, tremorBaseline * TREMOR_FULL_RATIO)

        var swayVel = 0.0
        var swayRange = 0.0
        if (swayWindow.size >= 5) {
            var path = 0.0
            for (i in 1 until swayWindow.size) {
                val a = swayWindow[i - 1]; val b = swayWindow[i]
                path += hypot(b[1] - a[1], b[2] - a[2])
            }
            val span = swayWindow.last()[0] - swayWindow.first()[0]
            swayVel = if (span > 0) path / span else 0.0
            val mx = swayWindow.sumOf { it[1] } / swayWindow.size
            val my = swayWindow.sumOf { it[2] } / swayWindow.size
            swayRange = sqrt(swayWindow.sumOf { (it[1] - mx) * (it[1] - mx) + (it[2] - my) * (it[2] - my) } / swayWindow.size)
        }
        val swayIndex = max(ramp(swayVel, SWAY_VEL_LOW, SWAY_VEL_HIGH), ramp(swayRange, SWAY_RANGE_LOW, SWAY_RANGE_HIGH))

        val jerk = if (jerkWindow.count > 0) jerkWindow.mean else 0.0
        // Log scale: jerk spans orders of magnitude between a slow flow and a rushed one.
        val jerkIndex = if (jerk <= 0) 0.0 else ramp(ln(jerk), ln(JERK_LOW), ln(JERK_HIGH))
        val speed = if (speedWindow.count > 0) speedWindow.mean else 0.0

        val s = MotorFeatureSnapshot(
            timestampMs = tsMs,
            valid = validSec >= MIN_VALID_SEC,
            phoneMoving = phoneMoving,
            tremorRms = tremor,
            tremorBaseline = tremorBaseline,
            tremorIndex = tremorIndex,
            swayVelocity = swayVel,
            swayRange = swayRange,
            swayIndex = swayIndex,
            jerkRms = jerk,
            jerkIndex = jerkIndex,
            transitionSpeed = speed,
            holdRatio = holdRatioNow(),
            currentHoldSec = if (currentLabel != null) currentBestHold else 0.0,
            targetHoldSec = currentTarget,
            earlyExits = earlyExits,
            holdsCompleted = holdsCompleted,
            inHold = inHold
        )
        snapshot = s
        return s
    }

    private fun accumulate(s: MotorFeatureSnapshot, dt: Double, t: Double) {
        if (!s.valid || dt <= 0.0) return
        aggSec += dt
        aggTremor += s.tremorIndex * dt
        aggSway += s.swayIndex * dt
        aggJerk += s.jerkIndex * dt
        if (s.transitionSpeed > 0) { aggSpeed += s.transitionSpeed * dt; aggSpeedSec += dt }
        peakTremorRatio = max(peakTremorRatio, s.tremorRatio)
        val rel = t - sessionStartT
        if (s.inHold) swayByTime += rel to s.swayIndex
        if (lastTimelineT.isNaN() || t - lastTimelineT >= 2.0) {
            lastTimelineT = t
            stressTimeline += rel to EmotionEstimator.estimate(s).stress
            tremorSamples += s.tremorIndex
            if (stressTimeline.size > 900) { stressTimeline.removeAt(0); tremorSamples.removeAt(0) }
        }
    }

    /** Whole-session summary so far (call before [reset]). */
    fun aggregate(): MotorSessionAggregate {
        currentLabel?.let { finishEpisode(); currentLabel = null }
        val n = aggSec.coerceAtLeast(1e-6)
        val third = swayByTime.size / 3
        val early = if (third > 0) swayByTime.take(third).map { it.second }.average() else aggSway / n
        val late = if (third > 0) swayByTime.takeLast(third).map { it.second }.average() else aggSway / n
        return MotorSessionAggregate(
            analysedSec = aggSec,
            meanTremorIndex = aggTremor / n,
            p75TremorIndex = tremorSamples.sorted().let { if (it.isEmpty()) 0.0 else it[(it.size * 3 / 4).coerceAtMost(it.size - 1)] },
            peakTremorRatio = peakTremorRatio,
            meanSwayIndex = aggSway / n,
            meanJerkIndex = aggJerk / n,
            meanTransitionSpeed = if (aggSpeedSec > 0) aggSpeed / aggSpeedSec else 0.0,
            holdRatio = holdRatioNow(),
            earlyExits = earlyExits,
            holdsCompleted = holdsCompleted,
            earlySway = early,
            lateSway = late,
            stressTimeline = stressTimeline.toList()
        )
    }

    /** Time-windowed running mean of samples. */
    private class RollingMean(private val windowSec: Double) {
        private val ts = ArrayDeque<Double>()
        private val vs = ArrayDeque<Double>()
        private var sum = 0.0
        val count: Int get() = vs.size
        val mean: Double get() = if (vs.isEmpty()) 0.0 else sum / vs.size
        fun add(t: Double, v: Double) {
            ts.addLast(t); vs.addLast(v); sum += v
            while (ts.isNotEmpty() && t - ts.first() > windowSec) { ts.removeFirst(); sum -= vs.removeFirst() }
            if (sum < 0) sum = vs.sum() // guard float drift
        }
        fun clear() { ts.clear(); vs.clear(); sum = 0.0 }
    }

    companion object {
        val TREMOR_POINTS = intArrayOf(L.LEFT_WRIST, L.RIGHT_WRIST, L.LEFT_ANKLE, L.RIGHT_ANKLE, L.NOSE)
        val JOINT_POINTS = intArrayOf(
            L.LEFT_SHOULDER, L.RIGHT_SHOULDER, L.LEFT_ELBOW, L.RIGHT_ELBOW, L.LEFT_WRIST, L.RIGHT_WRIST,
            L.LEFT_HIP, L.RIGHT_HIP, L.LEFT_KNEE, L.RIGHT_KNEE, L.LEFT_ANKLE, L.RIGHT_ANKLE
        )

        const val MIN_VISIBILITY = 0.35f
        const val MIN_TORSO_PX = 20.0
        const val MAX_GAP_SEC = 0.5
        const val GLITCH_TORSO = 0.8
        /** Same "phone steady" limit the pre-flight check uses (m/s²). */
        const val PHONE_MOTION_SD_MAX = 0.3
        const val MIN_VALID_SEC = 1.5
        const val RESAMPLE_SEC = 0.2
        const val MOVING_SPEED = 0.25
        const val DEFAULT_TARGET_HOLD_SEC = 15.0
        const val EARLY_EXIT_RATIO = 0.5

        /** Calm residual velocity (keypoint flicker of a still person), torso lengths / s. */
        const val TREMOR_BASELINE_DEFAULT = 0.45
        const val TREMOR_BASELINE_MIN = 0.15
        /** Tremor index reaches 1 at this multiple of the calm baseline. */
        const val TREMOR_FULL_RATIO = 2.8
        const val SWAY_VEL_LOW = 0.06
        const val SWAY_VEL_HIGH = 0.35
        const val SWAY_RANGE_LOW = 0.015
        const val SWAY_RANGE_HIGH = 0.08
        /** Torso lengths / s³ on the 5 Hz grid: slow flow ≈ 3, rushed snap ≈ 60+. */
        const val JERK_LOW = 4.0
        const val JERK_HIGH = 60.0

        fun ramp(v: Double, lo: Double, hi: Double): Double =
            if (hi <= lo) 0.0 else ((v - lo) / (hi - lo)).coerceIn(0.0, 1.0)
    }
}
