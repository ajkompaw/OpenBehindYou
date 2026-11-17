package dev.openbehindyou.ui

import android.content.Context
import androidx.annotation.StringRes
import dev.openbehindyou.R
import dev.openbehindyou.detector.DetectionReason
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.detector.ScanMode
import dev.openbehindyou.detector.Sensitivity
import dev.openbehindyou.service.GuardSnapshot

@StringRes
fun levelTitleRes(snapshot: GuardSnapshot): Int = when {
    snapshot.starting -> R.string.guard_starting_title
    !snapshot.running -> R.string.guard_off_title
    snapshot.level == SafetyLevel.ALERT -> R.string.level_alert
    snapshot.level == SafetyLevel.CAUTION -> R.string.level_caution
    else -> R.string.level_safe
}

@StringRes
fun statusDetailRes(snapshot: GuardSnapshot): Int = when {
    snapshot.errorRes != null -> snapshot.errorRes
    snapshot.starting -> R.string.status_detail_starting
    !snapshot.running -> R.string.guard_off_description
    snapshot.level == SafetyLevel.ALERT -> R.string.status_detail_alert
    snapshot.level == SafetyLevel.CAUTION && snapshot.reason == DetectionReason.MOVEMENT_DETECTED ->
        R.string.status_detail_caution_motion

    snapshot.level == SafetyLevel.CAUTION -> R.string.status_detail_caution_secondary
    !snapshot.primaryPresent -> R.string.status_detail_no_face
    else -> R.string.status_detail_safe
}

@StringRes
fun chipLabelRes(snapshot: GuardSnapshot): Int = when {
    snapshot.starting -> R.string.chip_starting
    snapshot.running -> R.string.chip_live
    else -> R.string.chip_off
}

@StringRes
fun sensitivityLabelRes(sensitivity: Sensitivity): Int = when (sensitivity) {
    Sensitivity.LOW -> R.string.sensitivity_low
    Sensitivity.MEDIUM -> R.string.sensitivity_medium
    Sensitivity.HIGH -> R.string.sensitivity_high
}

@StringRes
fun sensitivityDescriptionRes(sensitivity: Sensitivity): Int = when (sensitivity) {
    Sensitivity.LOW -> R.string.sensitivity_low_description
    Sensitivity.MEDIUM -> R.string.sensitivity_medium_description
    Sensitivity.HIGH -> R.string.sensitivity_high_description
}

@StringRes
fun scanModeRes(mode: ScanMode): Int = when (mode) {
    ScanMode.LOW -> R.string.metric_scan_low
    ScanMode.HIGH -> R.string.metric_scan_high
}

fun lastAlertLabel(context: Context, lastAlertAtMs: Long, nowMs: Long): String {
    if (lastAlertAtMs <= 0L) return context.getString(R.string.no_alert_yet)
    val elapsedSeconds = ((nowMs - lastAlertAtMs) / 1_000L).coerceAtLeast(0L)
    val relative = when {
        elapsedSeconds < 15L -> context.getString(R.string.last_alert_now)
        elapsedSeconds < 60L -> context.getString(R.string.last_alert_seconds, elapsedSeconds.toInt())
        else -> context.getString(R.string.last_alert_minutes, (elapsedSeconds / 60L).toInt())
    }
    return context.getString(R.string.last_alert_format, relative)
}
