package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = DeepNavyInk,
    primaryContainer = Color(0xFF0F3D36),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = CobaltSecondaryDark,
    onSecondary = DeepNavyInk,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = AmberExitGoldBright,
    onTertiary = DeepNavyInk,
    background = MidnightBackground,
    onBackground = CrispWhiteInk,
    surface = MidnightSurface,
    onSurface = CrispWhiteInk,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = SoftSlateInk,
    outline = MidnightBorder,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = CobaltSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = AmberExitGold,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = DeepNavyInk,
    surface = IvorySurface,
    onSurface = DeepNavyInk,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = SlateSecondaryInk,
    outline = SlateBorder,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

@Composable
fun PythonYolAcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    largeText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = buildAppTypography(largeText = largeText)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
