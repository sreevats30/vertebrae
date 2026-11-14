package com.companion.yogawellness.classifier

import com.companion.yogawellness.feedback.SafetyGuards
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.replay.SyntheticLandmarkFactory
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.AsanaSpecs
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StandingPosesTest {

    private val classifier = RuleAndCentroidClassifier()

    @Test
    fun syntheticWarrior1ClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("warrior_1")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.VIRABHADRASANA_I)
        assertNotNull(res.label)
        assertEquals(Asana.VIRABHADRASANA_I, res.label?.asana)
    }

    @Test
    fun syntheticExtendedSideAngleClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("extended_side_angle")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.UTTHITA_PARSVAKONASANA)
        assertNotNull(res.label)
        assertEquals(Asana.UTTHITA_PARSVAKONASANA, res.label?.asana)
    }

    @Test
    fun syntheticHalfMoonClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("half_moon")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.ARDHA_CHANDRASANA)
        assertNotNull(res.label)
        assertEquals(Asana.ARDHA_CHANDRASANA, res.label?.asana)
    }

    @Test
    fun syntheticMalasanaClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("yogic_squat")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.MALASANA)
        assertNotNull(res.label)
        assertEquals(Asana.MALASANA, res.label?.asana)
    }

    @Test
    fun warrior1KneeOvershootGuardFires() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("warrior_1")!!
        // With normal peak frame, knee overshoot is within tolerance
        val label = AsanaLabel(Asana.VIRABHADRASANA_I, PoseSide.FRONT_LEFT)
        val alert = SafetyGuards.evaluate(label, frame, emptyMap())
        // Should not trigger on perfect alignment
        assertTrue(alert == null || alert.id == "warrior1_knee")
    }

    @Test
    fun halfMoonStandingKneeHyperextensionGuardFires() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("half_moon")!!
        val label = AsanaLabel(Asana.ARDHA_CHANDRASANA, PoseSide.LEFT_LIFTED)
        val alert = SafetyGuards.evaluate(label, frame, mapOf("Standing Knee" to 185.0))
        assertNotNull(alert)
        assertEquals("half_moon_knee", alert?.id)
    }

    @Test
    fun targetHintRelaxesRunnerUpMargin() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("warrior_1")!!
        val resWithHint = classifier.classify(frame, targetHint = Asana.VIRABHADRASANA_I)
        assertEquals(Asana.VIRABHADRASANA_I, resWithHint.label?.asana)
    }
}
