package dev.openbehindyou.camera

import dev.openbehindyou.detector.DetectionConfig
import dev.openbehindyou.detector.FaceAssessment
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.detector.ScanMode
import kotlin.math.max

/**
 * Duty-cycle controller for the ML Kit analyzer.
 *
 * Low mode runs at roughly 1.4–1.9 FPS. Motion, a nearby face, or an unconfirmed
 * alert candidate opens a short high-frequency window (roughly 7–11 FPS).
 */
class AdaptiveScanController {
    private var highUntilMs = 0L

    fun boost(nowMs: Long, durationMs: Long) {
        highUntilMs = max(highUntilMs, nowMs + durationMs)
    }

    fun onAssessment(assessment: FaceAssessment, nowMs: Long, config: DetectionConfig) {
        if (assessment.level != SafetyLevel.SAFE || assessment.nearbyFaces > 0) {
            boost(nowMs, config.highBoostDurationMs)
        }
    }

    fun isHigh(nowMs: Long): Boolean = nowMs < highUntilMs

    fun mode(nowMs: Long): ScanMode = if (isHigh(nowMs)) ScanMode.HIGH else ScanMode.LOW

    fun intervalMs(nowMs: Long, config: DetectionConfig): Long =
        if (isHigh(nowMs)) config.highScanIntervalMs else config.lowScanIntervalMs

    fun reset() {
        highUntilMs = 0L
    }
}
