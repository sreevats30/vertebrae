package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.PostureFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseRegistryTest {

    @Test
    fun allSixteenPosesRegisteredWithCorrectFamilies() {
        val specs = PoseRegistry.allSpecs
        assertEquals(16, specs.size)

        val standing = specs.filter { it.family == PostureFamily.STANDING }
        val seated = specs.filter { it.family == PostureFamily.SEATED }
        val floor = specs.filter { it.family == PostureFamily.FLOOR }

        assertEquals(9, standing.size)
        assertEquals(3, seated.size)
        assertEquals(4, floor.size)

        // Check catalog ID lookup
        assertNotNull(PoseRegistry.entryForCatalogId("warrior_1"))
        assertNotNull(PoseRegistry.entryForCatalogId("extended_side_angle"))
        assertNotNull(PoseRegistry.entryForCatalogId("half_moon"))
        assertNotNull(PoseRegistry.entryForCatalogId("yogic_squat"))
        assertNotNull(PoseRegistry.entryForCatalogId("bound_angle"))
        assertNotNull(PoseRegistry.entryForCatalogId("seated_twist"))
        assertNotNull(PoseRegistry.entryForCatalogId("neck_releases"))
        assertNotNull(PoseRegistry.entryForCatalogId("downward_dog"))
        assertNotNull(PoseRegistry.entryForCatalogId("childs_pose"))
        assertNotNull(PoseRegistry.entryForCatalogId("legs_up_the_wall"))
        assertNotNull(PoseRegistry.entryForCatalogId("corpse_relaxation"))

        assertTrue(PoseRegistry.isLiveScored("warrior_1"))
        assertTrue(PoseRegistry.isLiveScored("downward_dog"))
    }
}
