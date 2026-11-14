package com.companion.yogawellness.pose

import kotlin.math.sqrt

/** Phone sensor readings that accompany a frame (null when replaying a recorded clip). */
data class SensorSnapshot(
    val tilt: PhoneTiltStatus?,
    /** Standard deviation of |acceleration| over the last ~1 s, in m/s². */
    val motionSd: Double?
)

enum class PreflightCheckId(val label: String) {
    FULL_BODY("Full body"),
    DISTANCE("Distance"),
    PHONE("Phone steady"),
    LIGHTING("Lighting")
}

enum class PreflightProfile {
    STANDING,
    SEATED,
    FLOOR_SIDE
}

data class PreflightCheck(val id: PreflightCheckId, val ok: Boolean, val message: String?)

data class PreflightResult(val checks: List<PreflightCheck>) {
    val allGreen: Boolean get() = checks.all { it.ok }
    val primaryMessage: String? get() = checks.firstOrNull { !it.ok }?.message
    fun check(id: PreflightCheckId): PreflightCheck = checks.first { it.id == id }
}

/**
 * Pre-flight guard (§5.1). Must be green before scoring starts:
 *  1. Nose, wrists, ankles visible (>= 0.60) and inside [0.04, 0.96]
 *  2. Torso length 18–38% of frame height
 *  3. Phone pitch/roll <= 15° and |a| SD < 0.3 m/s²
 *  4. Mean luminance of the centre crop >= 50
 */
object PreflightGuard {
    const val LANDMARK_VISIBILITY_MIN = 0.60f
    const val EDGE_MARGIN = 0.04f
    const val TORSO_MIN_FRACTION = 0.18
    const val TORSO_MAX_FRACTION = 0.38
    const val MOTION_SD_MAX = 0.3
    const val LUMINANCE_MIN = 50.0

    private val REQUIRED_STANDING = listOf(
        PoseLandmarkIndex.NOSE,
        PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.RIGHT_WRIST,
        PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.RIGHT_ANKLE
    )

    private val REQUIRED_SEATED = listOf(
        PoseLandmarkIndex.NOSE,
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP
    )

    private val REQUIRED_FLOOR = listOf(
        PoseLandmarkIndex.NOSE,
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP
    )

    fun evaluate(
        frame: PoseFrame?,
        sensors: SensorSnapshot?,
        profile: PreflightProfile = PreflightProfile.STANDING
    ): PreflightResult {
        val checks = ArrayList<PreflightCheck>(4)

        val requiredLms = when (profile) {
            PreflightProfile.STANDING -> REQUIRED_STANDING
            PreflightProfile.SEATED -> REQUIRED_SEATED
            PreflightProfile.FLOOR_SIDE -> REQUIRED_FLOOR
        }

        // 1. Whole body in frame
        checks += when {
            frame == null || !frame.hasPose ->
                PreflightCheck(PreflightCheckId.FULL_BODY, false, "Step into view so the camera can see you")
            else -> {
                val bad = requiredLms.filter { i ->
                    val lm = frame[i]
                    lm.visibility < LANDMARK_VISIBILITY_MIN ||
                        lm.x < EDGE_MARGIN || lm.x > 1f - EDGE_MARGIN ||
                        lm.y < EDGE_MARGIN || lm.y > 1f - EDGE_MARGIN
                }
                if (bad.isEmpty()) PreflightCheck(PreflightCheckId.FULL_BODY, true, null)
                else PreflightCheck(PreflightCheckId.FULL_BODY, false, fullBodyMessage(bad))
            }
        }

        // 2. Camera distance
        checks += if (frame == null || !frame.hasPose) {
            when (profile) {
                PreflightProfile.STANDING -> PreflightCheck(PreflightCheckId.DISTANCE, false, "Stand about 2–3 m from the phone")
                PreflightProfile.SEATED -> PreflightCheck(PreflightCheckId.DISTANCE, false, "Sit about 1.5–2 m from the phone")
                PreflightProfile.FLOOR_SIDE -> PreflightCheck(PreflightCheckId.DISTANCE, false, "Place phone on the floor about 2 m away")
            }
        } else {
            val fraction = torsoFraction(frame)
            val minFrac = when (profile) {
                PreflightProfile.STANDING -> TORSO_MIN_FRACTION
                PreflightProfile.SEATED -> 0.22
                PreflightProfile.FLOOR_SIDE -> 0.15
            }
            val maxFrac = when (profile) {
                PreflightProfile.STANDING -> TORSO_MAX_FRACTION
                PreflightProfile.SEATED -> 0.58
                PreflightProfile.FLOOR_SIDE -> 0.65
            }
            when {
                fraction > maxFrac -> PreflightCheck(PreflightCheckId.DISTANCE, false, "Step back a little from the phone")
                fraction < minFrac -> PreflightCheck(PreflightCheckId.DISTANCE, false, "Come a little closer to the phone")
                else -> PreflightCheck(PreflightCheckId.DISTANCE, true, null)
            }
        }

        // 3. Phone upright & steady (sensor not available -> not blocking, e.g. replay)
        val tilt = sensors?.tilt
        val motion = sensors?.motionSd
        checks += when {
            profile != PreflightProfile.FLOOR_SIDE && tilt != null && !tilt.isUprightOk -> PreflightCheck(
                PreflightCheckId.PHONE, false,
                tilt.warningMessage ?: "Prop your phone slightly more upright (<15° tilt) for accurate vertical alignment."
            )
            motion != null && motion >= MOTION_SD_MAX ->
                PreflightCheck(PreflightCheckId.PHONE, false, "Prop your phone against something steady")
            else -> PreflightCheck(PreflightCheckId.PHONE, true, null)
        }

        // 4. Lighting
        val lum = frame?.meanLuminance
        checks += if (lum != null && lum < LUMINANCE_MIN) {
            PreflightCheck(PreflightCheckId.LIGHTING, false, "The room is too dark — turn on a light or face a window")
        } else {
            PreflightCheck(PreflightCheckId.LIGHTING, true, null)
        }

        return PreflightResult(checks)
    }

