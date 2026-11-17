package dev.openbehindyou.detector

/** A single ML Kit face result converted into normalized, rotation-corrected geometry. */
data class FaceObservation(
    val trackingId: Int?,
    val bounds: NormalizedRect,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
)
