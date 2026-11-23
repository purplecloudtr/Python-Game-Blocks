package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

fun buildAppTypography(largeText: Boolean): Typography {
    val scale = if (largeText) 1.18f else 1.0f
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * scale).sp,
            lineHeight = (32f * scale).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * scale).sp,
            lineHeight = (28f * scale).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (25f * scale).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (17f * scale).sp,
            lineHeight = (25f * scale).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp,
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * scale).sp,
            lineHeight = (19f * scale).sp,
        ),
    )
}
