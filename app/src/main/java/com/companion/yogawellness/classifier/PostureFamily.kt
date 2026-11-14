package com.companion.yogawellness.classifier

import com.companion.yogawellness.scoring.Asana

/**
 * Three macroscopic posture families (§4.1):
 *  - STANDING: upright or squatting on feet (Tadasana, Tree, Warrior I/II, Triangle, Extended Side Angle, Chair, Half Moon, Malasana)
 *  - SEATED: pelvis on or near the floor with upright or twisting torso (Butterfly, Seated Twist, Neck Releases)
 *  - FLOOR: horizontal, inverted, or kneeling postures (Downward Dog, Child's Pose, Legs-Up-The-Wall, Savasana)
 */
enum class PostureFamily(val label: String) {
    STANDING("Standing"),
    SEATED("Seated"),
    FLOOR("Floor");

    fun poses(): List<Asana> = when (this) {
        STANDING -> listOf(
            Asana.TADASANA,
            Asana.VRIKSHASANA,
            Asana.VIRABHADRASANA_I,
            Asana.VIRABHADRASANA_II,
            Asana.TRIKONASANA,
            Asana.UTTHITA_PARSVAKONASANA,
            Asana.UTKATASANA,
            Asana.ARDHA_CHANDRASANA,
            Asana.MALASANA
        )
        SEATED -> listOf(
            Asana.BADDHA_KONASANA,
            Asana.SEATED_TWIST,
            Asana.NECK_RELEASES
        )
        FLOOR -> listOf(
            Asana.ADHO_MUKHA_SVANASANA,
            Asana.BALASANA,
            Asana.VIPARITA_KARANI,
            Asana.SAVASANA
        )
    }
}

/**
 * Gate classifier that segments postures into macroscopic families (§4.1).
 */
object PostureFamilyClassifier {

    fun classifyFamily(frame: com.companion.yogawellness.pose.PoseFrame): PostureFamily {
        if (!frame.hasPose) return PostureFamily.STANDING
        val g = com.companion.yogawellness.pose.PoseGeometry(frame, 0.25)
        val torso = g.torsoLengthPx
        if (torso < 1.0) return PostureFamily.STANDING

        val phi = com.companion.yogawellness.pose.JointAngleExtractor.segmentVsVertical(g.midHip, g.midShoulder)
        val lHip = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.LEFT_HIP)
        val rHip = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.RIGHT_HIP)
        val lAnkle = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.LEFT_ANKLE)
        val rAnkle = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.RIGHT_ANKLE)
        val lSh = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.LEFT_SHOULDER)
        val rSh = g.point(com.companion.yogawellness.pose.PoseLandmarkIndex.RIGHT_SHOULDER)

        val midHipY = (lHip.y + rHip.y) / 2.0
        val midAnkleY = (lAnkle.y + rAnkle.y) / 2.0
        val midShY = (lSh.y + rSh.y) / 2.0

        // In Downward Dog, hips are physically higher than shoulders (lower screen Y)
        if (midHipY < midShY - 0.15 * torso) return PostureFamily.FLOOR

        // Horizontal body (Savasana, Legs-up-the-wall, Child's pose)
        if (phi > 60.0) return PostureFamily.FLOOR

        // Seated: ankles and hips at similar height (hip to ankle vertical distance < 0.65 torso)
        val hipAnkleVertDist = (midAnkleY - midHipY) / torso
        if (hipAnkleVertDist < 0.65) {
            val anklePelvisDist = minOf(
                kotlin.math.hypot(lAnkle.x - lHip.x, lAnkle.y - lHip.y),
                kotlin.math.hypot(rAnkle.x - rHip.x, rAnkle.y - rHip.y)
            ) / torso
            if (anklePelvisDist < 0.85 && phi < 35.0) {
                return PostureFamily.SEATED
            }
        }

        return PostureFamily.STANDING
    }
}
