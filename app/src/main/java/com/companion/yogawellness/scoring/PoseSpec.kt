package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.PostureFamily

enum class HoldPolicy {
    ALIGNMENT_HOLD,
    STILLNESS_HOLD
}

enum class CameraView(val label: String) {
    FRONT("Front"),
    ANGLE_45("45°"),
    SIDE("Side")
}

data class PoseSpec(
    val asana: Asana,
    val catalogId: String,
    val family: PostureFamily,
    val view: CameraView,
    val hasSides: Boolean,
    val joints: List<JointSpec>,
    val holdPolicy: HoldPolicy = HoldPolicy.ALIGNMENT_HOLD,
    val setupTip: String,
    val setupDiagramKey: String,
    val maxHoldSec: Int? = asana.maxHoldSec,
    val zWeight: Float = asana.zWeight
)
