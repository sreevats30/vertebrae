package com.companion.yogawellness.personalisation

import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.pose.PipelineEvent
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.HoldPhase
import com.companion.yogawellness.scoring.HoldStateMachine
import com.companion.yogawellness.scoring.HoldStatus
import com.companion.yogawellness.scoring.ToleranceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeProfileTest {

    // ---- The mapping ------------------------------------------------------------------------

    @Test
    fun everyBandHasAProfileForItself() {
        AgeBand.entries.forEach { assertEquals(it, AgeProfile.forBand(it).band) }
    }

    @Test
    fun utkatasanaCapPerBand() {
        assertEquals(
            mapOf(
                AgeBand.UNDER_18 to 30, AgeBand.AGE_18_30 to 30, AgeBand.AGE_31_50 to 30,
                AgeBand.AGE_51_64 to 20, AgeBand.AGE_65_PLUS to 15
            ),
            AgeBand.entries.associateWith { AgeProfile.forBand(it).utkatasanaCapSeconds }
        )
    }

    /** Age may only shorten the hold: no band is ever given longer than the pose's own cap. */
    @Test
    fun capIsNeverAboveTheStandardThirtySeconds() {
        assertEquals(30, AgeProfile.DEFAULT_UTKATASANA_CAP_SEC)
        assertEquals(Asana.UTKATASANA.maxHoldSec, AgeProfile.DEFAULT_UTKATASANA_CAP_SEC)
        AgeBand.entries.forEach { band ->
            assertTrue("$band", AgeProfile.utkatasanaCapFor(band) in 1..30)
            assertEquals(AgeProfile.forBand(band).utkatasanaCapSeconds, AgeProfile.utkatasanaCapFor(band))
        }
    }

    @Test
    fun noBandYetKeepsTheStandardCap() {
        assertEquals(30, AgeProfile.utkatasanaCapFor(null))
    }

    @Test
    fun suggestedSessionLengthPerBand() {
        assertEquals(
            mapOf(
                AgeBand.UNDER_18 to 10..20, AgeBand.AGE_18_30 to 15..30, AgeBand.AGE_31_50 to 15..30,
                AgeBand.AGE_51_64 to 15..25, AgeBand.AGE_65_PLUS to 10..20
            ),
            AgeBand.entries.associateWith { AgeProfile.forBand(it).suggestedSessionMinutes }
        )
    }

    @Test
    fun questionnairesAreOffOnlyForUnder18() {
        AgeBand.entries.forEach { band ->
            assertEquals("$band", band != AgeBand.UNDER_18, AgeProfile.forBand(band).wellnessQuestionnairesEnabled)
        }
    }

    @Test
    fun preferredCategoriesPerBand() {
        fun cats(b: AgeBand) = AgeProfile.forBand(b).preferredCategories
        assertEquals(listOf(AsanaCategory.MORNING, AsanaCategory.STANDING, AsanaCategory.BALANCE), cats(AgeBand.UNDER_18))
        assertEquals(AsanaCategory.entries.toList(), cats(AgeBand.AGE_18_30))
        assertEquals(listOf(AsanaCategory.DESK, AsanaCategory.MORNING, AsanaCategory.EVENING), cats(AgeBand.AGE_31_50))
        assertEquals(listOf(AsanaCategory.DESK, AsanaCategory.HIPS, AsanaCategory.EVENING), cats(AgeBand.AGE_51_64))
        assertEquals(listOf(AsanaCategory.EVENING, AsanaCategory.HIPS, AsanaCategory.DESK), cats(AgeBand.AGE_65_PLUS))
        AgeBand.entries.forEach { b ->
            assertTrue("$b has categories", cats(b).isNotEmpty())
            assertEquals("$b lists no category twice", cats(b).size, cats(b).toSet().size)
        }
    }

    // ---- The cap in the hold timer ----------------------------------------------------------

    /** A steady, well-aligned hold completes exactly at the band's cap. */
    @Test
    fun holdTimerCompletesAtEachBandsCap() {
        AgeBand.entries.forEach { band ->
            val cap = AgeProfile.utkatasanaCapFor(band)
            val h = HoldStateMachine(maxHoldSec = cap)
            var t = 0.0
            var caps = 0
            var st = HoldStatus.IDLE
            repeat(25 * 35) {
                st = h.update(t, 90.0)
                if (st.capReached) caps++
                t += 0.04
            }
            assertEquals("$band", 1, caps)
            assertEquals("$band", HoldPhase.COMPLETE, st.phase)
            assertEquals("$band", cap.toDouble(), st.bestGraceSec, 0.1)
        }
    }

    // ---- The cap through the real pipeline --------------------------------------------------

    private class ChairRun(val capEvents: List<PipelineEvent.HoldCapReached>, val firstCapAtSec: Double?, val last: PipelineOutput)

    /** Holds a perfect Chair Pose for [seconds] at 25 FPS (no sensors, as in a replay). */
    private fun holdChair(pipeline: PosePipeline, seconds: Int): ChairRun {
        val b = SyntheticPoseBuilder()
        val caps = mutableListOf<PipelineEvent.HoldCapReached>()
        var firstAt: Double? = null
        var out: PipelineOutput? = null
        repeat(seconds * 25) { i ->
            val o = pipeline.process(b.chair(i * 40L), null)
            o.events.filterIsInstance<PipelineEvent.HoldCapReached>().forEach {
                caps += it
                if (firstAt == null) firstAt = o.hold.graceHoldSec
            }
            out = o
        }
        return ChairRun(caps, firstAt, out!!)
    }

    @Test
    fun defaultPipelineKeepsTheThirtySecondCap() {
        val p = PosePipeline()
        assertEquals(30, p.utkatasanaCapSec)
        val early = holdChair(p, 25)
        assertTrue("no cap before 30 s", early.capEvents.isEmpty())
        assertEquals(30, early.last.holdCapSec)
        assertEquals(HoldPhase.HOLDING, early.last.hold.phase)

        val full = holdChair(PosePipeline(), 34)
        assertEquals(1, full.capEvents.size)
        assertEquals(30, full.capEvents.single().seconds)
        assertEquals(30.0, full.firstCapAtSec!!, 0.1)
        assertEquals(HoldPhase.COMPLETE, full.last.hold.phase)
    }

    @Test
    fun pipelineCapFollowsTheAgeBand() {
        for ((band, cap) in listOf(AgeBand.AGE_51_64 to 20, AgeBand.AGE_65_PLUS to 15)) {
            val run = holdChair(PosePipeline(ToleranceProfile.STANDARD, AgeProfile.utkatasanaCapFor(band)), 34)
            assertEquals("$band", 1, run.capEvents.size)
            assertEquals("$band: the coach announces the real number", cap, run.capEvents.single().seconds)
            assertEquals("$band", cap.toDouble(), run.firstCapAtSec!!, 0.1)
            assertEquals("$band: the HUD shows the real cap", cap, run.last.holdCapSec)
            assertEquals("$band", HoldPhase.COMPLETE, run.last.hold.phase)
        }
        for (band in listOf(AgeBand.UNDER_18, AgeBand.AGE_18_30, AgeBand.AGE_31_50)) {
            val run = holdChair(PosePipeline(ToleranceProfile.STANDARD, AgeProfile.utkatasanaCapFor(band)), 25)
            assertTrue("$band keeps 30 s", run.capEvents.isEmpty())
            assertEquals(30, run.last.holdCapSec)
        }
    }

    /** Age changes the hold cap and nothing else: the alignment score is the same for every band. */
    @Test
    fun scoringIsIdenticalForEveryBand() {
        val scores = AgeBand.entries.map { band ->
            holdChair(PosePipeline(ToleranceProfile.STANDARD, AgeProfile.utkatasanaCapFor(band)), 5).last.score!!
        }
        assertEquals(1, scores.toSet().size)
        assertEquals(100.0, scores.first(), 0.5)
    }

    @Test
    fun onlyChairPoseIsCapped() {
        val p = PosePipeline(ToleranceProfile.STANDARD, utkatasanaCapSec = 15)
        assertEquals(15, p.holdCapSecFor(Asana.UTKATASANA))
        Asana.entries.filter { it != Asana.UTKATASANA }.forEach { assertNull("$it", p.holdCapSecFor(it)) }
        assertFalse(Asana.entries.any { it != Asana.UTKATASANA && it.maxHoldSec != null })
    }
}
