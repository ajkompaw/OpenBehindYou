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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.openbehindyou.R
import dev.openbehindyou.detector.Sensitivity
import dev.openbehindyou.service.GuardSnapshot
import dev.openbehindyou.ui.components.ErrorNoticeCard
import dev.openbehindyou.ui.components.GuardCard
import dev.openbehindyou.ui.components.MetricCard
import dev.openbehindyou.ui.components.NumberedStep
import dev.openbehindyou.ui.components.PermissionNoticeCard
import dev.openbehindyou.ui.components.StatusChip
import dev.openbehindyou.ui.components.StatusOrb
import dev.openbehindyou.ui.theme.GuardOutline
import dev.openbehindyou.ui.theme.GuardTextMuted
import dev.openbehindyou.ui.theme.safetyColor
import kotlinx.coroutines.delay

private data class PermissionNotice(
    val titleRes: Int,
    val messageRes: Int,
)

@Composable
fun DashboardScreen(
    snapshot: GuardSnapshot,
    sensitivity: Sensitivity,
    cameraGranted: Boolean,
    notificationsEnabled: Boolean,
    permissionWarningRes: Int?,
    onToggleGuard: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(snapshot.lastAlertAtMs) {
        if (snapshot.lastAlertAtMs > 0L) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(15_000L)
            }
        }
    }

    val statusColor = when {
        !snapshot.active -> GuardTextMuted
        snapshot.starting -> MaterialTheme.colorScheme.secondary
        else -> safetyColor(snapshot.level)
    }
    val title = stringResource(levelTitleRes(snapshot))
    val detail = stringResource(statusDetailRes(snapshot))
    val lastAlert = remember(snapshot.lastAlertAtMs, nowMs) {
        lastAlertLabel(context, snapshot.lastAlertAtMs, nowMs)
    }
    val notices = buildPermissionNotices(
        cameraGranted = cameraGranted,
        notificationsEnabled = notificationsEnabled,
        snapshot = snapshot,
        permissionWarningRes = permissionWarningRes,
    )
    val guardSwitchDescription = stringResource(R.string.cd_guard_switch)

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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = statusColor.copy(alpha = if (snapshot.active) 0.11f else 0.06f),
                border = BorderStroke(
                    1.dp,
                    statusColor.copy(alpha = if (snapshot.active) 0.40f else 0.20f),
                ),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusOrb(
                            color = statusColor,
                            label = stringResource(R.string.cd_status_indicator, title),
                            active = snapshot.active,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        StatusChip(
                            text = stringResource(chipLabelRes(snapshot)),
                            color = statusColor,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = GuardOutline.copy(alpha = 0.55f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (snapshot.active) {
                                stringResource(R.string.guard_active_across_apps)
                            } else {
                                stringResource(R.string.privacy_strip)
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = GuardTextMuted,
                        )
                        if (snapshot.active || snapshot.lastAlertAtMs > 0L) {
                            Text(
                                text = lastAlert,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (snapshot.lastAlertAtMs > 0L) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    GuardTextMuted
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            GuardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.guard_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (snapshot.active) {
                                stringResource(R.string.guard_subtitle_on)
                            } else {
                                stringResource(R.string.guard_subtitle_off)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = snapshot.active,
                        onCheckedChange = onToggleGuard,
                        modifier = Modifier.semantics {
                            contentDescription = guardSwitchDescription
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                if (snapshot.starting) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.sensitivity_current,
                            stringResource(sensitivityLabelRes(sensitivity)),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = GuardTextMuted,
                    )
                    if (snapshot.active) {
                        Text(
                            text = stringResource(R.string.guard_active_across_apps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (notices.isNotEmpty()) {
            items(notices, key = { it.messageRes }) { notice ->
                PermissionNoticeCard(
                    title = stringResource(notice.titleRes),
                    message = stringResource(notice.messageRes),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = onOpenSettings,
                )
            }
        }

        if (snapshot.errorRes != null) {
            item {
                ErrorNoticeCard(message = stringResource(snapshot.errorRes))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val unavailable = stringResource(R.string.value_unavailable)
                MetricCard(
                    label = stringResource(R.string.metric_faces),
                    value = if (snapshot.active) {
                        stringResource(R.string.metric_faces_value, snapshot.totalFaces)
                    } else {
                        unavailable
                    },
                    supporting = if (snapshot.active && snapshot.nearbyFaces > 0) {
                        pluralStringResource(
                            R.plurals.metric_nearby,
                            snapshot.nearbyFaces,
                            snapshot.nearbyFaces,
                        )
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.metric_fps),
                    value = if (snapshot.active && snapshot.effectiveFps > 0f) {
                        stringResource(R.string.metric_fps_value, snapshot.effectiveFps)
                    } else {
                        unavailable
                    },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.metric_scan),
                    value = if (snapshot.active) {
                        stringResource(scanModeRes(snapshot.scanMode))
                    } else {
                        unavailable
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stat_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.privacy_strip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            GuardCard {
                Text(
                    text = stringResource(R.string.how_it_works_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(14.dp))
                NumberedStep(1, stringResource(R.string.how_it_works_1))
                Spacer(Modifier.height(10.dp))
                NumberedStep(2, stringResource(R.string.how_it_works_2))
                Spacer(Modifier.height(10.dp))
                NumberedStep(3, stringResource(R.string.how_it_works_3))
            }
        }
    }
}

private fun buildPermissionNotices(
    cameraGranted: Boolean,
    notificationsEnabled: Boolean,
    snapshot: GuardSnapshot,
    permissionWarningRes: Int?,
): List<PermissionNotice> {
    val notices = mutableListOf<PermissionNotice>()
    if (!cameraGranted) {
        notices += PermissionNotice(
            titleRes = R.string.permission_needed_title,
            messageRes = R.string.permission_camera_required,
        )
    }
    if (!notificationsEnabled && (snapshot.active || permissionWarningRes != null)) {
        notices += PermissionNotice(
            titleRes = R.string.notification_permission_needed_title,
            messageRes = R.string.permission_notifications_limited,
        )
    }
    if (permissionWarningRes != null &&
        notices.none { it.messageRes == permissionWarningRes }
    ) {
        notices += PermissionNotice(
            titleRes = R.string.permission_needed_title,
            messageRes = permissionWarningRes,
        )
    }
    return notices.distinctBy { it.messageRes }
}
