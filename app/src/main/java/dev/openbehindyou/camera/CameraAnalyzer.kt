package dev.openbehindyou.camera

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import dev.openbehindyou.detector.DetectionConfig
import dev.openbehindyou.detector.DetectionReport
import dev.openbehindyou.detector.FaceEvaluator
import dev.openbehindyou.detector.FaceObservation
import dev.openbehindyou.detector.MlKitFaceDetector
import dev.openbehindyou.detector.NormalizedRect
import dev.openbehindyou.detector.ScanMode
import dev.openbehindyou.detector.ThreatSmoother
import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * CameraX analyzer with:
 *  - single-threaded, serialized ML Kit work;
 *  - STRATEGY_KEEP_ONLY_LATEST at the CameraX layer;
 *  - separate cheap motion sampling;
 *  - adaptive ML Kit duty cycling;
 *  - bounding boxes normalized into ML Kit's upright image space.
 */
class CameraAnalyzer(
    private val detector: MlKitFaceDetector,
    initialConfig: DetectionConfig,
    private val listener: Listener,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var config: DetectionConfig = initialConfig

    @Volatile
    private var closed = false

    private val cleanupStarted = AtomicBoolean(false)

    private val analyzerExecutor: Executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "OpenBehindYou-analyzer").apply {
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    private val evaluator = FaceEvaluator()
    private val smoother = ThreatSmoother()
    private val motionMeter = MotionMeter()
    private val adaptive = AdaptiveScanController()
    private val inFlight = AtomicBoolean(false)
    private val detectionCompletionTimes = ArrayDeque<Long>()

    private var nextDetectionAtMs = 0L
    private var latestMotionScore = 0f
    private var lastMotionAtMs = 0L
    private var lastErrorAtMs = 0L

    val executor: Executor get() = analyzerExecutor

    fun updateConfig(newConfig: DetectionConfig) {
        config = newConfig
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (closed) {
            image.close()
            return
        }

        val now = android.os.SystemClock.elapsedRealtime()
        val currentConfig = config
        try {
            val mediaImage = image.image ?: run {
                image.close()
                return
            }

            val motionReading = motionMeter.sample(
                image = image,
                nowMs = now,
                minIntervalMs = currentConfig.motionSampleIntervalMs,
            )
            if (motionReading != null) {
                latestMotionScore = motionReading.score
                if (motionReading.score >= currentConfig.motionThreshold) {
                    lastMotionAtMs = now
                    adaptive.boost(now, currentConfig.highBoostDurationMs)
                    nextDetectionAtMs = minOf(nextDetectionAtMs, now)
                }
            }

            if (inFlight.get() || now < nextDetectionAtMs) {
                image.close()
                return
            }

            if (closed) {
                image.close()
                return
            }

            if (!inFlight.compareAndSet(false, true)) {
                image.close()
                return
            }

            if (closed) {
                inFlight.set(false)
                image.close()
                releaseResources()
                return
            }

            nextDetectionAtMs = now + adaptive.intervalMs(now, currentConfig)

            val rotation = image.imageInfo.rotationDegrees
            val measures = rotation == 90 || rotation == 270
            val uprightWidth = if (measures) image.height else image.width
            val uprightHeight = if (measures) image.width else image.height
            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

            detector.detect(inputImage)
                .addOnSuccessListener(analyzerExecutor) { faces ->
                    if (closed) return@addOnSuccessListener
                    try {
                        val observations = faces.mapNotNull { face ->
                            val bounds = face.boundingBox
                            if (bounds.width() <= 0 || bounds.height() <= 0) {
                                null
                            } else {
                                FaceObservation(
                                    trackingId = face.trackingId,
                                    bounds = NormalizedRect.fromPixels(
                                        left = bounds.left,
                                        top = bounds.top,
                                        right = bounds.right,
                                        bottom = bounds.bottom,
                                        frameWidth = uprightWidth,
                                        frameHeight = uprightHeight,
                                    ),
                                    headEulerAngleX = face.headEulerAngleX,
                                    headEulerAngleY = face.headEulerAngleY,
                                    headEulerAngleZ = face.headEulerAngleZ,
                                )
                            }
                        }
                        handleDetections(observations, currentConfig)
                    } catch (t: Throwable) {
                        reportError(t)
                    }
                }
                .addOnFailureListener(analyzerExecutor) { error ->
                    if (!closed) reportError(error)
                }
                .addOnCompleteListener(analyzerExecutor) {
                    image.close()
                    inFlight.set(false)
                    if (closed) {
                        releaseResources()
                        return@addOnCompleteListener
                    }
                    val completedAt = android.os.SystemClock.elapsedRealtime()
                    nextDetectionAtMs = completedAt + adaptive.intervalMs(completedAt, config)
                }
        } catch (t: Throwable) {
            image.close()
            inFlight.set(false)
            if (closed) releaseResources()
            reportError(t)
        }
    }

    private fun handleDetections(
        observations: List<FaceObservation>,
        currentConfig: DetectionConfig,
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        val motionRecent = now - lastMotionAtMs <= MOTION_RECENT_WINDOW_MS
        val rawAssessment = evaluator.assess(
            faces = observations,
            config = currentConfig,
            motionScore = latestMotionScore,
            motionRecent = motionRecent,
        )
        adaptive.onAssessment(rawAssessment, now, currentConfig)
        val smoothed = smoother.update(rawAssessment, now, currentConfig)
        val fps = recordDetection(now)
        val mode = adaptive.mode(now)

        listener.onDetection(
            DetectionReport(
                level = smoothed.level,
                reason = smoothed.reason,
                primaryPresent = smoothed.primaryPresent,
                totalFaces = smoothed.totalFaces,
                nearbyFaces = smoothed.nearbyFaces,
                effectiveFps = fps,
                scanMode = mode,
                timestampMs = now,
            ),
        )
    }

    private fun recordDetection(nowMs: Long): Float {
        detectionCompletionTimes.addLast(nowMs)
        while (detectionCompletionTimes.isNotEmpty() &&
            detectionCompletionTimes.first < nowMs - FPS_WINDOW_MS
        ) {
            detectionCompletionTimes.removeFirst()
        }
        if (detectionCompletionTimes.size < 2) return 0f
        val span = max(1L, detectionCompletionTimes.last - detectionCompletionTimes.first)
        return (detectionCompletionTimes.size - 1) * 1_000f / span
    }

    private fun reportError(error: Throwable) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastErrorAtMs >= ERROR_REPORT_INTERVAL_MS) {
            lastErrorAtMs = now
            Log.w(TAG, "Face analysis failed", error)
            listener.onAnalyzerError(error)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        if (inFlight.get()) {
            // Do not shut the executor down while an ML Kit task is completing:
            // its completion listener still needs to run and close the image.
            return
        }
        releaseResources()
    }

    private fun releaseResources() {
        if (!cleanupStarted.compareAndSet(false, true)) return
        try {
            detector.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Detector close failed", t)
        }
        (analyzerExecutor as? java.util.concurrent.ExecutorService)?.shutdown()
        smoother.reset()
        motionMeter.reset()
        adaptive.reset()
    }

    interface Listener {
        fun onDetection(report: DetectionReport)
        fun onAnalyzerError(error: Throwable)
    }

    private companion object {
        const val TAG = "CameraAnalyzer"
        const val FPS_WINDOW_MS = 5_000L
        const val MOTION_RECENT_WINDOW_MS = 1_200L
        const val ERROR_REPORT_INTERVAL_MS = 10_000L
    }
}
