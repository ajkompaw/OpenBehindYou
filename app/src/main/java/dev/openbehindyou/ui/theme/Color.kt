package dev.openbehindyou.ui.theme

import androidx.compose.ui.graphics.Color
import dev.openbehindyou.detector.SafetyLevel

val GuardBackground = Color(0xFF0B1220)
val GuardSurface = Color(0xFF131D2E)
val GuardSurfaceRaised = Color(0xFF1A2740)
val GuardSurfaceVariant = Color(0xFF243451)
val GuardOutline = Color(0xFF334763)
val GuardText = Color(0xFFF8FAFC)
val GuardTextMuted = Color(0xFF9FB0C7)
val GuardMint = Color(0xFF34D399)
val GuardBlue = Color(0xFF38BDF8)
val GuardAmber = Color(0xFFF59E0B)
val GuardRed = Color(0xFFEF4444)
val GuardGreen = Color(0xFF22C55E)

fun safetyColor(level: SafetyLevel): Color = when (level) {
    SafetyLevel.SAFE -> GuardGreen
    SafetyLevel.CAUTION -> GuardAmber
    SafetyLevel.ALERT -> GuardRed
}
