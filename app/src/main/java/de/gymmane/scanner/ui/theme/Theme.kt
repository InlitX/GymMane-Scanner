package de.gymmane.scanner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Background = Color(0xFF0C0D0C)
val Surface = Color(0xFF1C1D1B)
val Field = Color(0xFF292A27)
val Outline = Color(0xFF383A35)
val Apricot = Color(0xFFDCA17F)
val Gold = Color(0xFFE1B456)
val Sage = Color(0xFFA9BD8D)
val Ink = Color(0xFFF5F4F0)
val Muted = Color(0xFF9A9D94)

@Composable
fun GymManeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Apricot, onPrimary = Background,
            primaryContainer = Field, onPrimaryContainer = Ink,
            secondary = Sage, onSecondary = Background,
            secondaryContainer = Ink, onSecondaryContainer = Background,
            tertiary = Gold, onTertiary = Background,
            tertiaryContainer = Field, onTertiaryContainer = Ink,
            background = Background, onBackground = Ink,
            surface = Surface, onSurface = Ink,
            surfaceVariant = Field, onSurfaceVariant = Muted,
            surfaceContainer = Surface, surfaceContainerLow = Surface,
            surfaceContainerHigh = Field, surfaceContainerHighest = Field,
            surfaceContainerLowest = Background, surfaceTint = Apricot,
            inverseSurface = Ink, inverseOnSurface = Background,
            outline = Outline, outlineVariant = Outline, error = Color(0xFFFFB4A9),
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
            headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp),
        ),
        content = { de.gymmane.scanner.ui.MotionProvider(content) },
    )
}
