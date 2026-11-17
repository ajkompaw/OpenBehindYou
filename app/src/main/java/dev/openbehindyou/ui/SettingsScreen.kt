package dev.openbehindyou.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.openbehindyou.R
import dev.openbehindyou.detector.Sensitivity
import dev.openbehindyou.ui.components.GuardCard
import dev.openbehindyou.ui.theme.GuardOutline
import dev.openbehindyou.ui.theme.GuardSurface

@Composable
fun SettingsScreen(
    sensitivity: Sensitivity,
    onSensitivitySelected: (Sensitivity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.sensitivity_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sensitivity_applies_immediately),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sensitivity.entries.forEach { option ->
                        val selected = option == sensitivity
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onSensitivitySelected(option) },
                                    role = Role.RadioButton,
                                ),
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                GuardSurface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                } else {
                                    GuardOutline.copy(alpha = 0.40f)
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 4.dp, end = 10.dp),
                                ) {
                                    Text(
                                        text = stringResource(sensitivityLabelRes(option)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = stringResource(sensitivityDescriptionRes(option)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_saved_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.adaptive_scan_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.adaptive_scan_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(14.dp))
                PrivacyItem(
                    title = stringResource(R.string.privacy_local_title),
                    body = stringResource(R.string.privacy_local_description),
                )
                PrivacyDivider()
                PrivacyItem(
                    title = stringResource(R.string.privacy_no_network_title),
                    body = stringResource(R.string.privacy_no_network_description),
                )
                PrivacyDivider()
                PrivacyItem(
                    title = stringResource(R.string.privacy_no_overlay_title),
                    body = stringResource(R.string.privacy_no_overlay_description),
                )
                PrivacyDivider()
                PrivacyItem(
                    title = stringResource(R.string.privacy_camera_title),
                    body = stringResource(R.string.privacy_camera_description),
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.battery_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.battery_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.limitations_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.limitations_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_license),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PrivacyItem(
    title: String,
    body: String,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyDivider() {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = GuardOutline.copy(alpha = 0.45f))
    Spacer(Modifier.height(12.dp))
}
