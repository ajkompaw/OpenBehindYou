package dev.openbehindyou.detector

/** Three-state classification surfaced to the UI and notifications. */
enum class SafetyLevel {
    SAFE,
    CAUTION,
    ALERT,
}

/** Why the detector selected the current level. */
enum class DetectionReason {
    NONE,
    NO_USER,
    SECONDARY_FACE_NEARBY,
    LOOKING_OVER_SHOULDER,
    MOVEMENT_DETECTED,
}

/** Adaptive analyzer mode, exposed for diagnostics in the UI. */
enum class ScanMode {
    LOW,
    HIGH,
}
