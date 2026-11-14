package com.companion.yogawellness.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.x contrast checks for the Twilight (front pages) and Ink (detection) palettes, plus pins
 * that keep Ink and the semantic colours byte-identical. COLOUR_GUIDE.md §3 quotes these numbers;
 * the full table is written to app/build/reports/contrast/contrast.md on every run.
 */
class ThemeContrastTest {

    private val t = Twilight

    private val surfaces = listOf("canvas" to t.canvas, "raised" to t.raised, "control" to t.control, "controlHigh" to t.controlHigh)

    // ---- Pins ----------------------------------------------------------------------------------

    @Test
    fun inkPaletteIsUnchanged() {
        val original = AppColors(
            canvas = Color(0xFF0E0E0D), raised = Color(0xFF171716), control = Color(0xFF1F1F1D), controlHigh = Color(0xFF292927),
            line = Color(0xFF262624), lineStrong = Color(0xFF3B3A37),
            text = Color(0xFFEEECE7), textSecondary = Color(0xFFABA9A2), textTertiary = Color(0xFF807E78), textDisabled = Color(0xFF57564F),
            inverse = Color(0xFFEEECE7), onInverse = Color(0xFF0E0E0D),
            accent = Color(0xFF6DBE98), onAccent = Color(0xFF0B1A13), accentMuted = Color(0xFF1A2A22),
            warning = Color(0xFFE2AE52), warningMuted = Color(0xFF2D2416),
            negative = Color(0xFFE8705E), negativeMuted = Color(0xFF301B17),
            mood = Color(0xFF6DBE98), stress = Color(0xFFE38B60), sleep = Color(0xFF8EA5E8), quality = Color(0xFFD4B77F)
        )
        assertEquals(original, Ink)
    }

    @Test
    fun semanticColoursAreByteIdenticalInBothPalettes() {
        assertEquals(0xFF6DBE98.toInt(), ScoreColors.Green.toArgb())
        assertEquals(0xFFE2AE52.toInt(), ScoreColors.Amber.toArgb())
        assertEquals(0xFFE8705E.toInt(), ScoreColors.Red.toArgb())
        assertEquals(0xFF8C8A84.toInt(), ScoreColors.Neutral.toArgb())
        for (p in listOf(Ink, Twilight)) {
            assertEquals(ScoreColors.Green, p.accent)
            assertEquals(ScoreColors.Amber, p.warning)
            assertEquals(ScoreColors.Red, p.negative)
        }
        assertEquals(Ink.onAccent, Twilight.onAccent)
        assertEquals(Ink.mood, Twilight.mood)
        assertEquals(Ink.stress, Twilight.stress)
        assertEquals(Ink.sleep, Twilight.sleep)
        assertEquals(Ink.quality, Twilight.quality)
    }

    /** Twilight may differ from Ink only in neutrals, text, inverse/onInverse and the muted fills. */
    @Test
    fun twilightOnlyOverridesTheAllowedTokens() {
        val onlyAllowed = Ink.copy(
            canvas = t.canvas, raised = t.raised, control = t.control, controlHigh = t.controlHigh,
            line = t.line, lineStrong = t.lineStrong,
            text = t.text, textSecondary = t.textSecondary, textTertiary = t.textTertiary, textDisabled = t.textDisabled,
            inverse = t.inverse, onInverse = t.onInverse,
            accentMuted = t.accentMuted, warningMuted = t.warningMuted, negativeMuted = t.negativeMuted
        )
        assertEquals(onlyAllowed, Twilight)
    }

    @Test
    fun twilightTokensMatchTheGuide() {
        val expected = mapOf(
            "canvas" to "#0F1719", "raised" to "#161E22", "control" to "#1D272C", "controlHigh" to "#232F35",
            "line" to "#243038", "lineStrong" to "#637680",
            "text" to "#E8EEEC", "textSecondary" to "#A9B7B8", "textTertiary" to "#8B9CA4", "textDisabled" to "#55636A",
            "inverse" to "#DCE9E4", "onInverse" to "#0F1719",
            "accentMuted" to "#203130", "warningMuted" to "#2E2F28", "negativeMuted" to "#2F2829"
        )
        val actual = mapOf(
            "canvas" to t.canvas, "raised" to t.raised, "control" to t.control, "controlHigh" to t.controlHigh,
            "line" to t.line, "lineStrong" to t.lineStrong,
            "text" to t.text, "textSecondary" to t.textSecondary, "textTertiary" to t.textTertiary, "textDisabled" to t.textDisabled,
            "inverse" to t.inverse, "onInverse" to t.onInverse,
            "accentMuted" to t.accentMuted, "warningMuted" to t.warningMuted, "negativeMuted" to t.negativeMuted
        ).mapValues { hex(it.value) }
        assertEquals(expected, actual)
    }

    // ---- Twilight thresholds -------------------------------------------------------------------

    @Test
    fun textTokensMeetAaOnEveryTwilightSurface() {
        for (fg in listOf("text" to t.text, "textSecondary" to t.textSecondary, "textTertiary" to t.textTertiary)) {
            for (bg in surfaces) atLeast(4.5, fg, bg)
        }
    }

    /** Outlines that identify a control need 3:1. controlHigh (~2.9:1) is a pressed fill, not an outline surface. */
    @Test
    fun lineStrongMeetsNonTextContrastOnControlSurfaces() {
        for (bg in surfaces.filter { it.first != "controlHigh" }) atLeast(3.0, "lineStrong" to t.lineStrong, bg)
    }

    @Test
    fun semanticColoursMeetAaOnEveryTwilightSurface() {
        for (fg in listOf("green" to ScoreColors.Green, "amber" to ScoreColors.Amber, "red" to ScoreColors.Red)) {
            for (bg in surfaces) atLeast(4.5, fg, bg)
        }
    }

