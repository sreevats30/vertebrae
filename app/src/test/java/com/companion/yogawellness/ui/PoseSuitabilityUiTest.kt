package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.codex.AsanaCodexScreen
import com.companion.yogawellness.ui.codex.AsanaDetailContent
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
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

/** Age-band suitability on the Learn tab and the pose page: tags, the "For you" view and care notices. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h1000dp-xxhdpi")
class PoseSuitabilityUiTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<YogaWellnessApp>()

    @Before
    fun cleanSlate() {
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
    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size
    private fun scrollTo(text: String) = compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    private fun detail(id: String, band: AgeBand?, onPracticeLive: (Asana) -> Unit = {}) = setScreen {
        AsanaDetailContent(AsanaCatalog32.byId(id)!!, initialPlaying = false, onBack = {}, onPracticeLive = onPracticeLive, ageBand = band)
    }

    // ---- Learn tab --------------------------------------------------------------------------

    @Test
    fun withoutABandLearnIsThePlainCatalog() {
        setScreen { AsanaCodexScreen(onOpen = {}) } // nothing stored yet
        compose.onNodeWithText("1. Cat-Cow Flow").assertExists()
        assertEquals(0, count("For you"))
        assertEquals(0, count("Go gently"))
    }

    @Test
    fun learnFollowsTheStoredBand() {
        app.preferences.ageBand = AgeBand.AGE_18_30
        setScreen { AsanaCodexScreen(onOpen = {}) }
        assertEquals(1, count("For you")) // the chip
        org.junit.Assert.assertTrue(count("Good fit") >= 1) // tags on recommended Desk poses in view
        assertEquals(0, count("Go gently"))
    }

    @Test
    fun forYouViewListsSuggestedPosesFirstAndKeepsEveryPoseOpen() {
        var opened: String? = null
        setScreen { AsanaCodexScreen(onOpen = { opened = it }, ageBand = AgeBand.AGE_65_PLUS) }
        compose.onNodeWithText("Desk Worker & Tech-Neck Relief").assertExists() // "All" is still the default view
        compose.onNodeWithText("For you").performClick().assertIsSelected()
        compose.onNodeWithText("Recommended for you").assertExists()
        compose.onNodeWithText("Sorted for ages 65 and over.", substring = true).assertExists()
        compose.onNodeWithText("29. Child's Pose").assertExists() // Evening leads for 65+
        assertEquals(0, count("Desk Worker & Tech-Neck Relief"))
        save("learn_for_you_65_plus")

        scrollTo("Also suitable")
        scrollTo("With a little extra care")
        compose.onNodeWithText("Still open to you — each has a gentler option").assertExists()
        // A cautioned pose is listed, tagged, and opens like any other.
        scrollTo("19. Tree Pose")
        compose.onNodeWithText("19. Tree Pose").performClick()
        assertEquals("tree_pose", opened)
        save("learn_for_you_65_plus_care")

        // Leaving "For you" restores the category view (the chip row is the list's second item).
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("All").performClick()
        scrollTo("Desk Worker & Tech-Neck Relief")
        assertEquals(0, count("Recommended for you"))
    }

    @Test
    fun forYouViewStillSearches() {
        setScreen { AsanaCodexScreen(onOpen = {}, ageBand = AgeBand.AGE_51_64) }
        compose.onNodeWithText("For you").performClick()
        compose.onNodeWithText("Search poses").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("warrior")
        compose.onNodeWithText("13. Warrior I").assertExists()
        compose.onNodeWithText("20. Warrior III").assertExists() // cautioned, but never hidden
        assertEquals(0, count("29. Child's Pose"))
    }

    @Test
    @Config(qualifiers = "w360dp-h1000dp-night-xxhdpi")
    fun forYouViewInDarkTheme() {
        setScreen { AsanaCodexScreen(onOpen = {}, ageBand = AgeBand.AGE_51_64) }
        compose.onNodeWithText("For you").performClick()
        compose.onNodeWithText("Recommended for you").assertExists()
        save("dark/learn_for_you")
    }

    // ---- Pose page --------------------------------------------------------------------------

    @Test
    fun cautionedPoseShowsANoticeThatDoesNotBlockPractice() {
        var practised: Asana? = null
        detail("tree_pose", AgeBand.AGE_65_PLUS) { practised = it }
        compose.onNodeWithText("Go gently with this one").assertExists()
        compose.onNodeWithText("Consider practising next to a wall or chair.").assertExists()
        compose.onNodeWithText("Easier option: Keep the toes on the floor with the heel on the ankle.").assertExists()
        compose.onNodeWithText("You can still practise this pose.", substring = true).assertExists()
        save("codex_detail_tree_caution")

        compose.onNodeWithText("See easier options").performClick()
        compose.onNodeWithText("Make it easier").assertExists()
        compose.onNodeWithText("Practice this live").performScrollTo().performClick()
        assertEquals(Asana.VRIKSHASANA, practised)
    }

    @Test
    fun easierVariantPoseShowsItsOwnNotice() {
        detail("warrior_2", AgeBand.AGE_65_PLUS)
        compose.onNodeWithText("A gentler option may suit you").assertExists()
        compose.onNodeWithText("Consider a shallower bend in the front knee.").assertExists()
        compose.onNodeWithText("Easier option: Bend the front knee less.").assertExists()
    }

    @Test
    fun chairPoseNoticeNamesTheBandsHoldCap() {
        detail("chair_pose", AgeBand.AGE_65_PLUS)
        compose.onNodeWithText("the live timer stops at 15 s", substring = true).assertExists()
        // The static safety note no longer claims 30 s for everyone.
        compose.onNodeWithText("30 s, or less for older age ranges", substring = true).performScrollTo().assertExists()
    }

    @Test
    fun okPoseWithATipShowsItWithoutACaution() {
        detail("tree_pose", AgeBand.AGE_51_64)
        compose.onNodeWithText("A tip for your age range").assertExists()
        compose.onNodeWithText("Consider keeping a wall or chair within reach.").assertExists()
        assertEquals(0, count("Go gently with this one"))
        assertEquals(0, count("See easier options"))
    }

    @Test
    fun suitablePoseShowsNoNotice() {
        detail("tree_pose", AgeBand.AGE_18_30)
        listOf("Go gently with this one", "A gentler option may suit you", "A tip for your age range", "See easier options")
            .forEach { assertEquals(it, 0, count(it)) }
        compose.onNodeWithText("Play").assertExists()
    }

    @Test
    fun noBandMeansNoNotice() {
        detail("tree_pose", null)
        assertEquals(0, count("Go gently with this one"))
    }

    @Test
    @Config(qualifiers = "w360dp-h1700dp-xxhdpi")
    fun cautionNoticeAtDoubleFontScale() {
        setScreen {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                AsanaDetailContent(
                    AsanaCatalog32.byId("triangle_pose")!!, initialPlaying = false, onBack = {}, onPracticeLive = {},
                    ageBand = AgeBand.AGE_51_64
                )
            }
        }
        compose.onNodeWithText("Go gently with this one").assertExists()
        compose.onNodeWithText("See easier options").performScrollTo().assertExists()
        save("codex_detail_caution_fontscale_2")
    }

    @Test
    @Config(qualifiers = "w360dp-h1000dp-night-xxhdpi")
    fun cautionNoticeInDarkTheme() {
        detail("chair_pose", AgeBand.AGE_51_64)
        compose.onNodeWithText("the live timer stops at 20 s", substring = true).assertExists()
        save("dark/codex_detail_caution")
    }
}

