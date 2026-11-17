package dev.openbehindyou.detector

import kotlin.math.max

/**
 * Stabilizes noisy per-frame assessments:
 *  - ALERT must be seen for N consecutive frames (or a stable tracking id).
 *  - CAUTION and ALERT have hold windows so a single dropped frame does not
 *    immediately clear a warning.
 */
class ThreatSmoother {
    private var alertStreak = 0
    private var alertTrackingId: Int? = null
    private var alertUntilMs = 0L
    private var cautionUntilMs = 0L
    private var lastAlertReason = DetectionReason.NONE
    private var lastCautionReason = DetectionReason.NONE

    fun update(
        assessment: FaceAssessment,
        nowMs: Long,
        config: DetectionConfig,
    ): SmoothedAssessment {
        if (assessment.level == SafetyLevel.ALERT) {
            cautionUntilMs = max(cautionUntilMs, nowMs + config.cautionHoldMs)

            val id = assessment.candidateTrackingId
            alertStreak = when {
                alertStreak == 0 -> 1
                id == null || alertTrackingId == null -> alertStreak + 1
                id == alertTrackingId -> alertStreak + 1
                else -> 1
            }
            alertTrackingId = id

            if (alertStreak >= config.alertConfirmations) {
                alertUntilMs = max(alertUntilMs, nowMs + config.alertHoldMs)
                lastAlertReason = assessment.reason
            }
        } else {
            alertStreak = 0
            alertTrackingId = null
        }

        if (assessment.level == SafetyLevel.CAUTION) {
            cautionUntilMs = max(cautionUntilMs, nowMs + config.cautionHoldMs)
            lastCautionReason = assessment.reason
        }

        val level = when {
            nowMs < alertUntilMs -> SafetyLevel.ALERT
            nowMs < cautionUntilMs -> SafetyLevel.CAUTION
            else -> SafetyLevel.SAFE
        }

        val reason = when (level) {
            SafetyLevel.ALERT -> when {
                assessment.level == SafetyLevel.ALERT && assessment.reason != DetectionReason.NONE ->
                    assessment.reason
                lastAlertReason != DetectionReason.NONE -> lastAlertReason
                else -> DetectionReason.LOOKING_OVER_SHOULDER
            }

            SafetyLevel.CAUTION -> when {
                assessment.level == SafetyLevel.CAUTION && assessment.reason != DetectionReason.NONE ->
                    assessment.reason
                lastCautionReason != DetectionReason.NONE -> lastCautionReason
                else -> DetectionReason.SECONDARY_FACE_NEARBY
            }

            SafetyLevel.SAFE -> if (!assessment.primaryPresent) {
                DetectionReason.NO_USER
            } else {
                DetectionReason.NONE
            }
        }

        return SmoothedAssessment(
            level = level,
            reason = reason,
            primaryPresent = assessment.primaryPresent,
            totalFaces = assessment.totalFaces,
            nearbyFaces = assessment.nearbyFaces,
        )
    }

    fun reset() {
        alertStreak = 0
        alertTrackingId = null
        alertUntilMs = 0L
        cautionUntilMs = 0L
        lastAlertReason = DetectionReason.NONE
        lastCautionReason = DetectionReason.NONE
    }
}
