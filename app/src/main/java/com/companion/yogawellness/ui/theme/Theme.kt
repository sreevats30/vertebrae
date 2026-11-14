package com.companion.yogawellness.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/*
 * Design system: two palettes over the same [AppColors] tokens (see COLOUR_GUIDE.md).
 *
 *  - [Twilight] (default, [YogaWellnessTheme]): the front pages (Home, Learn, History, Insights,
 *    check-ins, questionnaires, consent, Settings). Calm, low-vividness cool blue-green neutrals and
 *    soft off-white text. Built with `Ink.copy(...)`, so it can only differ in neutrals, text, the
 *    primary-action fill and the muted semantic fills.
 *  - [Ink] (original, [DetectionTheme]): the pose-detection screens (the Practice route, its camera
 *    HUD, pose overlay, score explainer and recording UI). Warm near-black neutrals, ivory action
 *    colour. Do not edit: Practice must stay pixel-identical.
 *
 * In both palettes:
 *  - Green / amber / red keep exactly the same values ([ScoreColors], accent, warning, negative):
 *    green = on target / correct, amber = caution, red = problem / incorrect.
 *  - `inverse` is the action colour: primary buttons and the selected option of a selector.
 *  - Data series have their own hues and are only used inside charts and meters.
 *  - Surfaces are mostly the page itself. Raised surfaces are kept for sheets, dialogs, media tiles
 *    and controls; content is separated by spacing and hairlines rather than cards.
 */

/** Score bands (alignment, joints, sessions). Also used over the camera feed. */
object ScoreColors {
    val Green = Color(0xFF6DBE98)
    val Amber = Color(0xFFE2AE52)
    val Red = Color(0xFFE8705E)
    val Neutral = Color(0xFF8C8A84)
}

@Immutable
data class AppColors(
    /** Page background. */
    val canvas: Color,
    /** Sheets, dialogs and the bottom navigation. */
    val raised: Color,
    /** Inputs, selector tracks, media tiles, unselected chips. */
    val control: Color,
    /** Pressed / emphasised control fill. */
    val controlHigh: Color,
    /** Hairline dividers. */
    val line: Color,
    /** Outlines of secondary buttons and unchecked controls. */
    val lineStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    /** Ivory: primary-action fill and the selected option of a selector. */
    val inverse: Color,
    val onInverse: Color,
    /** Jade: on target, active, progress. */
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val negative: Color,
    val negativeMuted: Color,
    /** Data series — the same hues as the trend chart. */
    val mood: Color,
    val stress: Color,
    val sleep: Color,
    val quality: Color
)

/**
 * Original warm "practice room" palette. Used only by [DetectionTheme] (the Practice route).
 * Do not change any value here: ThemeContrastTest pins every token.
 */
internal val Ink = AppColors(
    canvas = Color(0xFF0E0E0D),
    raised = Color(0xFF171716),
    control = Color(0xFF1F1F1D),
    controlHigh = Color(0xFF292927),
    line = Color(0xFF262624),
    lineStrong = Color(0xFF3B3A37),
    text = Color(0xFFEEECE7),
    textSecondary = Color(0xFFABA9A2),
    textTertiary = Color(0xFF807E78),
    textDisabled = Color(0xFF57564F),
    inverse = Color(0xFFEEECE7),
    onInverse = Color(0xFF0E0E0D),
    accent = Color(0xFF6DBE98),
    onAccent = Color(0xFF0B1A13),
    accentMuted = Color(0xFF1A2A22),
    warning = Color(0xFFE2AE52),
    warningMuted = Color(0xFF2D2416),
    negative = Color(0xFFE8705E),
    negativeMuted = Color(0xFF301B17),
    mood = Color(0xFF6DBE98),
    stress = Color(0xFFE38B60),
    sleep = Color(0xFF8EA5E8),
    quality = Color(0xFFD4B77F)
)

/**
 * Calm front-page palette (COLOUR_GUIDE.md §3). Only neutrals, text, `inverse`/`onInverse` and the
 * three muted fills differ from [Ink]; every semantic and data-series colour is inherited unchanged.
 * Contrast thresholds are asserted in ThemeContrastTest.
 */
internal val Twilight = Ink.copy(
    canvas = Color(0xFF0F1719),
    raised = Color(0xFF161E22),
    control = Color(0xFF1D272C),
    // Lighter values push red below 4.5:1 (#243036 gives 4.46).
    controlHigh = Color(0xFF232F35),
    line = Color(0xFF243038),
    // >= 3:1 on canvas, raised and control (outlines that identify a control).
    lineStrong = Color(0xFF637680),
    text = Color(0xFFE8EEEC),
    textSecondary = Color(0xFFA9B7B8),
    // >= 4.5:1 on canvas, raised, control and controlHigh.
    textTertiary = Color(0xFF8B9CA4),
    textDisabled = Color(0xFF55636A),
    inverse = Color(0xFFDCE9E4),
    onInverse = Color(0xFF0F1719),
    // Semantic colour at 12% over `raised`. Do not go above 12%: red on negativeMuted is 4.54:1 at
    // 15% and 4.30:1 at 18%.
    accentMuted = Color(0xFF203130),
    warningMuted = Color(0xFF2E2F28),
    negativeMuted = Color(0xFF2F2829)
)

