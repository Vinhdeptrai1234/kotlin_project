package com.example.mhike.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF7C3AED)      // tím
private val Secondary = Color(0xFF06B6D4)    // cyan
private val Tertiary = Color(0xFFFF6B6B)     // hồng cam
private val Bg = Color(0xFFF7F7FB)
private val Surface = Color(0xFFFFFFFF)

val VibrantScheme = lightColorScheme(
    primary = Primary, onPrimary = Color.White,
    secondary = Secondary, onSecondary = Color.White,
    tertiary = Tertiary, onTertiary = Color.White,
    background = Bg, surface = Surface, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFECE8F8), outline = Color(0xFFD0CFE4)
)

@Composable
fun VibrantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibrantScheme,
        typography = Typography(),
        content = content
    )
}
