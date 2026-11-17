package dev.openbehindyou.detector

/**
 * Heuristic thresholds. All distances are expressed relative to the primary
 * face, so the rules are independent of camera resolution and screen size.
 */
data class DetectionConfig(
    val minPrimaryFaceAreaFraction: Float,
    val minSecondaryToPrimaryAreaRatio: Float,
    val alertSecondaryToPrimaryAreaRatio: Float,
    val maxNearbySideOffsetInPrimaryWidths: Float,
    val maxNearbyVerticalOffsetInPrimaryHeights: Float,
    val maxNearbyBelowPrimaryInPrimaryHeights: Float,
    val maxAlertSideOffsetInPrimaryWidths: Float,
    val maxAlertVerticalOffsetInPrimaryHeights: Float,
    val maxAlertCenterBelowPrimaryInPrimaryHeights: Float,
    val maxAlertYawDegrees: Float,
    val maxAlertPitchDegrees: Float,
    val alertConfirmations: Int,
    val cautionHoldMs: Long,
    val alertHoldMs: Long,
    val lowScanIntervalMs: Long,
    val highScanIntervalMs: Long,
    val highBoostDurationMs: Long,
    val motionSampleIntervalMs: Long,
    val motionThreshold: Float,
)

/**
 * User-selectable presets. Higher sensitivity means smaller/farther secondary
 * faces and looser head-pose limits, which is more protective but noisier.
 */
enum class Sensitivity {
    LOW,
    MEDIUM,
    HIGH;

    fun toConfig(): DetectionConfig = when (this) {
        LOW -> DetectionConfig(
            minPrimaryFaceAreaFraction = 0.035f,
            minSecondaryToPrimaryAreaRatio = 0.30f,
            alertSecondaryToPrimaryAreaRatio = 0.45f,
            maxNearbySideOffsetInPrimaryWidths = 1.05f,
            maxNearbyVerticalOffsetInPrimaryHeights = 1.05f,
            maxNearbyBelowPrimaryInPrimaryHeights = 0.30f,
            maxAlertSideOffsetInPrimaryWidths = 0.85f,
            maxAlertVerticalOffsetInPrimaryHeights = 0.75f,
            maxAlertCenterBelowPrimaryInPrimaryHeights = 0.22f,
            maxAlertYawDegrees = 20f,
            maxAlertPitchDegrees = 25f,
            alertConfirmations = 4,
            cautionHoldMs = 1_800L,
            alertHoldMs = 3_200L,
            lowScanIntervalMs = 700L,
            highScanIntervalMs = 140L,
            highBoostDurationMs = 3_500L,
            motionSampleIntervalMs = 240L,
            motionThreshold = 0.070f,
        )

        MEDIUM -> DetectionConfig(
            minPrimaryFaceAreaFraction = 0.025f,
            minSecondaryToPrimaryAreaRatio = 0.22f,
            alertSecondaryToPrimaryAreaRatio = 0.34f,
            maxNearbySideOffsetInPrimaryWidths = 1.25f,
            maxNearbyVerticalOffsetInPrimaryHeights = 1.20f,
            maxNearbyBelowPrimaryInPrimaryHeights = 0.35f,
            maxAlertSideOffsetInPrimaryWidths = 1.00f,
            maxAlertVerticalOffsetInPrimaryHeights = 0.85f,
            maxAlertCenterBelowPrimaryInPrimaryHeights = 0.28f,
            maxAlertYawDegrees = 28f,
            maxAlertPitchDegrees = 32f,
            alertConfirmations = 3,
            cautionHoldMs = 1_600L,
            alertHoldMs = 2_800L,
            lowScanIntervalMs = 620L,
            highScanIntervalMs = 110L,
            highBoostDurationMs = 4_000L,
            motionSampleIntervalMs = 200L,
            motionThreshold = 0.055f,
        )

        HIGH -> DetectionConfig(
            minPrimaryFaceAreaFraction = 0.018f,
            minSecondaryToPrimaryAreaRatio = 0.15f,
            alertSecondaryToPrimaryAreaRatio = 0.25f,
            maxNearbySideOffsetInPrimaryWidths = 1.45f,
            maxNearbyVerticalOffsetInPrimaryHeights = 1.35f,
            maxNearbyBelowPrimaryInPrimaryHeights = 0.40f,
            maxAlertSideOffsetInPrimaryWidths = 1.20f,
            maxAlertVerticalOffsetInPrimaryHeights = 0.95f,
            maxAlertCenterBelowPrimaryInPrimaryHeights = 0.34f,
            maxAlertYawDegrees = 35f,
            maxAlertPitchDegrees = 38f,
            alertConfirmations = 2,
            cautionHoldMs = 1_400L,
            alertHoldMs = 2_600L,
            lowScanIntervalMs = 540L,
            highScanIntervalMs = 95L,
            highBoostDurationMs = 4_500L,
            motionSampleIntervalMs = 170L,
            motionThreshold = 0.045f,
        )
    }
}
