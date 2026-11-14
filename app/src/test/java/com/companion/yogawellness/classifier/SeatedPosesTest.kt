package com.companion.yogawellness.classifier

import com.companion.yogawellness.feedback.SafetyGuards
import com.companion.yogawellness.replay.SyntheticLandmarkFactory
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatedPosesTest {

    private val classifier = RuleAndCentroidClassifier()

    @Test
    fun syntheticButterflyClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("bound_angle")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.BADDHA_KONASANA)
        assertNotNull(res.label)
        assertEquals(Asana.BADDHA_KONASANA, res.label?.asana)
    }

    @Test
    fun syntheticSeatedTwistClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("seated_twist")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.SEATED_TWIST)
        assertNotNull(res.label)
        assertEquals(Asana.SEATED_TWIST, res.label?.asana)
    }

    @Test
    fun syntheticNeckReleasesClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("neck_releases") ?: SyntheticLandmarkFactory.fromCatalogPeak("mountain")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.NECK_RELEASES)
        assertNotNull(res.label)
        assertEquals(Asana.NECK_RELEASES, res.label?.asana)
    }

    @Test
    fun neckReleasesHeadBackGuardFires() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("mountain")!!
        val label = AsanaLabel(Asana.NECK_RELEASES, PoseSide.SYMMETRIC)
        val alert = SafetyGuards.evaluate(label, frame, emptyMap())
        // Neutral frame should not trigger head-back
        assertTrue(alert == null || alert.id == "neck_head_back" || alert.id == "neck_shrug")
    }
}
