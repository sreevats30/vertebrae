package com.companion.yogawellness.ui.onboarding

import com.companion.yogawellness.YogaWellnessApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.theme.AppTheme

/** Why the app asks, in one sentence (shown at onboarding and in Settings). */
const val AGE_RANGE_REASON = "We use your age range to suggest suitable poses and timings. It stays on this device."

/**
 * Shown once, after consent and before Home, while no age band is stored. The band is required:
 * Continue stays disabled until one is chosen. Nothing here blocks anything later — the band only
 * shapes suggestions, and it can be changed in Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgeOnboardingScreen(onContinue: (AgeBand) -> Unit) {
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val band = AgeBand.fromStored(chosen)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "A little about you",
                subtitle = "One question, then you're in",
                bottom = {
                    // Wraps at large font sizes instead of clipping the second pill.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("Stays on this device", icon = Icons.Rounded.Memory)
                        GlassPill("Age range only", icon = Icons.Rounded.Lock)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(AGE_RANGE_REASON, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PanelCard(spacing = 14.dp, glow = MaterialTheme.colorScheme.primary) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.Person, MaterialTheme.colorScheme.primary, size = 40.dp)
                    Column {
                        Overline("Choose one")
                        Text("Your age range", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                AgeBandOptions(band, onSelect = { chosen = it.name })
            }
            Callout(
                "We never ask for your birthdate or exact age. This is general wellness guidance, not medical advice — " +
                    "you can still open any pose and change any default.",
                Icons.Rounded.Lock,
                AppTheme.colors.info
            )
            GradientButton(
                "Continue",
                { band?.let(onContinue) },
                Modifier.fillMaxWidth(),
                enabled = band != null,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward
            )
            Text(
                if (band == null) "Choose your age range to continue." else "You can change this any time in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.inkMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * The user's own age band, observed so screens update when it changes in Settings. Null before
 * onboarding, and in previews or tests without the app's [YogaWellnessApp].
 */
@Composable
fun currentAgeBand(): AgeBand? {
    val app = LocalContext.current.applicationContext as? YogaWellnessApp ?: return null
    val band by app.preferences.ageBandState.collectAsStateWithLifecycle()
    return band
}

/** The five bands as a radio group. Shared by onboarding and Settings → Age range. */
@Composable
fun AgeBandOptions(selected: AgeBand?, onSelect: (AgeBand) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AgeBand.entries.forEach { band -> AgeBandRow(band, band == selected) { onSelect(band) } }
    }
}

@Composable
private fun AgeBandRow(band: AgeBand, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            // Comfortably above the 48 dp minimum touch target, and free to grow with large text.
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.08f)))
                else SolidColor(c.well)
            )
            .border(1.dp, if (selected) accent.copy(alpha = 0.70f) else c.wellBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Age range: ${band.label}" }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) accent else c.inkMuted.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        }
        Text(
            band.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            modifier = Modifier.weight(1f)
        )
    }
}
