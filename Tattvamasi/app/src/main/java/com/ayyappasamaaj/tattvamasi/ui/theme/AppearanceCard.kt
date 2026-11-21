package com.ayyappasamaaj.tattvamasi.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayyappasamaaj.tattvamasi.ui.card

/** About us, Contact: System / Light / Dark, as in the website's menu (Appearance). System follows the phone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceCard() {
    val context = LocalContext.current
    Column(Modifier.card(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
            modifier = Modifier.semantics { heading() })
        val choices = Appearance.Choice.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = Appearance.choice == choice,
                    onClick = { Appearance.set(context, choice) },
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AppColors.claySoft,
                        activeContentColor = AppColors.clayDark,
                        inactiveContainerColor = AppColors.card,
                        inactiveContentColor = AppColors.ink,
                    ),
                ) { Text(choice.label) }
            }
        }
        Text("System follows your phone's light or dark setting.", style = MaterialTheme.typography.bodySmall,
            color = AppColors.muted)
    }
}
