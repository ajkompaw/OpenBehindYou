package dev.openbehindyou.detector

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Thin local-only wrapper around ML Kit's bundled face detector.
 *
 * PERFORMANCE_MODE_FAST + tracking is the low-latency configuration. Tracking
 * IDs let the smoother require a stable candidate across confirmation frames.
 */
class MlKitFaceDetector {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.08f)
            .enableTracking()
            .build(),
    )

    fun detect(image: InputImage): Task<List<Face>> = detector.process(image)

    fun close() {
        detector.close()
    }
}
