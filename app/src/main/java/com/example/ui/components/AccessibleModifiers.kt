package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FocusRingDark
import com.example.ui.theme.FocusRingLight

fun Modifier.accessibleFocusRing(
    shape: Shape = RoundedCornerShape(12.dp),
    width: Dp = 3.dp,
    darkThemeOverride: Boolean? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val isDark = darkThemeOverride ?: isSystemInDarkTheme()
    val ringColor: Color = if (isDark) FocusRingDark else FocusRingLight

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
        }
        .then(
            if (isFocused) {
                Modifier.border(width = width, color = ringColor, shape = shape)
            } else {
                Modifier
            }
        )
}
