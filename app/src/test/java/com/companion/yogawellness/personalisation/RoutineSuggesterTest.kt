package com.companion.yogawellness.personalisation

import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.PoseSuitability
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.wellness.Persona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class RoutineSuggesterTest {

    private fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test
    fun windowForEveryHour() {
        (0..23).forEach { h ->
            val expected = when (h) {
                in 5..9 -> DayWindow.MORNING
                in 10..16 -> DayWindow.DAYTIME
                in 17..20 -> DayWindow.EARLY_EVENING
                else -> DayWindow.NIGHT
            }
            assertEquals("$h:00", expected, DayWindow.of(h))
            assertEquals(expected, RoutineSuggester.suggest(at(h, 30), AgeBand.AGE_18_30).window)
        }
    }

    /** The core safety rule: whatever the hour, band or stress, only RECOMMENDED / OK poses are suggested. */
    @Test
    fun neverSuggestsAPoseThatAsksForCare() {
        for (band in AgeBand.entries) for (h in 0..23) for (stress in listOf(null, 3, 9)) {
            val r = RoutineSuggester.suggest(at(h), band, stress)
            assertTrue("$band $h:00 stress=$stress is empty", r.poses.isNotEmpty())
            r.poses.forEach {
                assertTrue("$band $h:00: ${it.id}", PoseSuitability.adviceFor(it, band).suitability.isSuggested)
            }
            assertEquals("no repeats", r.poses.size, r.poses.distinctBy { it.id }.size)
        }
    }

    @Test
    fun highStressMeansTheCalmSetAtAnyHour() {
        for (band in AgeBand.entries) for (h in listOf(6, 12, 18)) {
            val r = RoutineSuggester.suggest(at(h), band, latestStress = RoutineSuggester.HIGH_STRESS)
            assertTrue(r.calmBecauseOfStress)
            assertEquals("Evening calm", r.title)
            assertTrue(r.poses.all { it.category == AsanaCategory.EVENING })
        }
        assertFalse(RoutineSuggester.suggest(at(12), AgeBand.AGE_18_30, latestStress = 6).calmBecauseOfStress)
    }

    @Test
    fun nightIsEveningCalmOnly() {
        for (band in AgeBand.entries) for (h in listOf(21, 23, 2, 4)) {
            val r = RoutineSuggester.suggest(at(h), band)
            assertTrue(r.poses.all { it.category == AsanaCategory.EVENING })
        }
    }

    @Test
    fun deskBreakIsShort() {
        for (band in AgeBand.entries) {
            val r = RoutineSuggester.suggest(at(13), band)
            assertEquals("Desk break", r.title)
            assertTrue("$band: ${r.minutes} min", r.minutes in 2..5)
            assertTrue(r.poses.all { it.category == AsanaCategory.DESK })
        }
        assertTrue(RoutineSuggester.suggest(at(13), AgeBand.AGE_65_PLUS).reason.contains("seated"))
    }

    @Test
    fun morningGivesOlderBandsALongerWarmUp() {
        val young = RoutineSuggester.suggest(at(7), AgeBand.AGE_18_30)
        val older = RoutineSuggester.suggest(at(7), AgeBand.AGE_51_64)
        assertEquals("Morning set", young.title)
        assertEquals(listOf("mountain", "cat_cow"), older.poses.take(2).map { it.id })
        assertTrue(older.reason.contains("warm-up"))
        assertTrue(older.poses.size > young.poses.size)
    }

    @Test
    fun earlyEveningIsStandingAndHipsWhereSuitable() {
        val r = RoutineSuggester.suggest(at(18), AgeBand.AGE_18_30)
        assertEquals("Standing and hips", r.title)
        assertTrue(r.poses.all { it.category == AsanaCategory.STANDING || it.category == AsanaCategory.HIPS })
        // 65+: every standing and hips pose asks for care, so the calm set stands in.
        assertEquals("Evening calm", RoutineSuggester.suggest(at(18), AgeBand.AGE_65_PLUS).title)
    }

    @Test
    fun minutesEstimate() {
        RoutineSuggester.suggest(at(7), AgeBand.AGE_18_30).let { assertEquals(RoutineSuggester.estimateMinutes(it.poses), it.minutes) }
        assertEquals(1, RoutineSuggester.estimateMinutes(emptyList()))
    }

    // ---- Personal pattern -------------------------------------------------------------------

    private fun session(hour: Int, day: Int) = PracticeSessionEntity(
        epochDay = day.toLong(),
        startedAtMs = LocalDate.ofEpochDay(day.toLong()).atTime(hour, 10).toInstant(ZoneOffset.UTC).toEpochMilli(),
        endedAtMs = 0, activeSec = 600.0, meanScore = null, profile = "STANDARD"
    )

    @Test
    fun noPatternFromFewerThanFiveSessions() {
        assertNull(RoutineSuggester.usualPracticeHour(emptyList(), ZoneOffset.UTC))
        assertNull(RoutineSuggester.usualPracticeHour((1..4).map { session(7, it) }, ZoneOffset.UTC))
    }

    @Test
    fun mostFrequentHourOnceThereAreFive() {
        val s = listOf(7, 7, 18, 7, 21).mapIndexed { i, h -> session(h, i) }
        assertEquals(7, RoutineSuggester.usualPracticeHour(s, ZoneOffset.UTC))
    }

    @Test
    fun aTieGoesToTheRecentHabit() {
        val s = listOf(7, 7, 18, 18, 12).mapIndexed { i, h -> session(h, i) }
        assertEquals(18, RoutineSuggester.usualPracticeHour(s, ZoneOffset.UTC))
    }

    @Test
    fun hourFormatting() {
        assertEquals(listOf("12 AM", "7 AM", "12 PM", "6 PM", "11 PM"), listOf(0, 7, 12, 18, 23).map(RoutineSuggester::formatHour))
    }

    // ---- Demo personas ----------------------------------------------------------------------

    @Test
    fun demoPersonasHaveFixedBands() {
        assertEquals(AgeBand.AGE_18_30, Persona.A_CONSISTENT.ageBand)
        assertEquals(AgeBand.AGE_18_30, Persona.B_EXAM_GAP.ageBand)
        assertEquals(AgeBand.AGE_31_50, Persona.C_NULL.ageBand)
    }

    @Test
    fun questionnairesFollowTheBandAndDefaultToOn() {
        assertTrue(AgeProfile.questionnairesEnabledFor(null))
        AgeBand.entries.forEach { assertEquals(it != AgeBand.UNDER_18, AgeProfile.questionnairesEnabledFor(it)) }
    }
}
