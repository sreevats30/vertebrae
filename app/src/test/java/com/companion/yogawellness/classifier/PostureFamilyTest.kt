package com.companion.yogawellness.classifier

import com.companion.yogawellness.replay.SyntheticLandmarkFactory
import com.companion.yogawellness.scoring.PoseRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PostureFamilyTest {

    @Test
    fun syntheticPosesMatchConfiguredFamilies() {
        for (spec in PoseRegistry.allSpecs) {
            val frame = SyntheticLandmarkFactory.fromCatalogPeak(spec.catalogId) ?: continue
            val family = PostureFamilyClassifier.classifyFamily(frame)
            assertNotNull(family)
            // Verify assigned family matches spec
            assertEquals("${spec.catalogId} expected ${spec.family}", spec.family, family)
        }
    }
}