    fun torsoFraction(frame: PoseFrame): Double =
        PoseGeometry(frame, 0.0).torsoLengthPx / frame.imageHeight

    private fun fullBodyMessage(bad: List<Int>): String {
        val feet = bad.any { it == PoseLandmarkIndex.LEFT_ANKLE || it == PoseLandmarkIndex.RIGHT_ANKLE }
        val hands = bad.any { it == PoseLandmarkIndex.LEFT_WRIST || it == PoseLandmarkIndex.RIGHT_WRIST }
        return when {
            feet && hands -> "Step back so your whole body is in the frame"
            feet -> "Make sure your feet are in the frame"
            hands -> "Make sure both hands are in the frame"
            else -> "Make sure your head is in the frame"
        }
    }
}

/** Rolling SD of accelerometer magnitude, used for the "phone steady" check. */
class MotionSteadinessTracker(private val windowNanos: Long = 1_000_000_000L) {
    private val times = ArrayDeque<Long>()
    private val mags = ArrayDeque<Double>()

    @Synchronized
    fun add(timestampNanos: Long, ax: Float, ay: Float, az: Float) {
        mags.addLast(sqrt((ax * ax + ay * ay + az * az).toDouble()))
        times.addLast(timestampNanos)
        while (times.isNotEmpty() && timestampNanos - times.first() > windowNanos) {
            times.removeFirst()
            mags.removeFirst()
        }
    }

    /** SD in m/s², or null with fewer than 5 samples. */
    @Synchronized
    fun sd(): Double? {
        if (mags.size < 5) return null
        val mean = mags.average()
        return sqrt(mags.sumOf { (it - mean) * (it - mean) } / mags.size)
    }
}

object LuminanceMeter {
    /**
     * Mean Rec.601 luma (0–255) of the centre 50% crop of an ARGB_8888 pixel array,
     * sampling every [stride]th pixel in both directions.
     */
    fun meanLuminance(argb: IntArray, width: Int, height: Int, stride: Int = 6): Double {
        val x0 = width / 4
        val x1 = width * 3 / 4
        val y0 = height / 4
        val y1 = height * 3 / 4
        var sum = 0.0
        var n = 0
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val p = argb[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                sum += 0.299 * r + 0.587 * g + 0.114 * b
                n++
                x += stride
            }
            y += stride
        }
        return if (n == 0) 0.0 else sum / n
    }
}
