package dev.openbehindyou.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceEvaluatorTest {

    private val config = Sensitivity.MEDIUM.toConfig()
    private val evaluator = FaceEvaluator()

    @Test
    fun `primary face alone is safe`() {
        val result = evaluator.assess(
            faces = listOf(primary()),
            config = config,
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.SAFE, result.level)
        assertEquals(DetectionReason.NONE, result.reason)
        assertTrue(result.primaryPresent)
        assertEquals(1, result.totalFaces)
        assertEquals(0, result.nearbyFaces)
    }

    @Test
    fun `distant background face is ignored`() {
        val distant = face(
            left = 0.04f,
            top = 0.04f,
            right = 0.11f,
            bottom = 0.13f,
        )

        val result = evaluator.assess(
            faces = listOf(primary(), distant),
            config = config,
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.SAFE, result.level)
        assertEquals(0, result.nearbyFaces)
        assertEquals(2, result.totalFaces)
    }

    @Test
    fun `nearby secondary face with sideways gaze is caution`() {
        val secondary = face(
            left = 0.62f,
            top = 0.30f,
            right = 0.77f,
            bottom = 0.50f,
            yaw = 65f,
        )

        val result = evaluator.assess(
            faces = listOf(primary(), secondary),
            config = config,
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.CAUTION, result.level)
        assertEquals(DetectionReason.SECONDARY_FACE_NEARBY, result.reason)
        assertEquals(1, result.nearbyFaces)
    }

    @Test
    fun `large close head-on secondary face raises alert`() {
        val secondary = face(
            left = 0.62f,
            top = 0.28f,
            right = 0.82f,
            bottom = 0.53f,
            yaw = 5f,
            pitch = 2f,
        )

        val result = evaluator.assess(
            faces = listOf(primary(), secondary),
            config = config,
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.ALERT, result.level)
        assertEquals(DetectionReason.LOOKING_OVER_SHOULDER, result.reason)
        assertEquals(1, result.nearbyFaces)
    }

    @Test
    fun `motion with a weak candidate is caution instead of alert`() {
        val weak = face(
            left = 0.66f,
            top = 0.32f,
            right = 0.78f,
            bottom = 0.48f,
            yaw = 0f,
        )

        val result = evaluator.assess(
            faces = listOf(primary(), weak),
            config = config,
            motionScore = 0.25f,
            motionRecent = true,
        )

        assertEquals(SafetyLevel.CAUTION, result.level)
        assertEquals(DetectionReason.MOVEMENT_DETECTED, result.reason)
        assertEquals(0, result.nearbyFaces)
    }

    @Test
    fun `no eligible face returns no user`() {
        val result = evaluator.assess(
            faces = emptyList(),
            config = config,
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.SAFE, result.level)
        assertEquals(DetectionReason.NO_USER, result.reason)
        assertFalse(result.primaryPresent)
        assertNull(result.candidateTrackingId)
    }

    @Test
    fun `high sensitivity accepts a face that low sensitivity ignores`() {
        val secondary = face(
            left = 0.64f,
            top = 0.32f,
            right = 0.78f,
            bottom = 0.49f,
            yaw = 0f,
        )

        val low = evaluator.assess(
            faces = listOf(primary(), secondary),
            config = Sensitivity.LOW.toConfig(),
            motionScore = 0f,
            motionRecent = false,
        )
        val high = evaluator.assess(
            faces = listOf(primary(), secondary),
            config = Sensitivity.HIGH.toConfig(),
            motionScore = 0f,
            motionRecent = false,
        )

        assertEquals(SafetyLevel.SAFE, low.level)
        assertEquals(SafetyLevel.CAUTION, high.level)
    }

    private fun primary(): FaceObservation = FaceObservation(
        trackingId = 1,
        bounds = NormalizedRect(0.35f, 0.25f, 0.65f, 0.60f),
        headEulerAngleX = 0f,
        headEulerAngleY = 0f,
        headEulerAngleZ = 0f,
    )

    private fun face(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        yaw: Float = 0f,
        pitch: Float = 0f,
    ): FaceObservation = FaceObservation(
        trackingId = 2,
        bounds = NormalizedRect(left, top, right, bottom),
        headEulerAngleX = pitch,
        headEulerAngleY = yaw,
        headEulerAngleZ = 0f,
    )
}
