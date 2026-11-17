package dev.openbehindyou.camera

import android.util.Log
import androidx.camera.core.ImageProxy
import kotlin.math.abs

/**
 * Very cheap luma-motion gate. It never allocates large bitmaps and samples at
 * only a handful of FPS, even while the camera produces 30 FPS.
 *
 * A global mean-delta subtraction keeps automatic exposure changes from
 * looking like motion; only locally changing blocks raise the score.
 */
class MotionMeter(
    private val gridWidth: Int = 24,
    private val gridHeight: Int = 24,
) {
    private var previous = IntArray(gridWidth * gridHeight)
    private var previousWidth = 0
    private var previousHeight = 0
    private var hasPrevious = false
    private var lastSampleAtMs = 0L
    private var smoothedScore = 0f

    data class Reading(
        val score: Float,
        val sampledAtMs: Long,
    )

    fun sample(image: ImageProxy, nowMs: Long, minIntervalMs: Long): Reading? {
        if (nowMs - lastSampleAtMs < minIntervalMs) return null

        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        return try {
            val current = IntArray(gridWidth * gridHeight)
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            for (gy in 0 until gridHeight) {
                val sourceY = (((gy + 0.5f) * height) / gridHeight)
                    .toInt()
                    .coerceIn(0, height - 1)
                val rowStart = sourceY * rowStride
                for (gx in 0 until gridWidth) {
                    val sourceX = (((gx + 0.5f) * width) / gridWidth)
                        .toInt()
                        .coerceIn(0, width - 1)
                    current[gy * gridWidth + gx] =
                        buffer.get(rowStart + sourceX * pixelStride).toInt() and 0xFF
                }
            }

            if (!hasPrevious || previousWidth != width || previousHeight != height) {
                previous = current
                previousWidth = width
                previousHeight = height
                hasPrevious = true
                lastSampleAtMs = nowMs
                smoothedScore = 0f
                return Reading(0f, nowMs)
            }

            val count = current.size
            val deltas = IntArray(count)
            var deltaSum = 0L
            for (index in 0 until count) {
                val delta = current[index] - previous[index]
                deltas[index] = delta
                deltaSum += delta
            }
            val globalDelta = deltaSum.toFloat() / count
            var changedBlocks = 0
            for (delta in deltas) {
                if (abs(delta - globalDelta) > LOCAL_DELTA_THRESHOLD) {
                    changedBlocks++
                }
            }

            val rawScore = changedBlocks.toFloat() / count
            smoothedScore = smoothedScore * 0.45f + rawScore * 0.55f
            previous = current
            previousWidth = width
            previousHeight = height
            lastSampleAtMs = nowMs

            Reading(score = smoothedScore.coerceIn(0f, 1f), sampledAtMs = nowMs)
        } catch (t: Throwable) {
            Log.w(TAG, "Motion sampling failed", t)
            hasPrevious = false
            null
        }
    }

    fun reset() {
        hasPrevious = false
        previousWidth = 0
        previousHeight = 0
        lastSampleAtMs = 0L
        smoothedScore = 0f
    }

    private companion object {
        const val TAG = "MotionMeter"
        const val LOCAL_DELTA_THRESHOLD = 18f
    }
}