    @Test
    fun chartSeriesMeetAaOnEveryTwilightSurface() {
        for (fg in listOf("mood" to t.mood, "stress" to t.stress, "sleep" to t.sleep, "quality" to t.quality)) {
            for (bg in surfaces) atLeast(4.5, fg, bg)
        }
    }

    @Test
    fun actionPairsMeetAa() {
        atLeast(4.5, "inverse" to t.inverse, "onInverse" to t.onInverse)
        atLeast(4.5, "accent" to t.accent, "onAccent" to t.onAccent)
    }

    // ---- Muted fills ---------------------------------------------------------------------------

    @Test
    fun mutedFillsAreTheSemanticColourAt12PercentOverRaised() {
        assertEquals(0.12f, TWILIGHT_MUTED_ALPHA)
        for ((fg, muted) in listOf(t.accent to t.accentMuted, t.warning to t.warningMuted, t.negative to t.negativeMuted)) {
            val expected = blend(fg, t.raised, TWILIGHT_MUTED_ALPHA.toDouble())
            val a = rgb(muted)
            for (i in 0..2) assertTrue("${hex(muted)} vs ${hex(expected)}", abs(a[i] - rgb(expected)[i]) <= 1)
        }
    }

    @Test
    fun eachMutedFillKeepsAaWithItsOwnForeground() {
        atLeast(4.5, "accent" to t.accent, "accentMuted" to t.accentMuted)
        atLeast(4.5, "warning" to t.warning, "warningMuted" to t.warningMuted)
        atLeast(4.5, "negative" to t.negative, "negativeMuted" to t.negativeMuted)
    }

    /** Why 12% is the ceiling: red over a stronger red fill drops below 4.5:1. */
    @Test
    fun strongerNegativeFillWouldFailAa() {
        val at18 = blend(t.negative, t.raised, 0.18)
        assertTrue(contrast(t.negative, at18) < 4.5)
    }

    // ---- Report --------------------------------------------------------------------------------

    /** Every foreground token against every surface, for both palettes (app/build/reports/contrast/contrast.md). */
    @Test
    fun writeContrastTable() {
        val sb = StringBuilder()
        for ((name, p) in listOf("Twilight" to Twilight, "Ink" to Ink)) {
            val bgs = listOf("canvas" to p.canvas, "raised" to p.raised, "control" to p.control, "controlHigh" to p.controlHigh)
            val fgs = listOf(
                "text" to p.text, "textSecondary" to p.textSecondary, "textTertiary" to p.textTertiary,
                "textDisabled" to p.textDisabled, "line" to p.line, "lineStrong" to p.lineStrong,
                "green (accent)" to p.accent, "amber (warning)" to p.warning, "red (negative)" to p.negative,
                "neutral (ScoreColors)" to ScoreColors.Neutral,
                "mood" to p.mood, "stress" to p.stress, "sleep" to p.sleep, "quality" to p.quality
            )
            sb.append("## $name\n\n| token | ").append(bgs.joinToString(" | ") { "${it.first} ${hex(it.second)}" }).append(" |\n")
            sb.append("|---|").append(bgs.joinToString("") { "---|" }).append("\n")
            for ((fn, fc) in fgs) {
                sb.append("| $fn ${hex(fc)} | ").append(bgs.joinToString(" | ") { f2(contrast(fc, it.second)) }).append(" |\n")
            }
            sb.append("\n| pair | ratio |\n|---|---|\n")
            sb.append("| inverse / onInverse | ${f2(contrast(p.inverse, p.onInverse))} |\n")
            sb.append("| accent / onAccent | ${f2(contrast(p.accent, p.onAccent))} |\n")
            sb.append("| accent / accentMuted | ${f2(contrast(p.accent, p.accentMuted))} |\n")
            sb.append("| warning / warningMuted | ${f2(contrast(p.warning, p.warningMuted))} |\n")
            sb.append("| negative / negativeMuted | ${f2(contrast(p.negative, p.negativeMuted))} |\n\n")
        }
        val out = File("build/reports/contrast").apply { mkdirs() }
        File(out, "contrast.md").writeText(sb.toString())
        println(sb)
    }

    // ---- WCAG 2.x ------------------------------------------------------------------------------

    private fun atLeast(min: Double, fg: Pair<String, Color>, bg: Pair<String, Color>) {
        val r = contrast(fg.second, bg.second)
        assertTrue("${fg.first} ${hex(fg.second)} on ${bg.first} ${hex(bg.second)} = ${f2(r)}:1, needs $min:1", r >= min)
    }

    companion object {
        fun rgb(c: Color): IntArray {
            val argb = c.toArgb()
            require(argb ushr 24 == 0xFF) { "contrast is only defined here for opaque tokens" }
            return intArrayOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)
        }

        private fun linear(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }

        /** WCAG 2.x relative luminance. */
        fun luminance(c: Color): Double {
            val (r, g, b) = rgb(c).toList()
            return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
        }

        /** WCAG 2.x contrast ratio, 1..21. */
        fun contrast(a: Color, b: Color): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        /** [fg] at [alpha] over opaque [bg], composited in sRGB and rounded to 8 bits (as the hex tokens are). */
        fun blend(fg: Color, bg: Color, alpha: Double): Color {
            val f = rgb(fg)
            val b = rgb(bg)
            val ch = IntArray(3) { (f[it] * alpha + b[it] * (1 - alpha)).roundToInt() }
            return Color(red = ch[0], green = ch[1], blue = ch[2])
        }

        fun hex(c: Color) = String.format(Locale.US, "#%06X", c.toArgb() and 0xFFFFFF)

        private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)
    }
}
