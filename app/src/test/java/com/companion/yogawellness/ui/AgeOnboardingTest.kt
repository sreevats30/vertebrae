package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.ui.onboarding.AGE_RANGE_REASON
import com.companion.yogawellness.ui.onboarding.AgeOnboardingScreen
import com.companion.yogawellness.ui.settings.SettingsScreen
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The one-question age onboarding: when it appears, what it requires, where the answer is saved,
 * and that Settings can change it. Renders are saved to app/build/screenshots/ for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h900dp-xxhdpi")
class AgeOnboardingTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<YogaWellnessApp>()

    @Before
    fun freshInstall() {
        runBlocking { app.repository.deleteAllData() } // also clears consent and any age band
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setScreen(content: @Composable () -> Unit) = compose.setContent { YogaWellnessTheme { content() } }

    private fun waitForText(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    // ---- The screen itself ------------------------------------------------------------------

    @Test
    fun continueIsDisabledUntilABandIsChosen() {
        var picked: AgeBand? = null
        setScreen { AgeOnboardingScreen(onContinue = { picked = it }) }
        compose.onNodeWithText("A little about you").assertExists()
        compose.onNodeWithText(AGE_RANGE_REASON).assertExists()
        AgeBand.entries.forEach { compose.onNodeWithText(it.label).assertExists() }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        save("age_onboarding")

        compose.onNodeWithText("31 to 50").performClick()
        compose.onNodeWithContentDescription("Age range: 31 to 50").assertIsSelected()
        compose.onNodeWithText("Continue").assertIsEnabled()
        // Changing your mind before continuing is fine; only the last choice counts.
        compose.onNodeWithText("51 to 64").performClick()
        save("age_onboarding_selected")
        compose.onNodeWithText("Continue").performClick()
        assertEquals(AgeBand.AGE_51_64, picked)
    }

    @Test
    fun everyRowIsALabelledRadioButtonWithAFullTouchTarget() {
        setScreen { AgeOnboardingScreen(onContinue = {}) }
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        AgeBand.entries.forEach { band ->
            compose.onNodeWithContentDescription("Age range: ${band.label}")
                .assert(radio)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h1500dp-xxhdpi")
    fun worksAtDoubleFontScale() {
        var picked: AgeBand? = null
        setScreen {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                AgeOnboardingScreen(onContinue = { picked = it })
            }
        }
        compose.onNodeWithText("65 and over").performScrollTo().performClick()
        save("age_onboarding_fontscale_2")
        compose.onNodeWithText("Continue").performScrollTo().assertIsEnabled().performClick()
        assertEquals(AgeBand.AGE_65_PLUS, picked)
    }

    @Test
    @Config(qualifiers = "w360dp-h900dp-night-xxhdpi")
    fun rendersInDarkTheme() {
        setScreen { AgeOnboardingScreen(onContinue = {}) }
        compose.onNodeWithText("18 to 30").performClick()
        save("dark/age_onboarding")
    }

    // ---- Where it sits in the app -----------------------------------------------------------

    @Test
    fun startRouteIsConsentThenAgeThenHome() {
        assertEquals(Routes.CONSENT, startRoute(hasConsent = false, hasAgeBand = false))
        assertEquals(Routes.CONSENT, startRoute(hasConsent = false, hasAgeBand = true))
        assertEquals(Routes.AGE, startRoute(hasConsent = true, hasAgeBand = false))
        assertEquals(Routes.HOME, startRoute(hasConsent = true, hasAgeBand = true))
    }

    /** Someone who accepted consent before this question existed sees it once, then Home. */
    @Test
    fun existingUserSeesOnboardingOnceAndTheChoiceIsSaved() {
        app.preferences.consentAcceptedAtMs = 1L
        assertNull(app.preferences.ageBand)
        setScreen { AppNavHost() }
        compose.onNodeWithText("A little about you").assertExists()
        // Full-screen destination: no bottom bar.
        assertEquals(0, compose.onAllNodesWithText("Insights").fetchSemanticsNodes().size)

        compose.onNodeWithText("18 to 30").performClick()
        compose.onNodeWithText("Continue").performClick()
        waitForText("Check in now")
        assertEquals(AgeBand.AGE_18_30, app.preferences.ageBand)
        listOf("Home", "Learn", "History", "Insights").forEach { compose.onNodeWithText(it).assertExists() }
        assertEquals(0, compose.onAllNodesWithText("A little about you").fetchSemanticsNodes().size)
    }

    @Test
    fun newUserGoesFromConsentToTheAgeQuestion() {
        setScreen { AppNavHost() }
        listOf(
            "I understand this app does not diagnose or treat any condition.",
            "I agree to on-device camera processing and to storing my data only on this device.",
            "I will practise safely and stop if anything hurts."
        ).forEach { compose.onNodeWithText(it).performScrollTo().performClick() }
        compose.onNodeWithText("Agree and continue").performScrollTo().performClick()
        waitForText("A little about you")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        assertNull(app.preferences.ageBand)
    }

    @Test
    fun returningUserWithABandGoesStraightHome() {
        app.preferences.consentAcceptedAtMs = 1L
        app.preferences.ageBand = AgeBand.AGE_31_50
        setScreen { AppNavHost() }
        compose.onNodeWithText("Check in now").assertExists()
        assertEquals(0, compose.onAllNodesWithText("A little about you").fetchSemanticsNodes().size)
    }

    // ---- Settings → Age range ---------------------------------------------------------------

    @Test
    fun settingsShowsTheBandAndCanChangeIt() {
        app.preferences.consentAcceptedAtMs = 1L
        app.preferences.ageBand = AgeBand.AGE_18_30
        setScreen { SettingsScreen(onBack = {}, onDataDeleted = {}) }
        compose.onNodeWithText("Age range").assertExists()
        compose.onNodeWithText("18 to 30").assertExists()

        compose.onNodeWithText("Change age range").performScrollTo().performClick()
        compose.onNodeWithText("Your age range").assertExists()
        compose.onNodeWithText("Save").assertIsNotEnabled() // nothing changed yet
        compose.onNodeWithText("65 and over").performClick()
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        assertEquals(AgeBand.AGE_65_PLUS, app.preferences.ageBand)
        compose.onNodeWithText("65 and over").assertExists() // the card updates without leaving Settings
        save("settings_age_range")
        assertEquals(0, compose.onAllNodesWithText("Your age range").fetchSemanticsNodes().size)
    }

    @Test
    fun cancellingTheSettingsDialogKeepsTheBand() {
        app.preferences.consentAcceptedAtMs = 1L
        app.preferences.ageBand = AgeBand.AGE_51_64
        setScreen { SettingsScreen(onBack = {}, onDataDeleted = {}) }
        compose.onNodeWithText("Change age range").performScrollTo().performClick()
        compose.onNodeWithText("Under 18").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(AgeBand.AGE_51_64, app.preferences.ageBand)
    }
}
