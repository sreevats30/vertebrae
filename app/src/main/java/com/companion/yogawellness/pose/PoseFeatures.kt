package com.companion.yogawellness.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Universal orientation-agnostic geometric features (§4.3) calculated from a [PoseFrame].
 */
data class OrientationAgnosticFeatures(
    val phiDeg: Double,
    val torsoAxis: Vec3,
    val pelvisFrame: PelvisFrame,
    val floorY: Double,
    val hipHeightAboveFloorTorso: Double,
    val ankleSeparationTorso: Double,
    val stanceWidthTorso: Double,
    val stanceAxisAngleDeg: Double,
    val nearSide: BodySide,
    val twistDeg: Double,
    val headTiltDeg: Double,
    val headYawDeg: Double,
    val headPitchDeg: Double,
    val leftKneeDeg: Double,
    val rightKneeDeg: Double,
    val leftArmElevDeg: Double,
    val rightArmElevDeg: Double
)

data class PelvisFrame(
    val lateral: Vec3,
    val up: Vec3,
    val forward: Vec3
)

/**
 * State container for temporal smoothing across frames (e.g. floorY over 0.5 s, neutral twist baseline).
 */
class PoseFeatureExtractor(
    private val floorSmoothingWindowSec: Double = 0.5
) {
    private val floorHistory = ArrayDeque<Pair<Double, Double>>() // (timestampSec, rawFloorY)
    var neutralTwistRatio: Double = 1.0

    fun reset() {
        floorHistory.clear()
        neutralTwistRatio = 1.0
    }

    /**
     * Compute universal floorY (Guardrail 2):
     * Lowest gravity-aligned Y coordinate among all landmarks with visibility > 0.50,
     * smoothed over [floorSmoothingWindowSec] seconds.
     */
    fun computeUniversalFloorY(frame: PoseFrame): Double {
        val visibleLms = frame.landmarks.filter { it.visibility > 0.50f }
        val rawFloorY = if (visibleLms.isEmpty()) {
            frame.imageHeight.toDouble()
        } else {
            // Screen Y points downward, so lowest physically = largest Y value in pixels
            visibleLms.maxOf { (it.y * frame.imageHeight).toDouble() }
        }

        floorHistory.addLast(frame.timestampSec to rawFloorY)
        val cutoff = frame.timestampSec - floorSmoothingWindowSec
        while (floorHistory.size > 1 && floorHistory.first().first < cutoff) {
            floorHistory.removeFirst()
        }

        return floorHistory.map { it.second }.average()
    }

    fun extract(
        frame: PoseFrame,
        zWeight: Double = 0.25
    ): OrientationAgnosticFeatures? {
        if (!frame.hasPose) return null
        val g = PoseGeometry(frame, zWeight)
        val torso = g.torsoLengthPx
        if (torso < 1.0) return null

        val smoothedFloorY = computeUniversalFloorY(frame)

        val midHip = g.midHip
        val midShoulder = g.midShoulder
        val torsoVec = midShoulder - midHip
        val torsoNorm = torsoVec.norm
        val normalizedTorso = if (torsoNorm > 1e-4) torsoVec * (1.0 / torsoNorm) else Vec3(0.0, -1.0, 0.0)

        // phi: angle of torso vector from upward vertical (0 = upright, 90 = horizontal, 180 = inverted)
        // Upward vertical in pixel coordinates is (0, -1, 0)
        val upVec = Vec3(0.0, -1.0, 0.0)
        val dotUp = (normalizedTorso dot upVec).coerceIn(-1.0, 1.0)
        val phiDeg = Math.toDegrees(acos(dotUp))

        // Pelvis frame (Guardrail 4):
        // lateral = rightHip - leftHip
        val rHip = g.point(PoseLandmarkIndex.RIGHT_HIP)
        val lHip = g.point(PoseLandmarkIndex.LEFT_HIP)
        val hipVec = rHip - lHip
        val hipNorm = hipVec.norm
        val lateral = if (hipNorm > 1e-4) hipVec * (1.0 / hipNorm) else Vec3(1.0, 0.0, 0.0)

        // When hip-to-hip vector is within 35 degrees of vertical (such as in Half Moon),
        // cross it with torsoAxis rather than gravity so pelvisFrame never collapses to zero.
        val hipDotUp = abs((lateral dot upVec).coerceIn(-1.0, 1.0))
        val hipAngleFromVerticalDeg = Math.toDegrees(acos(hipDotUp))
        val refUp = if (hipAngleFromVerticalDeg < 35.0 || hipAngleFromVerticalDeg > 145.0) {
            normalizedTorso
        } else {
            upVec
        }

        // forward = lateral x refUp
        var forward = Vec3(
            lateral.y * refUp.z - lateral.z * refUp.y,
            lateral.z * refUp.x - lateral.x * refUp.z,
            lateral.x * refUp.y - lateral.y * refUp.x
        )
        val fwdNorm = forward.norm
        if (fwdNorm > 1e-4) {
            forward = forward * (1.0 / fwdNorm)
        } else {
            forward = Vec3(0.0, 0.0, 1.0)
        }

        // up = forward x lateral
        val frameUp = Vec3(
            forward.y * lateral.z - forward.z * lateral.y,
            forward.z * lateral.x - forward.x * lateral.z,
            forward.x * lateral.y - forward.y * lateral.x
        )

        val pelvisFrame = PelvisFrame(lateral, frameUp, forward)

        val hipHeightAboveFloorTorso = max(0.0, (smoothedFloorY - midHip.y) / torso)

        val lAnkle = g.point(PoseLandmarkIndex.LEFT_ANKLE)
        val rAnkle = g.point(PoseLandmarkIndex.RIGHT_ANKLE)
        val ankleSep = (lAnkle - rAnkle).norm / torso
        val stanceWidth = abs(lAnkle.x - rAnkle.x) / torso

        // Stance axis angle: angle between ankle-to-ankle vector on ground (X-Z) plane and hip line
        val ankleGroundVec = Vec3(rAnkle.x - lAnkle.x, 0.0, rAnkle.z - lAnkle.z)
        val hipGroundVec = Vec3(lateral.x, 0.0, lateral.z)
        val stanceAxisAngleDeg = if (ankleGroundVec.norm > 1e-4 && hipGroundVec.norm > 1e-4) {
            val d = (ankleGroundVec.dot(hipGroundVec) / (ankleGroundVec.norm * hipGroundVec.norm)).coerceIn(-1.0, 1.0)
            Math.toDegrees(acos(abs(d)))
        } else {
            0.0
        }

        // Near side detection: based on ear and shoulder visibility and X order
        val lEarVis = g.visibility(PoseLandmarkIndex.LEFT_EAR)
        val rEarVis = g.visibility(PoseLandmarkIndex.RIGHT_EAR)
        val lShVis = g.visibility(PoseLandmarkIndex.LEFT_SHOULDER)
        val rShVis = g.visibility(PoseLandmarkIndex.RIGHT_SHOULDER)
        val nearSide = if ((lEarVis + lShVis) >= (rEarVis + rShVis)) BodySide.LEFT else BodySide.RIGHT

        // Safe acos in Seated Twist (Guardrail 3):
        // 2D shoulder-to-hip width ratio:
        val rSh = g.point(PoseLandmarkIndex.RIGHT_SHOULDER)
        val lSh = g.point(PoseLandmarkIndex.LEFT_SHOULDER)
        val shWidth2D = hypot(rSh.x - lSh.x, rSh.y - lSh.y)
        val hipWidth2D = max(1.0, hypot(rHip.x - lHip.x, rHip.y - lHip.y))
        val currentRatio = shWidth2D / hipWidth2D
        val baseRatio = max(0.2, neutralTwistRatio)
        val clampedRatio = (currentRatio / baseRatio).coerceIn(0.0, 1.0)
        val twistDeg = Math.toDegrees(acos(clampedRatio))

        // Head angles relative to shoulder line
        val nose = g.point(PoseLandmarkIndex.NOSE)
        val neck = (lSh + rSh) * 0.5
        val headVec = nose - neck
        val headTiltDeg = if (shWidth2D > 1e-4) {
            val shAngle = atan2(rSh.y - lSh.y, rSh.x - lSh.x)
            val headAngle = atan2(headVec.y, headVec.x)
            var diff = Math.toDegrees(abs(headAngle - (shAngle + Math.PI / 2.0)))
            if (diff > 180.0) diff = 360.0 - diff
            diff
        } else 0.0

        val rEar = g.point(PoseLandmarkIndex.RIGHT_EAR)
        val lEar = g.point(PoseLandmarkIndex.LEFT_EAR)
        val earDiffX = (rEar.x - lEar.x)
        val headYawDeg = if (shWidth2D > 1e-4) {
            val yawRatio = (abs(earDiffX) / shWidth2D).coerceIn(0.0, 1.0)
            Math.toDegrees(acos(yawRatio))
        } else 0.0

        val headPitchDeg = if (torso > 1e-4) {
            val noseAboveNeck = (neck.y - nose.y) / torso
            Math.toDegrees(atan2(abs(noseAboveNeck - 0.45), 0.45))
        } else 0.0

        fun ang(a: Int, b: Int, c: Int) = JointAngleExtractor.angle3(g.point(a), g.point(b), g.point(c))
        val leftKneeDeg = ang(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE)
        val rightKneeDeg = ang(PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE)
        val leftArmElev = ang(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_ELBOW)
        val rightArmElev = ang(PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_ELBOW)

        return OrientationAgnosticFeatures(
            phiDeg = phiDeg,
            torsoAxis = normalizedTorso,
            pelvisFrame = pelvisFrame,
            floorY = smoothedFloorY,
            hipHeightAboveFloorTorso = hipHeightAboveFloorTorso,
            ankleSeparationTorso = ankleSep,
            stanceWidthTorso = stanceWidth,
            stanceAxisAngleDeg = stanceAxisAngleDeg,
            nearSide = nearSide,
            twistDeg = twistDeg,
            headTiltDeg = headTiltDeg,
            headYawDeg = headYawDeg,
            headPitchDeg = headPitchDeg,
            leftKneeDeg = leftKneeDeg,
            rightKneeDeg = rightKneeDeg,
            leftArmElevDeg = leftArmElev,
            rightArmElevDeg = rightArmElev
        )
    }

    private fun hypot(a: Double, b: Double) = sqrt(a * a + b * b)
}
