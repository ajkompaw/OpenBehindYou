package dev.openbehindyou.detector

/** Raw per-frame assessment produced by [FaceEvaluator]. */
data class FaceAssessment(
    val level: SafetyLevel,
    val reason: DetectionReason,
    val primaryPresent: Boolean,
    val totalFaces: Int,
    val nearbyFaces: Int,
    val candidateTrackingId: Int?,
    val motionScore: Float,
)

/** Temporally smoothed result after confirmation and hold windows. */
data class SmoothedAssessment(
    val level: SafetyLevel,
    val reason: DetectionReason,
    val primaryPresent: Boolean,
    val totalFaces: Int,
    val nearbyFaces: Int,
)

/** Immutable report sent from the analyzer thread to the foreground service. */
data class DetectionReport(
    val level: SafetyLevel,
    val reason: DetectionReason,
    val primaryPresent: Boolean,
    val totalFaces: Int,
    val nearbyFaces: Int,
    val effectiveFps: Float,
    val scanMode: ScanMode,
    val timestampMs: Long,
)
