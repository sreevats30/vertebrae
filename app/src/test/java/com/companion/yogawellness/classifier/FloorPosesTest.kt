package com.companion.yogawellness.classifier

import com.companion.yogawellness.feedback.SafetyGuards
import com.companion.yogawellness.replay.SyntheticLandmarkFactory
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorPosesTest {

    private val classifier = RuleAndCentroidClassifier()

    @Test
    fun syntheticDownwardDogClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("downward_dog")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.ADHO_MUKHA_SVANASANA)
        assertNotNull(res.label)
        assertEquals(Asana.ADHO_MUKHA_SVANASANA, res.label?.asana)
    }

    @Test
    fun syntheticChildsPoseClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("childs_pose")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.BALASANA)
        assertNotNull(res.label)
        assertEquals(Asana.BALASANA, res.label?.asana)
    }

    @Test
    fun syntheticLegsUpTheWallClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("legs_up_the_wall") ?: SyntheticLandmarkFactory.fromCatalogPeak("corpse_relaxation")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.VIPARITA_KARANI)
        assertNotNull(res.label)
        assertEquals(Asana.VIPARITA_KARANI, res.label?.asana)
    }

    @Test
    fun syntheticSavasanaClassifiesCorrectly() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("corpse_relaxation")
        assertNotNull(frame)
        val res = classifier.classify(frame!!, targetHint = Asana.SAVASANA)
        assertNotNull(res.label)
        assertEquals(Asana.SAVASANA, res.label?.asana)
    }

    @Test
    fun downwardDogHyperextensionGuardFires() {
        val frame = SyntheticLandmarkFactory.fromCatalogPeak("downward_dog")!!
        val label = AsanaLabel(Asana.ADHO_MUKHA_SVANASANA, PoseSide.SYMMETRIC)
        val alert = SafetyGuards.evaluate(label, frame, mapOf("Arms Straight" to 186.0))
        assertNotNull(alert)
        assertEquals("downward_dog_hyperextension", alert?.id)
    }
}
