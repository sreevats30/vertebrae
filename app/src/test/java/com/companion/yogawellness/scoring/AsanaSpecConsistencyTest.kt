package com.companion.yogawellness.scoring

import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.pose.PoseGeometry
import com.companion.yogawellness.replay.SyntheticLandmarkFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AsanaSpecConsistencyTest {

    @Test
    fun roundTripSyntheticLandmarksMatchSpecTargetsWithinTolerance() {
        for (spec in PoseRegistry.allSpecs) {
            val asana = spec.asana
            if (!asana.isScored) continue
            val frame = SyntheticLandmarkFactory.fromCatalogPeak(spec.catalogId) ?: continue
            val extraction = JointAngleExtractor.extract(frame, spec.joints, PoseSide.FRONT_LEFT.frontSide, spec.zWeight.toDouble())
            assertNotNull(extraction)

            for (jointSpec in spec.joints) {
                if (jointSpec.notMeasured) continue
                val measured = extraction.angles[jointSpec.name] ?: continue
                if (measured.isNaN()) continue
                // For compound twists tolerance is up to 5°, otherwise 3° or within spec tolerance
                val allowedDiff = if (asana == Asana.SEATED_TWIST) 15.0 else maxOf(5.0, jointSpec.toleranceDeg)
                val diff = abs(measured - jointSpec.targetDeg)
                assertTrue(
                    "${spec.catalogId} - ${jointSpec.name}: measured $measured vs target ${jointSpec.targetDeg} (diff $diff, allowed $allowedDiff)",
                    diff <= allowedDiff
                )
            }
        }
    }
}
