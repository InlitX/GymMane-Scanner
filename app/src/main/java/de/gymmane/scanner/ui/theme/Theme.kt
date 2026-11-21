package de.gymmane.scanner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import de.gymmane.scanner.R

val Background = Color(0xFF0C0D0C)
val Surface = Color(0xFF1C1D1B)
val Field = Color(0xFF292A27)
val Outline = Color(0xFF383A35)
val Apricot = Color(0xFFDCA17F)
val Gold = Color(0xFFE1B456)
val Sage = Color(0xFFA9BD8D)
val Ink = Color(0xFFF5F4F0)
val Muted = Color(0xFF9A9D94)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.nunito(), displayMedium = base.displayMedium.nunito(), displaySmall = base.displaySmall.nunito(),
        headlineLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
        headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
        headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
        titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.3).sp),
        titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 15.sp, letterSpacing = 1.4.sp),
    )
}

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
        typography = AppTypography,
        content = { de.gymmane.scanner.ui.MotionProvider(content) },
    )
}