/** Alpha of the semantic colour in the Twilight muted fills (asserted in ThemeContrastTest). */
internal const val TWILIGHT_MUTED_ALPHA = 0.12f

private val LocalAppColors = staticCompositionLocalOf { Twilight }

private fun scheme(c: AppColors) = darkColorScheme(
    primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentMuted, onPrimaryContainer = c.accent,
    secondary = c.textSecondary, onSecondary = c.canvas, secondaryContainer = c.control, onSecondaryContainer = c.text,
    tertiary = c.accent, onTertiary = c.onAccent,
    background = c.canvas, onBackground = c.text,
    surface = c.raised, onSurface = c.text, surfaceVariant = c.control, onSurfaceVariant = c.textSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = c.canvas, surfaceContainerLow = c.raised, surfaceContainer = c.raised,
    surfaceContainerHigh = c.raised, surfaceContainerHighest = c.control,
    inverseSurface = c.inverse, inverseOnSurface = c.onInverse,
    outline = c.lineStrong, outlineVariant = c.line,
    scrim = Color.Black.copy(alpha = 0.6f),
    error = c.negative, onError = c.canvas, errorContainer = c.negativeMuted, onErrorContainer = c.negative
)

/** Corner radii: 4 for small marks, 8 for anything you press or type into, 12 for media and sheets. */
object Radius {
    val S = 4.dp
    val M = 8.dp
    val L = 12.dp
}

/** 4-point spacing scale. [Gutter] is the page margin. */
object Space {
    val XXS = 2.dp
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
    val Gutter = 20.dp
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.S),
    small = RoundedCornerShape(Radius.M),
    medium = RoundedCornerShape(Radius.M),
    large = RoundedCornerShape(Radius.L),
    extraLarge = RoundedCornerShape(Radius.L)
)

private val TwilightScheme = scheme(Twilight)
private val InkScheme = scheme(Ink)

/**
 * Applies [colors] to both the app tokens ([AppTheme.colors]) and Material 3, so M3 sheets,
 * dialogs, inputs, switches and ripples match the palette.
 */
@Composable
private fun PaletteTheme(colors: AppColors, colorScheme: ColorScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

/** Default theme: the calm [Twilight] palette for every front page. The app is dark-only. */
@Composable
fun YogaWellnessTheme(content: @Composable () -> Unit) = PaletteTheme(Twilight, TwilightScheme, content)

/**
 * The original [Ink] palette for pose detection. AppNavHost wraps only the Practice route in it, so
 * Practice, its camera HUD, pose overlay, score explainer, recording UI and every sheet or dialog it
 * opens look exactly as they did before Twilight, without any change to those files.
 *
 * Content colour defaults to Ink's text, as the app Scaffold supplied before Practice was nested in a
 * Twilight scaffold (translucent HUD surfaces and their ripples fall back to it). While Practice is on
 * screen the system bars use Ink's canvas; the colours active before are restored when it leaves.
 */
@Composable
fun DetectionTheme(content: @Composable () -> Unit) {
    DetectionSystemBars()
    PaletteTheme(Ink, InkScheme) {
        CompositionLocalProvider(LocalContentColor provides Ink.text, content = content)
    }
}

/**
 * Sets the status and navigation bars to Ink's canvas (#0E0E0D) on entry, through the same
 * `enableEdgeToEdge` + [SystemBarStyle] set-up as MainActivity, and restores the colours that were
 * active on entry when Practice leaves composition.
 *
 * On Android 15+ (targetSdk 35) the platform ignores bar colours and keeps the bars transparent, so
 * there this is a no-op and the bars show the Practice content behind them, as before.
 */
@Composable
private fun DetectionSystemBars() {
    val activity = LocalContext.current.findComponentActivity() ?: return
    DisposableEffect(activity) {
        val window = activity.window
        @Suppress("DEPRECATION") val previousStatus = window.statusBarColor
        @Suppress("DEPRECATION") val previousNavigation = window.navigationBarColor
        val ink = Ink.canvas.toArgb()
        activity.enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(ink), navigationBarStyle = SystemBarStyle.dark(ink))
        onDispose {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(previousStatus),
                navigationBarStyle = SystemBarStyle.dark(previousNavigation)
            )
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

/** The app's colour tokens: `AppTheme.colors.textSecondary`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
