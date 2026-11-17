package dev.openbehindyou.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GuardColorScheme = darkColorScheme(
    primary = GuardMint,
    onPrimary = Color(0xFF042416),
    primaryContainer = Color(0xFF123B2A),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GuardBlue,
    onSecondary = Color(0xFF062434),
    secondaryContainer = Color(0xFF0C3348),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = GuardAmber,
    onTertiary = Color(0xFF2A1A00),
    background = GuardBackground,
    onBackground = GuardText,
    surface = GuardSurface,
    onSurface = GuardText,
    surfaceVariant = GuardSurfaceVariant,
    onSurfaceVariant = GuardTextMuted,
    outline = GuardOutline,
    outlineVariant = Color(0xFF22334D),
    error = GuardRed,
    onError = Color.White,
    errorContainer = Color(0xFF3B1218),
    onErrorContainer = Color(0xFFFECDD3),
)

private val GuardTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val GuardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun OpenBehindYouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GuardColorScheme,
        typography = GuardTypography,
        shapes = GuardShapes,
        content = content,
    )
}
