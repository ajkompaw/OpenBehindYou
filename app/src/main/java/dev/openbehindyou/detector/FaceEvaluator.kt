package dev.openbehindyou.detector

import kotlin.math.abs

/**
 * Pure heuristic classifier.
 *
 * The largest sufficiently large face is the device owner's primary face.
 * Other faces are only considered threats when they are proportionally close
 * to the primary face and positioned near the owner's head/shoulder region.
 * That combination filters most background crowds and faces in posters.
 *
 * "Looking over the shoulder" is approximated from ML Kit's head pose. It is
 * intentionally a heuristic: a head-on secondary face that is large, close,
 * and at/above the primary head's level stays ALERT for a few frames before
 * the temporal smoother confirms it.
 */
class FaceEvaluator {

    fun assess(
        faces: List<FaceObservation>,
        config: DetectionConfig,
        motionScore: Float,
        motionRecent: Boolean,
    ): FaceAssessment {
        val eligible = faces.filter { it.bounds.area >= config.minPrimaryFaceAreaFraction }
        if (eligible.isEmpty()) {
            return FaceAssessment(
                level = SafetyLevel.SAFE,
                reason = DetectionReason.NO_USER,
                primaryPresent = false,
                totalFaces = faces.size,
                nearbyFaces = 0,
                candidateTrackingId = null,
                motionScore = motionScore,
            )
        }

        val primary = eligible.maxByOrNull { it.bounds.area } ?: eligible.first()
        if (primary.bounds.area <= 0f) {
            return FaceAssessment(
                level = SafetyLevel.SAFE,
                reason = DetectionReason.NO_USER,
                primaryPresent = false,
                totalFaces = faces.size,
                nearbyFaces = 0,
                candidateTrackingId = null,
                motionScore = motionScore,
            )
        }

        val others = faces.filter { it !== primary }
        val nearby = mutableListOf<FaceObservation>()
        val alertCandidates = mutableListOf<FaceObservation>()
        var weakNearbyFace = false

        for (face in others) {
            val primaryWidth = primary.bounds.width
            val primaryHeight = primary.bounds.height
            if (primaryWidth <= 0f || primaryHeight <= 0f) continue

            val areaRatio = face.bounds.area / primary.bounds.area
            if (areaRatio <= 0f) continue

            val dx = abs(face.bounds.centerX - primary.bounds.centerX)
            val dy = abs(face.bounds.centerY - primary.bounds.centerY)
            val notFarBelow = face.bounds.centerY <=
                primary.bounds.bottom + primaryHeight * config.maxNearbyBelowPrimaryInPrimaryHeights

            val broadlyNear = dx <= primaryWidth * config.maxNearbySideOffsetInPrimaryWidths * 1.35f &&
                dy <= primaryHeight * config.maxNearbyVerticalOffsetInPrimaryHeights * 1.30f &&
                notFarBelow
            val weakRatio = config.minSecondaryToPrimaryAreaRatio * 0.55f
            if (areaRatio >= weakRatio && broadlyNear) {
                weakNearbyFace = true
            }

            if (areaRatio < config.minSecondaryToPrimaryAreaRatio) continue
            val isNear = dx <= primaryWidth * config.maxNearbySideOffsetInPrimaryWidths &&
                dy <= primaryHeight * config.maxNearbyVerticalOffsetInPrimaryHeights &&
                notFarBelow
            if (!isNear) continue

            nearby += face

            val headFacingForward = abs(face.headEulerAngleY) <= config.maxAlertYawDegrees &&
                abs(face.headEulerAngleX) <= config.maxAlertPitchDegrees
            val tightPosition = dx <= primaryWidth * config.maxAlertSideOffsetInPrimaryWidths &&
                dy <= primaryHeight * config.maxAlertVerticalOffsetInPrimaryHeights &&
                face.bounds.centerY <=
                primary.bounds.centerY + primaryHeight * config.maxAlertCenterBelowPrimaryInPrimaryHeights

            if (areaRatio >= config.alertSecondaryToPrimaryAreaRatio && headFacingForward && tightPosition) {
                alertCandidates += face
            }
        }

        val strongestAlert = alertCandidates.maxByOrNull { it.bounds.area }
        val strongestNearby = nearby.maxByOrNull { it.bounds.area }

        val level: SafetyLevel
        val reason: DetectionReason
        val candidate: FaceObservation?

        when {
            strongestAlert != null -> {
                level = SafetyLevel.ALERT
                reason = DetectionReason.LOOKING_OVER_SHOULDER
                candidate = strongestAlert
            }

            strongestNearby != null -> {
                level = SafetyLevel.CAUTION
                reason = DetectionReason.SECONDARY_FACE_NEARBY
                candidate = strongestNearby
            }

            motionRecent && motionScore >= config.motionThreshold && weakNearbyFace -> {
                level = SafetyLevel.CAUTION
                reason = DetectionReason.MOVEMENT_DETECTED
                candidate = others
                    .filter { it.bounds.area / primary.bounds.area >= config.minSecondaryToPrimaryAreaRatio * 0.55f }
                    .maxByOrNull { it.bounds.area }
            }

            else -> {
                level = SafetyLevel.SAFE
                reason = DetectionReason.NONE
                candidate = null
            }
        }

        return FaceAssessment(
            level = level,
            reason = reason,
            primaryPresent = true,
            totalFaces = faces.size,
            nearbyFaces = nearby.size,
            candidateTrackingId = candidate?.trackingId,
            motionScore = motionScore,
        )
    }
}
