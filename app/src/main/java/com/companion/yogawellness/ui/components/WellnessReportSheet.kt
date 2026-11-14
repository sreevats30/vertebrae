package com.companion.yogawellness.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.motor.EmotionEstimate
import com.companion.yogawellness.motor.MentalWellnessReport
import com.companion.yogawellness.motor.MindState
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle

/** One colour per emotional dimension, consistent across chip, sheet and History. */
object MindColors {
    val Calm = Brand.Teal400
    val Stress = Brand.Stress
    val Fatigue = Brand.Sleep
    val Focus = Brand.Mood

    fun forState(s: MindState): Color = when (s) {
        MindState.CALM_FOCUSED, MindState.SETTLING -> Calm
        MindState.STRESS -> Stress
        MindState.FATIGUE -> Fatigue
        MindState.DISTRACTED -> Focus
    }
}

// ---------------------------------------------------------------------------------------------
// Surface 1: live Mind-State chip (camera HUD)
// ---------------------------------------------------------------------------------------------

/** Floating navy-glass pill: "Stress Detected (74%) • Elevated Tremor". */
@Composable
fun MindStateChip(estimate: EmotionEstimate, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(MindColors.forState(estimate.state), label = "mindTint")
    Row(
        modifier
            .clip(CircleShape)
            .background(Color(0xBF07111F))
            .border(1.dp, tint.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Mind state: ${estimate.chipText}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Text(
            "${estimate.state.label} ${estimate.headlineScore}%",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.45f)))
        Text(estimate.cue, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

// ---------------------------------------------------------------------------------------------
// Surface 2: Mental Wellness Report sheet (post-session and from History)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessReportSheet(report: MentalWellnessReport, onDismiss: () -> Unit, subtitle: String? = null) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        WellnessReportContent(report, subtitle, onDone = onDismiss)
    }
}

@Composable
fun WellnessReportContent(report: MentalWellnessReport, subtitle: String? = null, onDone: (() -> Unit)? = null) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero
        PanelCard(color = c.heroStart, brush = c.featureBrush, borderColor = null, glow = MindColors.forState(report.state), spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Brush.linearGradient(listOf(Brand.Mint, Brand.Teal500)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Brand.Navy900, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Overline("Mental wellness & motor report", color = Brand.Mint)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f)) }
                }
            }
            Text(report.headline, style = MaterialTheme.typography.titleLarge, color = Color.White)
            report.trendLine?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.TrendingDown, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(16.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
            Text(
                if (report.estimated) "Estimated from this session's saved stability, hold and alignment metrics."
                else "Read from your movement: micro-tremor, postural sway, transition smoothness and hold stability.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        // Four scores with their motor evidence
        ScoreRow("Calm", report.calm, report.calmReason, MindColors.Calm, Icons.Rounded.Spa)
        ScoreRow("Stress", report.stress, report.stressReason, MindColors.Stress, Icons.Rounded.Bolt)
        ScoreRow("Fatigue", report.fatigue, report.fatigueReason, MindColors.Fatigue, Icons.Rounded.Bedtime)
        ScoreRow("Focus", report.focus, report.focusReason, MindColors.Focus, Icons.Rounded.CenterFocusStrong)

        // Suggestions
        if (report.suggestions.isNotEmpty()) {
            PanelCard(spacing = 10.dp) {
                Overline("Suggested next poses")
                report.suggestions.forEach { s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        IconBadge(Icons.Rounded.SelfImprovement, tint = MaterialTheme.colorScheme.primary, size = 34.dp)
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(s.why, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                        }
                    }
                }
            }
        }

        Text(
            "Wellness insight from movement patterns, not a medical diagnosis.",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkMuted
        )
        onDone?.let {
            Spacer(Modifier.height(2.dp))
            GradientButton("Continue", it, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: Int, reason: String, color: Color, icon: ImageVector) {
    PanelCard(spacing = 8.dp, elevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, tint = color, size = 34.dp)
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("$value%", style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = color)
        }
        ProgressTrack(progress = value / 100f, color = color)
        Text(reason, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
    }
}

// ---------------------------------------------------------------------------------------------
// Surface 3: compact badge row for History cards
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessBadgeRow(report: MentalWellnessReport, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniPill("Calm", report.calm, MindColors.Calm)
        MiniPill("Stress", report.stress, MindColors.Stress)
        MiniPill("Fatigue", report.fatigue, MindColors.Fatigue)
        MiniPill("Focus", report.focus, MindColors.Focus)
    }
}

@Composable
private fun MiniPill(label: String, value: Int, color: Color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text("$label $value%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
