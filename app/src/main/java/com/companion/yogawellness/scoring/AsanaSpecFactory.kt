package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.PostureFamily

/**
 * Single source of truth for constructing [PoseSpec] instances.
 * Pairs AsanaSpecs definitions with PostureFamily, camera views, and setup guidance (§5.2).
 */
object AsanaSpecFactory {

    fun createSpec(asana: Asana): PoseSpec {
        return when (asana) {
            Asana.TADASANA -> PoseSpec(
                asana = asana,
                catalogId = "mountain",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = false,
                joints = emptyList(),
                setupTip = "Stand tall facing the camera with your whole body visible",
                setupDiagramKey = "tadasana_front"
            )

            Asana.VRIKSHASANA -> PoseSpec(
                asana = asana,
                catalogId = "tree_pose",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.VRIKSHASANA,
                setupTip = "Step back 2-3 meters so your feet and hands overhead are visible",
                setupDiagramKey = "tree_front"
            )

            Asana.VIRABHADRASANA_II -> PoseSpec(
                asana = asana,
                catalogId = "warrior_2",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.VIRABHADRASANA_II,
                setupTip = "Step into a wide stance facing the camera with arms extended wide",
                setupDiagramKey = "warrior2_front"
            )

            Asana.TRIKONASANA -> PoseSpec(
                asana = asana,
                catalogId = "triangle_pose",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.TRIKONASANA,
                setupTip = "Stand wide with both legs straight and reach down to your shin",
                setupDiagramKey = "triangle_front"
            )

            Asana.UTKATASANA -> PoseSpec(
                asana = asana,
                catalogId = "chair_pose",
                family = PostureFamily.STANDING,
                view = CameraView.ANGLE_45,
                hasSides = false,
                joints = AsanaSpecs.UTKATASANA,
                setupTip = "Turn at a 45° angle to the phone so your knees and spine are visible",
                setupDiagramKey = "chair_side"
            )

            Asana.VIRABHADRASANA_I -> PoseSpec(
                asana = asana,
                catalogId = "warrior_1",
                family = PostureFamily.STANDING,
                view = CameraView.ANGLE_45,
                hasSides = true,
                joints = AsanaSpecs.VIRABHADRASANA_I,
                setupTip = "Turn 45° to the phone, bend front knee, and reach arms overhead",
                setupDiagramKey = "warrior1_angle"
            )

            Asana.UTTHITA_PARSVAKONASANA -> PoseSpec(
                asana = asana,
                catalogId = "extended_side_angle",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.UTTHITA_PARSVAKONASANA,
                setupTip = "Wide stance facing the phone, bend front knee, reach top arm long",
                setupDiagramKey = "side_angle_front"
            )

            Asana.ARDHA_CHANDRASANA -> PoseSpec(
                asana = asana,
                catalogId = "half_moon",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.ARDHA_CHANDRASANA,
                setupTip = "Balance on one leg, float the other parallel to the floor",
                setupDiagramKey = "half_moon_front"
            )

            Asana.MALASANA -> PoseSpec(
                asana = asana,
                catalogId = "yogic_squat",
                family = PostureFamily.STANDING,
                view = CameraView.FRONT,
                hasSides = false,
                joints = AsanaSpecs.MALASANA,
                setupTip = "Feet slightly wider than hips, lower into a deep yogic squat",
                setupDiagramKey = "malasana_front"
            )

            Asana.BADDHA_KONASANA -> PoseSpec(
                asana = asana,
                catalogId = "bound_angle",
                family = PostureFamily.SEATED,
                view = CameraView.FRONT,
                hasSides = false,
                joints = AsanaSpecs.BADDHA_KONASANA,
                setupTip = "Sit comfortably on the mat facing the phone with soles of feet together",
                setupDiagramKey = "butterfly_front"
            )

            Asana.SEATED_TWIST -> PoseSpec(
                asana = asana,
                catalogId = "seated_twist",
                family = PostureFamily.SEATED,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.SEATED_TWIST,
                setupTip = "Sit tall, place the phone at seated eye level, rotate your upper spine",
                setupDiagramKey = "seated_twist_front"
            )

            Asana.NECK_RELEASES -> PoseSpec(
                asana = asana,
                catalogId = "neck_releases",
                family = PostureFamily.SEATED,
                view = CameraView.FRONT,
                hasSides = true,
                joints = AsanaSpecs.NECK_RELEASES,
                setupTip = "Sit upright at your desk or mat with shoulders relaxed and head in frame",
                setupDiagramKey = "neck_releases_front"
            )

            Asana.ADHO_MUKHA_SVANASANA -> PoseSpec(
                asana = asana,
                catalogId = "downward_dog",
                family = PostureFamily.FLOOR,
                view = CameraView.SIDE,
                hasSides = false,
                joints = AsanaSpecs.ADHO_MUKHA_SVANASANA,
                setupTip = "Place phone on the floor to your side, 2m away in landscape",
                setupDiagramKey = "downward_dog_side"
            )

            Asana.BALASANA -> PoseSpec(
                asana = asana,
                catalogId = "childs_pose",
                family = PostureFamily.FLOOR,
                view = CameraView.SIDE,
                hasSides = false,
                joints = AsanaSpecs.BALASANA,
                setupTip = "Place phone on the floor to your side, fold hips toward heels",
                setupDiagramKey = "childs_side"
            )

            Asana.VIPARITA_KARANI -> PoseSpec(
                asana = asana,
                catalogId = "legs_up_the_wall",
                family = PostureFamily.FLOOR,
                view = CameraView.SIDE,
                hasSides = false,
                joints = AsanaSpecs.VIPARITA_KARANI,
                setupTip = "Place phone side-on, lie on your back with legs extended up",
                setupDiagramKey = "legs_wall_side"
            )

            Asana.SAVASANA -> PoseSpec(
                asana = asana,
                catalogId = "corpse_relaxation",
                family = PostureFamily.FLOOR,
                view = CameraView.SIDE,
                hasSides = false,
                joints = AsanaSpecs.SAVASANA,
                holdPolicy = HoldPolicy.STILLNESS_HOLD,
                setupTip = "Place phone side-on, lie flat on your back and release all tension",
                setupDiagramKey = "savasana_side"
            )
        }
    }
}
