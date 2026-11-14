package com.companion.yogawellness.motor

import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseLandmarkIndex as L
import com.companion.yogawellness.scoring.HoldStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/** Synthetic standing person: keypoint flicker always, optional 5 Hz hand shaking. */
class MotorEmotionTest {

    private val rnd = Random(7)

    private fun frame(tMs: Long, scale: Float, shakeAmp: Float): PoseFrame {
        val base = HashMap<Int, Pair<Float, Float>>().apply {
            put(L.NOSE, 0.50f to 0.20f)
            put(L.LEFT_SHOULDER, 0.45f to 0.30f); put(L.RIGHT_SHOULDER, 0.55f to 0.30f)
            put(L.LEFT_ELBOW, 0.40f to 0.40f); put(L.RIGHT_ELBOW, 0.60f to 0.40f)
            put(L.LEFT_WRIST, 0.38f to 0.50f); put(L.RIGHT_WRIST, 0.62f to 0.50f)
            put(L.LEFT_HIP, 0.47f to 0.55f); put(L.RIGHT_HIP, 0.53f to 0.55f)
            put(L.LEFT_KNEE, 0.47f to 0.70f); put(L.RIGHT_KNEE, 0.53f to 0.70f)
            put(L.LEFT_ANKLE, 0.47f to 0.85f); put(L.RIGHT_ANKLE, 0.53f to 0.85f)
        }
        val t = tMs / 1000.0
        val lms = (0 until L.LANDMARK_COUNT).map { i ->
            val (bx, by) = base[i] ?: (0.5f to 0.5f)
            // ~1 px flicker on a 640 px frame (2 px for wrists, which MediaPipe tracks worst)
            val n = if (i == L.LEFT_WRIST || i == L.RIGHT_WRIST) 0.003f else 0.0015f
            var x = 0.5f + (bx - 0.5f) * scale + (rnd.nextGaussian() * n).toFloat()
            var y = 0.5f + (by - 0.5f) * scale + (rnd.nextGaussian() * n).toFloat()
            if (i == L.LEFT_WRIST || i == L.RIGHT_WRIST) {
                x += (shakeAmp * scale * sin(2 * PI * 5.0 * t)).toFloat()
                y += (shakeAmp * scale * 0.5 * sin(2 * PI * 5.0 * t + 1.0)).toFloat()
            }
            Landmark(x, y, 0f, 0.95f)
        }
        return PoseFrame(tMs, 480, 640, lms)
    }

    private fun run(seconds: Double, startMs: Long, scale: Float, shake: Float, ex: MotorFeatureExtractor, sm: LiveEmotionSmoother,
                    onFrame: (Long, EmotionEstimate) -> Unit = { _, _ -> }): Long {
        var ms = startMs
        val end = startMs + (seconds * 1000).toLong()
        while (ms < end) {
            val snap = ex.update(frame(ms, scale, shake), null, null, HoldStatus.IDLE, null)
            onFrame(ms, sm.update(EmotionEstimator.estimate(snap), ms))
            ms += 40 // 25 fps
        }
        return ms
    }

    @Test
    fun stillPersonReadsCalm_andHandShakingFlipsToStressWithin4s() {
        val ex = MotorFeatureExtractor()
        val sm = LiveEmotionSmoother()
        var last: EmotionEstimate? = null
        var ms = run(8.0, 0, 1f, 0f, ex, sm) { _, e -> last = e }
        assertTrue("calm expected, got ${last!!.chipText} tremor=${ex.snapshot.tremorIndex}", last!!.state != MindState.STRESS)
        assertTrue(last!!.stress < 40)

        var flippedAt = -1L
        val shakeStart = ms
        ms = run(5.0, ms, 1f, 0.02f, ex, sm) { t, e ->
            if (flippedAt < 0 && e.state == MindState.STRESS) flippedAt = t
            last = e
        }
        assertTrue("never flipped to stress (tremor=${ex.snapshot.tremorIndex})", flippedAt > 0)
        assertTrue("flip took ${flippedAt - shakeStart} ms", flippedAt - shakeStart <= 4000)
        assertEquals("Elevated Tremor", last!!.cue)

        // Calm again after the shaking stops (window drains in ~5 s plus hysteresis).
        run(9.0, ms, 1f, 0f, ex, sm) { _, e -> last = e }
        assertTrue("should settle, got ${last!!.chipText}", last!!.state != MindState.STRESS)
    }

    @Test
    fun cameraDistanceDoesNotChangeTremor() {
        val near = MotorFeatureExtractor(); val far = MotorFeatureExtractor()
        val sm = LiveEmotionSmoother()
        run(6.0, 0, 1f, 0.02f, near, sm)
        run(6.0, 0, 0.5f, 0.02f, far, LiveEmotionSmoother())
        val a = near.snapshot.tremorIndex; val b = far.snapshot.tremorIndex
        assertTrue("near=$a far=$b", a > 0.5 && b > 0.5)
    }

    @Test
    fun phoneMovementGatesTremor() {
        val ex = MotorFeatureExtractor()
        var ms = 0L
        repeat(150) {
            ex.update(frame(ms, 1f, 0.02f), com.companion.yogawellness.pose.SensorSnapshot(null, 1.0), null, HoldStatus.IDLE, null)
            ms += 40
        }
        assertTrue(ex.snapshot.phoneMoving)
        assertEquals(0.0, ex.snapshot.tremorIndex, 1e-9)
    }

    @Test
    fun reportHasFourScoresReasonsAndSuggestions() {
        val ex = MotorFeatureExtractor()
        val sm = LiveEmotionSmoother()
        run(6.0, 0, 1f, 0f, ex, sm)
        run(6.0, 6000, 1f, 0.02f, ex, sm)
        val r = EmotionEstimator.report(123L, ex.aggregate(), nowMs = 1L)
        assertEquals(123L, r.sessionStartedAtMs)
        assertTrue(r.stressReason.isNotBlank() && r.calmReason.isNotBlank() && r.focusReason.isNotBlank() && r.fatigueReason.isNotBlank())
        assertTrue(r.suggestions.size in 2..3)
        assertTrue("stress=${r.stress}", r.stress >= 40)
    }
}
