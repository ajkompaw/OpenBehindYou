package dev.openbehindyou.camera

import dev.openbehindyou.detector.DetectionConfig
import dev.openbehindyou.detector.DetectionReason
import dev.openbehindyou.detector.FaceAssessment
import dev.openbehindyou.detector.SafetyLevel
import dev.openbehindyou.detector.ScanMode
import dev.openbehindyou.detector.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveScanControllerTest {

    private val config: DetectionConfig = Sensitivity.MEDIUM.toConfig()

    @Test
    fun `assessment boosts high frequency window then decays`() {
        val controller = AdaptiveScanController()
        val assessment = FaceAssessment(
            level = SafetyLevel.CAUTION,
            reason = DetectionReason.SECONDARY_FACE_NEARBY,
            primaryPresent = true,
            totalFaces = 2,
            nearbyFaces = 1,
            candidateTrackingId = 5,
            motionScore = 0f,
        )

        assertFalse(controller.isHigh(1_000L))
        controller.onAssessment(assessment, 1_000L, config)
        assertTrue(controller.isHigh(1_001L))
        assertEquals(ScanMode.HIGH, controller.mode(1_001L))
        assertEquals(config.highScanIntervalMs, controller.intervalMs(1_001L, config))
        assertFalse(controller.isHigh(1_000L + config.highBoostDurationMs + 1L))
    }

    @Test
    fun `safe assessment in low mode stays low`() {
        val controller = AdaptiveScanController()
        val safe = FaceAssessment(
            level = SafetyLevel.SAFE,
            reason = DetectionReason.NONE,
            primaryPresent = true,
            totalFaces = 1,
            nearbyFaces = 0,
            candidateTrackingId = null,
            motionScore = 0f,
        )

        controller.onAssessment(safe, 5_000L, config)
        assertFalse(controller.isHigh(5_000L))
        assertEquals(config.lowScanIntervalMs, controller.intervalMs(5_000L, config))
    }
}
