package com.companion.yogawellness.codex

import com.companion.yogawellness.codex.Suitability.CAUTION
import com.companion.yogawellness.codex.Suitability.OK
import com.companion.yogawellness.codex.Suitability.RECOMMENDED
import com.companion.yogawellness.codex.Suitability.USE_EASIER_VARIANT
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.personalisation.AgeProfile
import com.companion.yogawellness.scoring.Asana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseSuitabilityTest {

    private val items = AsanaCatalog32.items
    private fun advice(id: String, band: AgeBand) = PoseSuitability.adviceFor(AsanaCatalog32.byId(id)!!, band)
    private val younger = listOf(AgeBand.UNDER_18, AgeBand.AGE_18_30, AgeBand.AGE_31_50)

    @Test
    fun everyPoseHasAdviceForEveryBand() {
        assertEquals(32, items.size)
        var cells = 0
        for (item in items) for (band in AgeBand.entries) {
            assertNotNull("${item.id} / $band", PoseSuitability.adviceFor(item, band))
            cells++
        }
        assertEquals(32 * 5, cells)
    }

    /** Anything that asks for care says why, in one plain sentence, and has an easier option to point to. */
    @Test
    fun everyCareNoteIsOneGentleSentenceWithAnEasierOptionBehindIt() {
        val commanding = listOf("must", "never", "do not", "don't", "forbidden", "not allowed", "avoid ")
        for (item in items) for (band in AgeBand.entries) {
            val a = PoseSuitability.adviceFor(item, band)
            val where = "${item.id} / $band"
            if (!a.suitability.isSuggested) {
                assertNotNull("$where needs a note", a.note)
                assertTrue("$where must have an easier option to point to", item.modifications.isNotEmpty())
            }
            a.note?.let { note ->
                assertTrue("$where: ends with a full stop", note.endsWith("."))
                assertEquals("$where: one sentence", 1, note.trimEnd('.').split(". ").size)
                assertTrue("$where: short", note.length <= 110)
                assertFalse("$where is phrased as an order: $note", commanding.any { note.lowercase().contains(it) })
            }
        }
    }

    @Test
    fun underFiftyOneNothingAsksForCare() {
        for (item in items) for (band in younger) {
            val a = PoseSuitability.adviceFor(item, band)
            assertTrue("${item.id} / $band", a.suitability.isSuggested)
            assertNull("${item.id} / $band", a.note)
        }
    }

    @Test
    fun theThreeYoungerBandsShareOneColumn() {
        for (item in items) {
            assertEquals(item.id, 1, younger.map { PoseSuitability.adviceFor(item, it) }.toSet().size)
        }
    }

    // ---- The four live-scored poses ---------------------------------------------------------

    @Test
    fun treePose() {
        younger.forEach { assertEquals(OK, advice("tree_pose", it).suitability) }
        advice("tree_pose", AgeBand.AGE_51_64).let { assertEquals(OK, it.suitability); assertTrue(it.note!!.contains("wall or chair")) }
        advice("tree_pose", AgeBand.AGE_65_PLUS).let { assertEquals(CAUTION, it.suitability); assertTrue(it.note!!.contains("wall or chair")) }
    }

    @Test
    fun warriorTwo() {
        (younger + AgeBand.AGE_51_64).forEach { assertEquals(PoseAdvice(OK), advice("warrior_2", it)) }
        advice("warrior_2", AgeBand.AGE_65_PLUS).let {
            assertEquals(USE_EASIER_VARIANT, it.suitability)
            assertTrue(it.note!!.contains("shallower"))
        }
    }

    @Test
    fun triangle() {
        younger.forEach { assertEquals(OK, advice("triangle_pose", it).suitability) }
        for (band in listOf(AgeBand.AGE_51_64, AgeBand.AGE_65_PLUS)) {
            val a = advice("triangle_pose", band)
            assertEquals(CAUTION, a.suitability)
            assertTrue(a.note!!.contains("shin or a block"))
        }
    }

    /** Chair Pose's note names the cap actually enforced for the band (Step 3). */
    @Test
    fun chairPoseNoteCarriesTheBandsCap() {
        younger.forEach { assertEquals(PoseAdvice(OK), advice("chair_pose", it)) }
        for ((band, cap) in listOf(AgeBand.AGE_51_64 to 20, AgeBand.AGE_65_PLUS to 15)) {
            val a = advice("chair_pose", band)
            assertEquals(CAUTION, a.suitability)
            assertEquals(cap, AgeProfile.utkatasanaCapFor(band))
            assertTrue(a.note, a.note!!.contains("$cap s"))
            assertTrue(a.note!!.contains("knees or lower back"))
        }
    }

    @Test
    fun adviceByLiveAsanaMatchesItsLearnPage() {
        for (band in AgeBand.entries) {
            assertEquals(advice("tree_pose", band), PoseSuitability.adviceFor(Asana.VRIKSHASANA, band))
            assertEquals(advice("warrior_2", band), PoseSuitability.adviceFor(Asana.VIRABHADRASANA_II, band))
            assertEquals(advice("triangle_pose", band), PoseSuitability.adviceFor(Asana.TRIKONASANA, band))
            assertEquals(advice("chair_pose", band), PoseSuitability.adviceFor(Asana.UTKATASANA, band))
        }
        assertNull("Tadasana is the calibration pose, with no live Learn page", PoseSuitability.adviceFor(Asana.TADASANA, AgeBand.AGE_65_PLUS))
    }

    // ---- Category defaults ------------------------------------------------------------------

    private fun suitabilities(id: String) =
        listOf(AgeBand.AGE_18_30, AgeBand.AGE_51_64, AgeBand.AGE_65_PLUS).map { advice(id, it).suitability }

    @Test
    fun categoryDefaults() {
        assertEquals(listOf(RECOMMENDED, RECOMMENDED, OK), suitabilities("cat_cow"))                 // Desk
        assertEquals(listOf(RECOMMENDED, OK, USE_EASIER_VARIANT), suitabilities("downward_dog"))      // Morning
        assertEquals(listOf(OK, OK, USE_EASIER_VARIANT), suitabilities("warrior_1"))                  // Standing
        assertEquals(listOf(OK, CAUTION, CAUTION), suitabilities("half_moon"))                        // Balance
        assertEquals(listOf(OK, CAUTION, CAUTION), suitabilities("yogic_squat"))                      // Hips
        assertEquals(listOf(RECOMMENDED, RECOMMENDED, RECOMMENDED), suitabilities("childs_pose"))     // Evening
        assertTrue(advice("downward_dog", AgeBand.AGE_51_64).note!!.contains("wrists"))
        assertTrue(advice("yogic_squat", AgeBand.AGE_65_PLUS).note!!.contains("knees"))
    }

    @Test
    fun wholeEveningSetIsRecommendedForEveryone() {
        items.filter { it.category == AsanaCategory.EVENING }.forEach { item ->
            AgeBand.entries.forEach { assertEquals(item.id, PoseAdvice(RECOMMENDED), PoseSuitability.adviceFor(item, it)) }
        }
    }

    @Test
    fun overridesWhereTheCategoryDoesNotFit() {
        // Eagle is filed under Desk but is a single-leg balance.
        assertEquals(listOf(OK, CAUTION, CAUTION), suitabilities("eagle_pose"))
        // Mountain is standing still, and every session calibrates in it.
        assertEquals(listOf(RECOMMENDED, OK, OK), suitabilities("mountain"))
        AgeBand.entries.forEach { assertNull(advice("mountain", it).note) }
        // Side Plank loads the wrist and shoulder.
        assertTrue(advice("side_plank", AgeBand.AGE_65_PLUS).note!!.contains("forearm"))
    }

    // ---- "For you" --------------------------------------------------------------------------

    @Test
    fun forYouNeverHidesAPose() {
        for (band in AgeBand.entries) {
            val listed = forYouSections(items, band).flatMap { it.items }
            assertEquals("$band", 32, listed.size)
            assertEquals("$band", items.map { it.id }.toSet(), listed.map { it.id }.toSet())
        }
    }

    @Test
    fun forYouPutsSuggestedPosesFirst() {
        for (band in AgeBand.entries) {
            val order = forYouSections(items, band).flatMap { it.items }.map { PoseSuitability.adviceFor(it, band).suitability }
            val firstCare = order.indexOfFirst { !it.isSuggested }
            if (firstCare >= 0) assertTrue("$band", order.drop(firstCare).none { it.isSuggested })
            val firstOk = order.indexOf(OK)
            if (firstOk >= 0) assertTrue("$band: recommended before OK", order.drop(firstOk).none { it == RECOMMENDED })
        }
    }

    @Test
    fun forYouSectionsPerBand() {
        fun keys(b: AgeBand) = forYouSections(items, b).map { it.key }
        younger.forEach { assertEquals(listOf("recommended", "suitable"), keys(it)) } // nothing needs care
        assertEquals(listOf("recommended", "suitable", "care"), keys(AgeBand.AGE_51_64))
        assertEquals(listOf("recommended", "suitable", "care"), keys(AgeBand.AGE_65_PLUS))
    }

    /** Inside a group the band's preferred categories lead: Desk first for 31–50, Evening first for 65+. */
    @Test
    fun forYouLeadsWithTheBandsPreferredCategories() {
        assertEquals(AsanaCategory.DESK, forYouSections(items, AgeBand.AGE_31_50).first().items.first().category)
        assertEquals(AsanaCategory.EVENING, forYouSections(items, AgeBand.AGE_65_PLUS).first().items.first().category)
        assertEquals(AsanaCategory.MORNING, forYouSections(items, AgeBand.UNDER_18).first().items.first().category)
    }

    @Test
    fun tagsOnlyWhereThereIsSomethingToSay() {
        assertEquals("Good fit", RECOMMENDED.tag)
        assertNull(OK.tag)
        assertEquals("Go gently", CAUTION.tag)
        assertEquals("Gentle option", USE_EASIER_VARIANT.tag)
    }
}
