package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.Suitability
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.practice.AgeCautionBanner
import com.companion.yogawellness.ui.practice.practiceAgeNote
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The practice screen's one-time age note for a pose started from "Practice this live". */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class PracticeAgeNoteTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val younger = listOf(AgeBand.UNDER_18, AgeBand.AGE_18_30, AgeBand.AGE_31_50)

    // ---- When there is a note ---------------------------------------------------------------

    @Test
    fun nothingToSayWithoutATargetOrABand() {
        assertNull(practiceAgeNote(null, AgeBand.AGE_65_PLUS))
        assertNull(practiceAgeNote(Asana.VRIKSHASANA, null))
    }

    @Test
    fun noNoteBelowFiftyOne() {
        for (band in younger) for (asana in Asana.entries) assertNull("$asana / $band", practiceAgeNote(asana, band))
    }

    @Test
    fun fiftyOneToSixtyFour() {
        val b = AgeBand.AGE_51_64
        assertNull("Tree Pose is fine with a tip; tips stay on the Learn page", practiceAgeNote(Asana.VRIKSHASANA, b))
        assertNull(practiceAgeNote(Asana.VIRABHADRASANA_II, b))
        assertEquals(Suitability.CAUTION, practiceAgeNote(Asana.TRIKONASANA, b)!!.advice.suitability)
        practiceAgeNote(Asana.UTKATASANA, b)!!.let {
            assertEquals(Suitability.CAUTION, it.advice.suitability)
            assertTrue(it.advice.note!!.contains("20 s"))
        }
    }

    @Test
    fun sixtyFiveAndOver() {
        val b = AgeBand.AGE_65_PLUS
        assertEquals(Suitability.CAUTION, practiceAgeNote(Asana.VRIKSHASANA, b)!!.advice.suitability)
        practiceAgeNote(Asana.VIRABHADRASANA_II, b)!!.let {
            assertEquals(Suitability.USE_EASIER_VARIANT, it.advice.suitability)
            assertEquals("Bend the front knee less.", it.easierOption)
        }
        assertNotNull(practiceAgeNote(Asana.TRIKONASANA, b))
        assertTrue(practiceAgeNote(Asana.UTKATASANA, b)!!.advice.note!!.contains("15 s"))
    }

    @Test
    fun calibrationPoseNeverGetsANote() {
        AgeBand.entries.forEach { assertNull("$it", practiceAgeNote(Asana.TADASANA, it)) }
    }

    @Test
    fun everyNoteHasTextAndAnEasierOption() {
        for (band in AgeBand.entries) for (asana in Asana.entries) {
            practiceAgeNote(asana, band)?.let {
                assertNotNull("$asana / $band", it.advice.note)
                assertNotNull("$asana / $band", it.easierOption)
            }
        }
    }

    // ---- The banner -------------------------------------------------------------------------

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val file = File("build/screenshots", "$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun bannerShowsTheNoteAndDismissesOnce() {
        val note = practiceAgeNote(Asana.VIRABHADRASANA_II, AgeBand.AGE_65_PLUS)!!
        var dismissals = 0
        compose.setContent {
            YogaWellnessTheme {
                var shown by remember { mutableStateOf(true) }
                Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (shown) AgeCautionBanner(note) { shown = false; dismissals++ }
                    }
                }
            }
        }
        compose.onNodeWithText("A gentler option may suit you").assertExists()
        compose.onNodeWithText("Consider a shallower bend in the front knee.").assertExists()
        compose.onNodeWithText("Easier option: Bend the front knee less.").assertExists()
        compose.onNodeWithText("For ages 65 and over · not medical advice").assertExists()
        compose.onNodeWithContentDescription("Dismiss age note").assertWidthIsAtLeast(44.dp).assertHeightIsAtLeast(44.dp)
        save("practice_age_note")

        compose.onNodeWithContentDescription("Dismiss age note").performClick()
        assertEquals(1, dismissals)
        assertEquals(0, compose.onAllNodesWithText("A gentler option may suit you").fetchSemanticsNodes().size)
    }

    @Test
    fun cautionBannerUsesTheCautionTitle() {
        val note = practiceAgeNote(Asana.UTKATASANA, AgeBand.AGE_51_64)!!
        compose.setContent { YogaWellnessTheme { AgeCautionBanner(note) {} } }
        compose.onNodeWithText("Go gently with this one").assertExists()
        compose.onNodeWithText("the live timer stops at 20 s", substring = true).assertExists()
    }
}
