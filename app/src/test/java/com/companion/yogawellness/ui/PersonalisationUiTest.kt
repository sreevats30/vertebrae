package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.home.HomeContent
import com.companion.yogawellness.ui.home.HomeState
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.Persona
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/** Under-18 flow, Today's suggestion on Home, Tele-MANAS entries and the reminder setting. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h2600dp-xxhdpi")
class PersonalisationUiTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
    private val today = LocalDate.now().toEpochDay()

    @Before
    fun clean() {
        runBlocking { app.repository.deleteAllData() }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val file = File("build/screenshots", "$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setScreen(content: @Composable () -> Unit) = compose.setContent { YogaWellnessTheme { content() } }
    private fun count(text: String, substring: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().size

    private fun home(band: AgeBand?, now: LocalTime = LocalTime.of(13, 0), usualHour: Int? = null, onOpenPose: (String) -> Unit = {}) =
        setScreen {
            HomeContent(
                HomeState(null, null, null, ActivitySummary.empty(today), today, ToleranceProfile.STANDARD, band, usualHour, now),
                onProfile = {}, onStartPractice = {}, onCheckIn = {}, onQuestionnaire = {}, onDashboard = {}, onSettings = {},
                onOpenPose = onOpenPose
            )
        }

    // ---- Home ---------------------------------------------------------------------------------

    @Test
    fun under18HomeHasNoQuestionnairesButKeepsTheCheckIn() {
        home(AgeBand.UNDER_18)
        compose.onNodeWithText("Check in now").assertExists()
        assertEquals(0, count("Mind & mood"))
        assertEquals(0, count("PHQ-9", substring = true))
        assertEquals(0, count("GAD-7", substring = true))
    }

    @Test
    fun adultHomeStillOffersTheQuestionnaires() {
        home(AgeBand.AGE_18_30)
        compose.onNodeWithText("Mind & mood").performScrollTo().assertExists()
        compose.onNodeWithText("Start PHQ-9").performScrollTo().assertExists()
    }

    @Test
    fun todaysSuggestionShowsTheRoutineAndStartsIt() {
        var opened: String? = null
        home(AgeBand.AGE_65_PLUS, now = LocalTime.of(13, 0), usualHour = 7) { opened = it }
        compose.onNodeWithText("TODAY'S SUGGESTION").performScrollTo().assertExists()
        compose.onNodeWithText("Desk break").assertExists()
        compose.onNodeWithText("You usually practise around 7 AM.").assertExists()
        compose.onNodeWithText("Practise on a light stomach", substring = true).assertExists()
        save("home_todays_suggestion")
        compose.onNodeWithText("Start with Cat-Cow Flow").performScrollTo().performClick()
        assertEquals("cat_cow", opened)
    }

    @Test
    fun noUsualTimeLineWithoutAPattern() {
        home(AgeBand.AGE_18_30, now = LocalTime.of(7, 0))
        compose.onNodeWithText("Morning set").performScrollTo().assertExists()
        assertEquals(0, count("You usually practise", substring = true))
    }

    @Test
    fun noSuggestionBeforeABandIsChosen() {
        home(null)
        assertEquals(0, count("TODAY'S SUGGESTION"))
    }

    @Test
    @Config(qualifiers = "w360dp-h2600dp-night-xxhdpi")
    fun suggestionInDarkTheme() {
        home(AgeBand.AGE_51_64, now = LocalTime.of(7, 0), usualHour = 7)
        compose.onNodeWithText("Morning set").performScrollTo()
        save("dark/home_todays_suggestion")
    }

    // ---- Insights -----------------------------------------------------------------------------

    private fun insights(band: AgeBand?) {
        val data = com.companion.yogawellness.wellness.DashboardBuilderTest().personaDashboard(Persona.A_CONSISTENT)
        setScreen {
            com.companion.yogawellness.ui.dashboard.DashboardScreenContent(
                data, null, error = null, onBack = {}, onExport = {}, ageBand = band
            )
        }
    }

    @Test
    fun under18InsightsSwapsTheReflectionsForTeleManas() {
        insights(AgeBand.UNDER_18)
        assertEquals(0, count("Self-reflection check-ins"))
        compose.onNodeWithText("Call Tele-MANAS 14416").performScrollTo().assertExists()
        compose.onNodeWithText("Weekly trend").assertExists() // statistics keep working from daily check-ins
    }

    @Test
    fun adultInsightsKeepTheReflections() {
        insights(AgeBand.AGE_31_50)
        compose.onNodeWithText("Self-reflection check-ins").performScrollTo().assertExists()
    }

    // ---- Settings -----------------------------------------------------------------------------

    @Test
    fun settingsShowsTeleManasAndAnOffReminder() {
        app.preferences.ageBand = AgeBand.AGE_18_30
        setScreen { com.companion.yogawellness.ui.settings.SettingsScreen(onBack = {}, onDataDeleted = {}) }
        compose.onNodeWithText("Call Tele-MANAS 14416").performScrollTo().assertExists()
        compose.onNodeWithText("Daily reminder").performScrollTo().assertExists()
        compose.onNodeWithText("nothing is sent anywhere", substring = true).assertExists()
        assertEquals(0, count("Earlier")) // hour controls only once it is on
        save("settings_personalisation")
    }
}
