package com.companion.yogawellness.replay

import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.ForwardKinematics
import com.companion.yogawellness.codex.PoseAngles
import com.companion.yogawellness.codex.Skeleton
import com.companion.yogawellness.codex.SkeletonJoint
import com.companion.yogawellness.codex.V3
import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseLandmarkIndex as L
import kotlin.random.Random

/**
 * Generates 33 MediaPipe-style [PoseFrame]s from codex forward kinematics [PoseAngles] or catalog poses (§9.2).
 */
object SyntheticLandmarkFactory {

    const val DEFAULT_WIDTH = 480
    const val DEFAULT_HEIGHT = 640

    fun fromCatalogPeak(
        catalogId: String,
        timestampMs: Long = 1000L,
        yawDeg: Float? = null,
        noiseFraction: Double = 0.0,
        random: Random = Random(42)
    ): PoseFrame? {
        val item = AsanaCatalog32.byId(catalogId) ?: return null
        val yaw = yawDeg ?: item.defaultView.yawDeg
        return fromPoseAngles(item.peak, timestampMs, yaw, noiseFraction, random)
    }

    fun fromPoseAngles(
        angles: PoseAngles,
        timestampMs: Long = 1000L,
        yawDeg: Float = 0f,
        noiseFraction: Double = 0.0,
        random: Random = Random(42),
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        torsoPx: Double = 135.0,
        hipCenterScreenX: Double = 240.0,
        hipCenterScreenY: Double = 330.0
    ): PoseFrame {
        val skel = ForwardKinematics.solve(angles, yawDeg)
        val lms = Array(L.LANDMARK_COUNT) { Landmark(0f, 0f, 0f, 0.95f) }

        fun toScreen(v: V3): Triple<Float, Float, Float> {
            val px = hipCenterScreenX + v.x * torsoPx
            val py = hipCenterScreenY - v.y * torsoPx // V3.UP is +Y, screen is +Y down
            val pz = (v.z * torsoPx).toFloat()
            val noiseX = if (noiseFraction > 0.0) random.nextDouble(-noiseFraction, noiseFraction) * torsoPx else 0.0
            val noiseY = if (noiseFraction > 0.0) random.nextDouble(-noiseFraction, noiseFraction) * torsoPx else 0.0
            val nx = ((px + noiseX) / width).toFloat().coerceIn(0.01f, 0.99f)
            val ny = ((py + noiseY) / height).toFloat().coerceIn(0.01f, 0.99f)
            return Triple(nx, ny, pz)
        }

        fun put(index: Int, v: V3, vis: Float = 0.95f) {
            val (x, y, z) = toScreen(v)
            lms[index] = Landmark(x, y, z, vis)
        }

        val nosePos = skel[SkeletonJoint.HEAD]
        put(L.NOSE, nosePos)
        val neckPos = skel[SkeletonJoint.NECK]

        // Face approximations
        val eyeOffset = V3(0.06, 0.04, 0.08)
        put(L.LEFT_EYE, nosePos + eyeOffset)
        put(L.RIGHT_EYE, nosePos + V3(-0.06, 0.04, 0.08))
        put(L.LEFT_EAR, nosePos + V3(0.15, 0.02, -0.05))
        put(L.RIGHT_EAR, nosePos + V3(-0.15, 0.02, -0.05))

        // Upper body
        val lSh = skel[SkeletonJoint.L_SHOULDER]
        val rSh = skel[SkeletonJoint.R_SHOULDER]
        val lEl = skel[SkeletonJoint.L_ELBOW]
        val rEl = skel[SkeletonJoint.R_ELBOW]
        val lWr = skel[SkeletonJoint.L_WRIST]
        val rWr = skel[SkeletonJoint.R_WRIST]

        put(L.LEFT_SHOULDER, lSh)
        put(L.RIGHT_SHOULDER, rSh)
        put(L.LEFT_ELBOW, lEl)
        put(L.RIGHT_ELBOW, rEl)
        put(L.LEFT_WRIST, lWr)
        put(L.RIGHT_WRIST, rWr)

        // Hands
        put(L.LEFT_PINKY, lWr + V3(0.02, -0.04, 0.0))
        put(L.RIGHT_PINKY, rWr + V3(-0.02, -0.04, 0.0))
        put(L.LEFT_INDEX, lWr + V3(0.04, -0.05, 0.0))
        put(L.RIGHT_INDEX, rWr + V3(-0.04, -0.05, 0.0))
        put(L.LEFT_THUMB, lWr + V3(0.02, -0.02, 0.02))
        put(L.RIGHT_THUMB, rWr + V3(-0.02, -0.02, 0.02))

        // Lower body
        val lHip = skel[SkeletonJoint.L_HIP]
        val rHip = skel[SkeletonJoint.R_HIP]
        val lKn = skel[SkeletonJoint.L_KNEE]
        val rKn = skel[SkeletonJoint.R_KNEE]
        val lAnk = skel[SkeletonJoint.L_ANKLE]
        val rAnk = skel[SkeletonJoint.R_ANKLE]

        put(L.LEFT_HIP, lHip)
        put(L.RIGHT_HIP, rHip)
        put(L.LEFT_KNEE, lKn)
        put(L.RIGHT_KNEE, rKn)
        put(L.LEFT_ANKLE, lAnk)
        put(L.RIGHT_ANKLE, rAnk)

        // Feet
        put(L.LEFT_HEEL, lAnk + V3(0.0, -0.03, -0.05))
        put(L.RIGHT_HEEL, rAnk + V3(0.0, -0.03, -0.05))
        val lToe = skel[SkeletonJoint.L_TOE]
        val rToe = skel[SkeletonJoint.R_TOE]
        put(L.LEFT_FOOT_INDEX, lToe)
        put(L.RIGHT_FOOT_INDEX, rToe)

        return PoseFrame(timestampMs, width, height, lms.toList(), 120.0)
    }
}
