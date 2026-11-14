package com.companion.yogawellness.ui.settings

import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.companion.yogawellness.personalisation.RoutineSuggester
import com.companion.yogawellness.personalisation.PracticeReminder
import com.companion.yogawellness.ui.wellness.SupportLineCard
import androidx.compose.material.icons.rounded.Edit
import com.companion.yogawellness.ui.onboarding.AgeBandOptions
import com.companion.yogawellness.ui.onboarding.AGE_RANGE_REASON
import com.companion.yogawellness.personalisation.AgeBand
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.BuildConfig
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.FeatureRow
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.components.Reveal
import com.companion.yogawellness.ui.components.SoftButton
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.Persona

@Composable
fun SettingsScreen(onBack: () -> Unit, onDataDeleted: () -> Unit, vm: WellnessViewModel = viewModel()) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPersona by remember { mutableStateOf<Persona?>(null) }
    var confirmRemoveDemo by remember { mutableStateOf(false) }
    val viewing by vm.activeProfileId.collectAsStateWithLifecycle()
    val ageBand by vm.ageBand.collectAsStateWithLifecycle()
    var editAge by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Settings & privacy",
                subtitle = "Your data, your control",
                onBack = onBack,
                bottom = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("No internet", icon = Icons.Rounded.CloudOff)
                        GlassPill("On-device only", icon = Icons.Rounded.Memory)
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
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Reveal(0) { PrivacyCard() }
            Reveal(1) { AgeRangeCard(ageBand, onChange = { editAge = true }) }
            Reveal(2) { ReminderCard(vm) }
            Reveal(3) { SupportLineCard() }
            Reveal(4) {
                PanelCard(spacing = 12.dp, elevation = 6.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.Science, Brand.Mood, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Overline("Explore", color = AppTheme.colors.inkMuted)
                            Text("Demo data", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(
                        "Load 42 days of synthetic data to explore the dashboard and history. " +
                            "Adds synthetic data you can browse. Your own data is not changed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Pill(
                        "Viewing: ${ProfileIds.displayName(viewing)}",
                        icon = if (ProfileIds.isDemo(viewing)) Icons.Rounded.Science else Icons.Rounded.Person,
                        color = if (ProfileIds.isDemo(viewing)) AppTheme.colors.warning else MaterialTheme.colorScheme.primary
                    )
                    Persona.entries.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = AppTheme.colors.hairline)
                        PersonaRow(p, loaded = viewing == p.profileId) { confirmPersona = p }
                    }
                    SoftButton(
                        "Remove demo data", { confirmRemoveDemo = true }, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.DeleteSweep, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Reveal(5) {
                PanelCard(
                    spacing = 12.dp,
                    borderColor = AppTheme.colors.danger.copy(alpha = 0.35f),
                    elevation = 6.dp,
                    glow = AppTheme.colors.danger
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.WarningAmber, AppTheme.colors.danger, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Overline("Danger zone", color = AppTheme.colors.danger)
                            Text("Delete All My Data", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(
                        "Erases check-ins, questionnaire answers, practice history, recorded pose clips, exported reports and settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GradientButton(
                        "Delete All My Data",
                        { confirmDelete = true },
                        Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.DeleteForever,
                        brush = Brush.horizontalGradient(listOf(AppTheme.colors.danger, Color(0xFFF04438)))
                    )
                }
            }
            Reveal(6) { AboutCard() }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (editAge) {
        AgeRangeDialog(current = ageBand, onSave = { vm.setAgeBand(it); editAge = false }, onDismiss = { editAge = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = AppTheme.colors.danger) },
            title = { Text("Delete everything?") },
            text = { Text("This permanently erases all data stored by this app on this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAllData(onDataDeleted) }) {
                    Text("Delete", color = AppTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    confirmPersona?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmPersona = null },
            icon = { Icon(Icons.Rounded.Science, contentDescription = null, tint = Brand.Mood) },
            title = { Text("Load ${p.label}?") },
            text = { Text(p.description + "\n\nYour own data is not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmPersona = null
                    vm.loadPersona(p) {}
                }) { Text("Load") }
            },
            dismissButton = { TextButton(onClick = { confirmPersona = null }) { Text("Cancel") } }
        )
    }
    if (confirmRemoveDemo) {
        AlertDialog(
            onDismissRequest = { confirmRemoveDemo = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Remove demo data?") },
            text = { Text("Removes the three synthetic personas. Your own data is not affected.") },
            confirmButton = {
                TextButton(onClick = { confirmRemoveDemo = false; vm.removeDemoData {} }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveDemo = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Optional daily reminder. Off by default; switching it on asks for the notification permission on
 * Android 13+ and stays off if that is refused. The hour starts at the user's usual practice time.
 */
@Composable
private fun ReminderCard(vm: WellnessViewModel) {
    val context = LocalContext.current
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val usual by vm.usualPracticeHour.collectAsStateWithLifecycle()
    val (enabled, hour) = reminder
    var refused by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refused = !granted
        if (granted) vm.setReminder(true, hour)
    }
    PanelCard(spacing = 12.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Alarm, MaterialTheme.colorScheme.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Overline("Optional", color = AppTheme.colors.inkMuted)
                Text("Daily reminder", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    when {
                        !on -> vm.setReminder(false, hour)
                        PracticeReminder.canNotify(context) -> vm.setReminder(true, if (!enabled && usual != null) usual!! else hour)
                        else -> askPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Daily practice reminder" }
            )
        }
        Text(
            "A notification on this phone at ${RoutineSuggester.formatHour(hour)} each day. It is created on the device — " +
                "nothing is sent anywhere." + (usual?.let { " You usually practise around ${RoutineSuggester.formatHour(it)}." } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (refused) {
            Text(
                "Notifications are turned off for this app, so the reminder stays off. You can allow them in Android settings.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.warning
            )
        }
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("Earlier", { vm.setReminder(true, (hour + 23) % 24) }, Modifier.weight(1f), icon = Icons.Rounded.Remove)
                Text(RoutineSuggester.formatHour(hour), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                SoftButton("Later", { vm.setReminder(true, (hour + 1) % 24) }, Modifier.weight(1f), icon = Icons.Rounded.Add)
            }
        }
    }
}

/** Settings → Age range: the band chosen at onboarding, with a way to change it. */
@Composable
private fun AgeRangeCard(band: AgeBand?, onChange: () -> Unit) {
    PanelCard(spacing = 12.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Person, MaterialTheme.colorScheme.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Overline("Personalisation", color = AppTheme.colors.inkMuted)
                Text("Age range", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Pill(band?.label ?: "Not set", color = if (band != null) MaterialTheme.colorScheme.primary else AppTheme.colors.warning)
        }
        Text(AGE_RANGE_REASON, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SoftButton("Change age range", onChange, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
    }
}

@Composable
private fun AgeRangeDialog(current: AgeBand?, onSave: (AgeBand) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Your age range") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Suggestions and gentler defaults follow this. Only the range is stored, on this phone.")
                AgeBandOptions(chosen, onSelect = { chosen = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { chosen?.let(onSave) }, enabled = chosen != null && chosen != current) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PrivacyCard() {
    val c = AppTheme.colors
    PanelCard(
        color = c.heroStart,
        brush = c.featureBrush,
        borderColor = null,
        spacing = 14.dp,
        glow = Brand.Mint
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Brand.Mint, Brand.Teal500)))
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.05f))), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = Brand.Navy900, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Overline("Privacy", color = Brand.Mint)
                Text("Private by design", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }
        Text(NON_DIAGNOSTIC_STATEMENT, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        Text(
            "Everything is processed and stored on this phone. The app has no internet permission and is excluded from backups.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivacyCheck("Camera frames analysed in memory, never saved")
            PrivacyCheck("No network access — nothing leaves this phone")
            PrivacyCheck("Excluded from cloud backups")
        }
    }
}

@Composable
private fun PrivacyCheck(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun PersonaRow(p: Persona, loaded: Boolean, onClick: () -> Unit) {
    val letter = p.name.first().toString()
    val tint = when (p) {
        Persona.A_CONSISTENT -> AppTheme.colors.success
        Persona.B_EXAM_GAP -> Brand.Stress
        Persona.C_NULL -> AppTheme.colors.info
    }
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.08f))))
                    .border(1.dp, tint.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, style = MaterialTheme.typography.titleMedium, color = tint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(p.description, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
            }
            if (loaded) Pill("Viewing", icon = Icons.Rounded.CheckCircle, color = AppTheme.colors.success)
            else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AppTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun AboutCard() {
    PanelCard(spacing = 14.dp, elevation = 0.dp, color = AppTheme.colors.well, borderColor = AppTheme.colors.wellBorder) {
        Overline("About", color = AppTheme.colors.inkMuted)
        FeatureRow(
            Icons.Rounded.Info,
            "Yoga Wellness Companion v${BuildConfig.VERSION_NAME} · pose estimation by MediaPipe, running fully on-device.",
            MaterialTheme.colorScheme.primary
        )
        FeatureRow(
            Icons.Rounded.VisibilityOff,
            "PHQ-9 and GAD-7 are shown as non-diagnostic self-reflection scores — never as clinical labels.",
            Brand.Mood
        )
        FeatureRow(
            Icons.Rounded.TextFields,
            "Typefaces: Inter and Plus Jakarta Sans (SIL Open Font License).",
            AppTheme.colors.inkMuted
        )
    }
}
