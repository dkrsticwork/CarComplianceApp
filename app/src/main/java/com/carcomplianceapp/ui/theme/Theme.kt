package com.carcomplianceapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────

object AppColors {
    // Teal greens (primary)
    val Teal50   = Color(0xFFE1F5EE)
    val Teal100  = Color(0xFF9FE1CB)
    val Teal400  = Color(0xFF1D9E75)
    val Teal600  = Color(0xFF0F6E56)
    val Teal800  = Color(0xFF085041)

    // Amber (warning)
    val Amber50  = Color(0xFFFAEEDA)
    val Amber400 = Color(0xFFBA7517)
    val Amber600 = Color(0xFF854F0B)
    val Amber800 = Color(0xFF633806)

    // Red (danger / critical)
    val Red50    = Color(0xFFFCEBEB)
    val Red400   = Color(0xFFE24B4A)
    val Red600   = Color(0xFFA32D2D)
    val Red800   = Color(0xFF791F1F)

    // Blue (info / legal)
    val Blue50   = Color(0xFFE6F1FB)
    val Blue400  = Color(0xFF378ADD)
    val Blue600  = Color(0xFF185FA5)
    val Blue800  = Color(0xFF0C447C)

    // Gray (neutral)
    val Gray50   = Color(0xFFF1EFE8)
    val Gray100  = Color(0xFFD3D1C7)
    val Gray200  = Color(0xFFB4B2A9)
    val Gray400  = Color(0xFF888780)
    val Gray600  = Color(0xFF5F5E5A)
    val Gray800  = Color(0xFF444441)
    val Gray900  = Color(0xFF2C2C2A)

    // Surface
    val Background = Color(0xFFF8F7F4)
    val Surface    = Color(0xFFFFFFFF)
    val SurfaceVar = Color(0xFFF1EFE8)
}

private val LightColorScheme = lightColorScheme(
    primary          = AppColors.Teal400,
    onPrimary        = Color.White,
    primaryContainer = AppColors.Teal50,
    onPrimaryContainer = AppColors.Teal800,

    secondary        = AppColors.Blue600,
    onSecondary      = Color.White,
    secondaryContainer = AppColors.Blue50,
    onSecondaryContainer = AppColors.Blue800,

    error            = AppColors.Red400,
    onError          = Color.White,
    errorContainer   = AppColors.Red50,
    onErrorContainer = AppColors.Red800,

    background       = AppColors.Background,
    onBackground     = AppColors.Gray900,

    surface          = AppColors.Surface,
    onSurface        = AppColors.Gray900,
    surfaceVariant   = AppColors.SurfaceVar,
    onSurfaceVariant = AppColors.Gray600,

    outline          = AppColors.Gray200,
    outlineVariant   = AppColors.Gray100
)

private val DarkColorScheme = darkColorScheme(
    primary          = AppColors.Teal100,
    onPrimary        = AppColors.Teal800,
    primaryContainer = AppColors.Teal800,
    onPrimaryContainer = AppColors.Teal50,

    secondary        = AppColors.Blue400,
    onSecondary      = AppColors.Blue800,
    secondaryContainer = AppColors.Blue800,
    onSecondaryContainer = AppColors.Blue50,

    error            = AppColors.Red400,
    onError          = AppColors.Red800,
    errorContainer   = AppColors.Red800,
    onErrorContainer = AppColors.Red50,

    background       = AppColors.Gray900,
    onBackground     = AppColors.Gray50,

    surface          = Color(0xFF1C1C1A),
    onSurface        = AppColors.Gray50,
    surfaceVariant   = Color(0xFF28281F),
    onSurfaceVariant = AppColors.Gray200,

    outline          = AppColors.Gray600,
    outlineVariant   = AppColors.Gray800
)

@Composable
fun CarComplianceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
