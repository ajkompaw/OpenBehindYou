package dev.openbehindyou.service

import android.os.SystemClock
import androidx.annotation.StringRes
import dev.openbehindyou.detector.DetectionReason
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.detector.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dev.openbehindyou.detector.DetectionReport

/** UI-facing immutable state shared by the foreground service and Compose. */
data class GuardSnapshot(
    val running: Boolean = false,
    val starting: Boolean = false,
    val level: SafetyLevel = SafetyLevel.SAFE,
    val reason: DetectionReason = DetectionReason.NONE,
    val primaryPresent: Boolean = false,
    val totalFaces: Int = 0,
    val nearbyFaces: Int = 0,
    val effectiveFps: Float = 0f,
    val scanMode: ScanMode = ScanMode.LOW,
    val lastUpdatedAtMs: Long = 0L,
    val lastAlertAtMs: Long = 0L,
    @StringRes val errorRes: Int? = null,
) {
    val active: Boolean get() = running || starting
}

/**
 * In-process bridge between the service and Compose. A process restart also
 * destroys the service, so no persisted copy is needed (or wanted).
 */
object GuardRuntime {
    private val _snapshot = MutableStateFlow(GuardSnapshot())
    val snapshot: StateFlow<GuardSnapshot> = _snapshot.asStateFlow()

    fun markStarting() {
        _snapshot.value = GuardSnapshot(
            starting = true,
            lastUpdatedAtMs = SystemClock.elapsedRealtime(),
        )
    }

    fun markRunning() {
        _snapshot.update { current ->
            current.copy(
                running = true,
                starting = false,
                errorRes = null,
                lastUpdatedAtMs = SystemClock.elapsedRealtime(),
            )
        }
    }

    fun updateDetection(report: DetectionReport) {
        _snapshot.update { current ->
            current.copy(
                running = true,
                starting = false,
                level = report.level,
                reason = report.reason,
                primaryPresent = report.primaryPresent,
                totalFaces = report.totalFaces,
                nearbyFaces = report.nearbyFaces,
                effectiveFps = report.effectiveFps,
                scanMode = report.scanMode,
                lastUpdatedAtMs = report.timestampMs,
                errorRes = null,
            )
        }
    }

    fun noteAlert(atEpochMs: Long = System.currentTimeMillis()) {
        _snapshot.update { it.copy(lastAlertAtMs = atEpochMs) }
    }

    fun setError(@StringRes errorRes: Int) {
        _snapshot.update {
            it.copy(
                errorRes = errorRes,
                lastUpdatedAtMs = SystemClock.elapsedRealtime(),
            )
        }
    }

    fun markStopped(@StringRes errorRes: Int? = null) {
        _snapshot.update {
            it.copy(
                running = false,
                starting = false,
                level = SafetyLevel.SAFE,
                reason = DetectionReason.NONE,
                primaryPresent = false,
                totalFaces = 0,
                nearbyFaces = 0,
                effectiveFps = 0f,
                scanMode = ScanMode.LOW,
                errorRes = errorRes,
                lastUpdatedAtMs = SystemClock.elapsedRealtime(),
            )
        }
    }
}
