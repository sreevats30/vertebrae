package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.SoftButton
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.wellness.CrisisLines

/**
 * Tele-MANAS, always reachable without a questionnaire (Settings for everyone; Insights for the
 * under-18 flow, where the PHQ-9 / GAD-7 crisis path is not offered). Opens the dialler only.
 */
@Composable
fun SupportLineCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PanelCard(modifier, spacing = 12.dp, elevation = 6.dp, glow = Brand.Mind) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.SupportAgent, Brand.Mind, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Overline("Support", color = AppTheme.colors.inkMuted)
                Text("Someone to talk to", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            "India's Tele-MANAS helpline is free and confidential, any time: ${CrisisLines.TELE_MANAS_SHORT} or " +
                "${CrisisLines.TELE_MANAS_TOLL_FREE}. In an emergency, call ${CrisisLines.EMERGENCY}. " +
                "This app is not intended to replace a doctor or counsellor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SoftButton(
            "Call Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}", { dial(context, CrisisLines.TELE_MANAS_SHORT) },
            Modifier.fillMaxWidth(), icon = Icons.Rounded.Call, color = Brand.Mind
        )
    }
}
