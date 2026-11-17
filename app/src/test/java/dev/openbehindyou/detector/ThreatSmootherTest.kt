package dev.openbehindyou.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreatSmootherTest {

    private val config = Sensitivity.MEDIUM.toConfig()
    private val smoother = ThreatSmoother()

    @Test
    fun `alert requires confirmation then holds`() {
        val alert = assessment(
            level = SafetyLevel.ALERT,
            reason = DetectionReason.LOOKING_OVER_SHOULDER,
            candidateId = 7,
        )

        assertEquals(SafetyLevel.CAUTION, smoother.update(alert, 1_000L, config).level)
        assertEquals(SafetyLevel.CAUTION, smoother.update(alert, 1_100L, config).level)
        val confirmed = smoother.update(alert, 1_200L, config)
        assertEquals(SafetyLevel.ALERT, confirmed.level)
        assertEquals(DetectionReason.LOOKING_OVER_SHOULDER, confirmed.reason)

        val safe = assessment(level = SafetyLevel.SAFE, reason = DetectionReason.NONE)
        assertEquals(SafetyLevel.ALERT, smoother.update(safe, 2_500L, config).level)
        assertEquals(SafetyLevel.ALERT, smoother.update(safe, 3_900L, config).level)
        assertEquals(SafetyLevel.SAFE, smoother.update(safe, 4_100L, config).level)
    }

    @Test
    fun `non-alert frame resets confirmation streak`() {
        val alert = assessment(
            level = SafetyLevel.ALERT,
            reason = DetectionReason.LOOKING_OVER_SHOULDER,
            candidateId = 3,
        )
        val caution = assessment(
            level = SafetyLevel.CAUTION,
            reason = DetectionReason.SECONDARY_FACE_NEARBY,
            candidateId = 3,
        )

        smoother.update(alert, 1_000L, config)
        assertEquals(SafetyLevel.CAUTION, smoother.update(caution, 1_100L, config).level)
        assertEquals(SafetyLevel.CAUTION, smoother.update(alert, 1_200L, config).level)
        assertEquals(SafetyLevel.CAUTION, smoother.update(alert, 1_300L, config).level)
        assertEquals(SafetyLevel.ALERT, smoother.update(alert, 1_400L, config).level)
    }

    @Test
    fun `changing tracking id restarts confirmation`() {
        val first = assessment(
            level = SafetyLevel.ALERT,
            reason = DetectionReason.LOOKING_OVER_SHOULDER,
            candidateId = 1,
        )
        val second = assessment(
            level = SafetyLevel.ALERT,
            reason = DetectionReason.LOOKING_OVER_SHOULDER,
            candidateId = 2,
        )

        smoother.update(first, 1_000L, config)
        smoother.update(first, 1_100L, config)
        assertEquals(SafetyLevel.CAUTION, smoother.update(second, 1_200L, config).level)
        assertEquals(SafetyLevel.CAUTION, smoother.update(second, 1_300L, config).level)
        assertEquals(SafetyLevel.ALERT, smoother.update(second, 1_400L, config).level)
    }

    @Test
    fun `no user reason is preserved while safe`() {
        val result = smoother.update(
            assessment(
                level = SafetyLevel.SAFE,
                reason = DetectionReason.NO_USER,
                primaryPresent = false,
            ),
            2_000L,
            config,
        )

        assertEquals(SafetyLevel.SAFE, result.level)
        assertEquals(DetectionReason.NO_USER, result.reason)
    }

    private fun assessment(
        level: SafetyLevel,
        reason: DetectionReason,
        candidateId: Int? = null,
        primaryPresent: Boolean = true,
    ) = FaceAssessment(
        level = level,
        reason = reason,
        primaryPresent = primaryPresent,
        totalFaces = if (primaryPresent) 2 else 0,
        nearbyFaces = if (level == SafetyLevel.SAFE) 0 else 1,
        candidateTrackingId = candidateId,
        motionScore = 0f,
    )
}
