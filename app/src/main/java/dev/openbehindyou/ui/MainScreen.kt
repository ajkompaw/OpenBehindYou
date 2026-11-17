package dev.openbehindyou.ui

import android.app.NotificationManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.openbehindyou.R
import dev.openbehindyou.detector.Sensitivity
import dev.openbehindyou.service.GuardNotifications
import dev.openbehindyou.service.GuardSnapshot
import dev.openbehindyou.ui.components.StatusChip
import dev.openbehindyou.ui.theme.GuardBackground
import dev.openbehindyou.ui.theme.GuardSurface
import dev.openbehindyou.ui.theme.GuardTextMuted
import dev.openbehindyou.ui.theme.safetyColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snapshot by viewModel.guard.collectAsStateWithLifecycle()
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var permissionWarningRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var permissionRefresh by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionRefresh++
    }

    val cameraGranted = remember(permissionRefresh) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    val notificationsEnabled = remember(permissionRefresh) {
        areNotificationsEnabled(context)
    }

    LaunchedEffect(cameraGranted, notificationsEnabled) {
        if (cameraGranted && notificationsEnabled) {
            permissionWarningRes = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRefresh++
        if (!hasCameraPermission(context)) {
            permissionWarningRes = R.string.permission_camera_required
            return@rememberLauncherForActivityResult
        }
        permissionWarningRes = if (areNotificationsEnabled(context)) {
            null
        } else {
            R.string.permission_notifications_limited
        }
        viewModel.startGuard()
    }

    val toggleGuard: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            viewModel.stopGuard()
            permissionWarningRes = null
        } else {
            val missing = buildList {
                if (!hasCameraPermission(context)) add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (missing.isEmpty()) {
                permissionWarningRes = if (areNotificationsEnabled(context)) {
                    null
                } else {
                    R.string.permission_notifications_limited
                }
                viewModel.startGuard()
            } else {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    val openSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuardBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GuardBackground),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stat_shield),
                                contentDescription = stringResource(R.string.cd_privacy_shield),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(21.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    if (snapshot.active) {
                        val chipColor = if (snapshot.starting) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            safetyColor(snapshot.level)
                        }
                        StatusChip(
                            text = stringResource(chipLabelRes(snapshot)),
                            color = chipColor,
                            modifier = Modifier.padding(end = 14.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = GuardSurface,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_stat_shield),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.tab_guard)) },
                    colors = guardNavColors(),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = guardNavColors(),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GuardBackground),
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    snapshot = snapshot,
                    sensitivity = sensitivity,
                    cameraGranted = cameraGranted,
                    notificationsEnabled = notificationsEnabled,
                    permissionWarningRes = permissionWarningRes,
                    onToggleGuard = toggleGuard,
                    onOpenSettings = openSettings,
                )

                else -> SettingsScreen(
                    sensitivity = sensitivity,
                    onSensitivitySelected = viewModel::setSensitivity,
                )
            }
        }
    }
}

@Composable
private fun guardNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    unselectedIconColor = GuardTextMuted,
    unselectedTextColor = GuardTextMuted,
)

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun areNotificationsEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        return false
    }

    // The app-level switch can be on while either of our channels is blocked
    // or the alert channel was downgraded below heads-up importance.
    val manager = context.getSystemService(NotificationManager::class.java) ?: return true
    val statusChannel = manager.getNotificationChannel(GuardNotifications.CHANNEL_STATUS)
    if (statusChannel?.importance == NotificationManager.IMPORTANCE_NONE) return false
    val alertChannel = manager.getNotificationChannel(GuardNotifications.CHANNEL_ALERTS)
    if (alertChannel != null && alertChannel.importance < NotificationManager.IMPORTANCE_HIGH) {
        return false
    }
    return true
}
