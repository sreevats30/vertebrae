package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.codex.AsanaCodexScreen
import com.companion.yogawellness.ui.codex.AsanaDetailContent
import com.companion.yogawellness.ui.home.HomeContent
import com.companion.yogawellness.ui.home.HomeState
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.Persona
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Dark-theme renders of every main screen (the theme most people use the app in). Each test fails
 * if the screen throws while composing or drawing, and saves a PNG to app/build/screenshots/dark/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-night-xxhdpi")
class DarkThemeRenderTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots/dark").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setScreen(content: @Composable () -> Unit) = compose.setContent { YogaWellnessTheme { content() } }

    @Test
    fun homeFirstRun() {
        val today = LocalDate.now().toEpochDay()
        setScreen {
            HomeContent(
                HomeState(null, null, null, ActivitySummary.empty(today), today, ToleranceProfile.STANDARD),
                onProfile = {}, onStartPractice = {}, onCheckIn = {}, onQuestionnaire = {}, onDashboard = {}, onSettings = {}
            )
        }
        compose.onNodeWithText("Check in now").assertExists()
        save("home_first_run")
    }

    @Test
    fun learnList() {
        setScreen { AsanaCodexScreen(onOpen = {}) }
        compose.onNodeWithText("1. Cat-Cow Flow").assertExists()
        save("learn_list")
    }

    @Test
    fun learnDetail() {
        setScreen { AsanaDetailContent(AsanaCatalog32.byId("warrior_2")!!, initialPlaying = false, onBack = {}, onPracticeLive = {}) }
        compose.onNodeWithText("Play").assertExists()
        save("learn_detail")
    }

    @Test
    fun historyEmpty() {
        setScreen {
            com.companion.yogawellness.ui.history.SessionHistoryContent(
                com.companion.yogawellness.ui.history.HistoryUiState(loading = false), onStartPractice = {}, onDelete = {}
            )
        }
        compose.onNodeWithText("Start practice").assertExists()
        save("history_empty")
    }

    @Test
    @Config(qualifiers = "w360dp-h1100dp-night-xxhdpi")
    fun historyPopulated() {
        setScreen {
            com.companion.yogawellness.ui.history.SessionHistoryContent(ScreenRenderTest().historyState(), onStartPractice = {}, onDelete = {})
        }
        compose.onNodeWithText("3m 20s").performClick()
        save("history_populated")
    }

    @Test
    @Config(qualifiers = "w360dp-h1700dp-night-xxhdpi")
    fun checkIn() {
        setScreen { com.companion.yogawellness.ui.wellness.CheckInScreen(onDone = {}) }
        compose.onNodeWithText("Stress").assertExists()
        save("checkin")
    }

    @Test
    @Config(qualifiers = "w360dp-h3000dp-night-xxhdpi")
    fun insights() {
        val data = com.companion.yogawellness.wellness.DashboardBuilderTest().personaDashboard(Persona.A_CONSISTENT)
        setScreen {
            com.companion.yogawellness.ui.dashboard.DashboardScreenContent(data, Persona.A_CONSISTENT.label, error = null, onBack = {}, onExport = {})
        }
        compose.onNodeWithText("Weekly trend").assertExists()
        save("insights")
    }

    @Test
    @Config(qualifiers = "w360dp-h1900dp-night-xxhdpi")
    fun settings() {
        setScreen { com.companion.yogawellness.ui.settings.SettingsScreen(onBack = {}, onDataDeleted = {}) }
        compose.onNodeWithText("Remove demo data").assertExists()
        save("settings")
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-night-xxhdpi")
    fun consent() {
        setScreen { com.companion.yogawellness.ui.consent.ConsentAndSafetyScreen(onAccepted = {}) }
        save("consent")
    }
}
