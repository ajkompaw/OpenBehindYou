package dev.openbehindyou.detector

import kotlin.math.max

/**
 * A rectangle normalized to the upright analysis frame: (0,0) is top-left and
 * (1,1) is bottom-right. Keeping these values free of Android graphics classes
 * makes the heuristic engine unit-testable on the JVM.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = max(0f, right - left)
    val height: Float get() = max(0f, bottom - top)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    companion object {
        fun fromPixels(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            frameWidth: Int,
            frameHeight: Int,
        ): NormalizedRect {
            val safeWidth = frameWidth.coerceAtLeast(1).toFloat()
            val safeHeight = frameHeight.coerceAtLeast(1).toFloat()
            return NormalizedRect(
                left = (left / safeWidth).coerceIn(0f, 1f),
                top = (top / safeHeight).coerceIn(0f, 1f),
                right = (right / safeWidth).coerceIn(0f, 1f),
                bottom = (bottom / safeHeight).coerceIn(0f, 1f),
            )
        }
    }
}
