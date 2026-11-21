package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WaveBridgeDarkScheme = darkColorScheme(
  primary = ElectricCyan,
  onPrimary = Color(0xFF002028),
  primaryContainer = Color(0xFF0E3A4D),
  onPrimaryContainer = Color(0xFFA5F3FC),
  secondary = CobaltAccent,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF1A2D50),
  onSecondaryContainer = Color(0xFFBFDBFE),
  tertiary = LiveEmerald,
  onTertiary = Color(0xFF002114),
  tertiaryContainer = Color(0xFF0A3628),
  onTertiaryContainer = Color(0xFFA7F3D0),
  error = CrimsonError,
  onError = Color.White,
  errorContainer = Color(0xFF450A0A),
  onErrorContainer = Color(0xFFFECACA),
  background = ObsidianBg,
  onBackground = TextPrimaryDark,
  surface = SlateSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = SlateSurfaceElevated,
  onSurfaceVariant = TextSecondaryDark,
  surfaceContainerLowest = ObsidianBg,
  surfaceContainerLow = SlateSurface,
  surfaceContainer = SlateSurfaceElevated,
  surfaceContainerHigh = SlateSurfaceHigh,
  outline = SlateOutline,
  outlineVariant = Color(0xFF1E293B),
)

private val WaveBridgeLightScheme = lightColorScheme(
  primary = CyanPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0C4A6E),
  secondary = CobaltSecondaryLight,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDBEAFE),
  onSecondaryContainer = Color(0xFF1E3A8A),
  tertiary = Color(0xFF059669),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFD1FAE5),
  onTertiaryContainer = Color(0xFF064E3B),
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = LightBg,
  onBackground = TextPrimaryLight,
  surface = LightSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = LightSurfaceElevated,
  onSurfaceVariant = TextSecondaryLight,
  surfaceContainerLowest = LightSurface,
  surfaceContainerLow = Color(0xFFF8FAFC),
  surfaceContainer = LightSurfaceElevated,
  surfaceContainerHigh = Color(0xFFE2E8F0),
  outline = LightOutline,
  outlineVariant = Color(0xFFE2E8F0),
)

val WaveBridgeShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WaveBridgeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) WaveBridgeDarkScheme else WaveBridgeLightScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = WaveBridgeTypography,
    shapes = WaveBridgeShapes,
    content = content,
  )
}
