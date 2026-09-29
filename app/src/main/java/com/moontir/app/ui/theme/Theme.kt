package com.moontir.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MoontirCustomColors(
    val surface: Color,
    val onSurface: Color,
    val surfaceSecondary: Color,
    val onSurfaceSecondary: Color,
    val surfaceTertiary: Color,
    val onSurfaceTertiary: Color,
    val brand: Color,
    val onBrand: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandTertiary: Color,
    val moonGlow: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val muted: Color
)

val DarkCustomColors = MoontirCustomColors(
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceSecondary = DarkSurfaceSecondary,
    onSurfaceSecondary = DarkOnSurfaceSecondary,
    surfaceTertiary = DarkSurfaceTertiary,
    onSurfaceTertiary = DarkOnSurfaceTertiary,
    brand = MoontirSageGreen,
    onBrand = DarkSurface,
    brandPrimary = MoontirSageGreen,
    brandSecondary = Color(0xFFC5D196),
    brandTertiary = Color(0x26D8E2AE),
    moonGlow = MoontirSageGreen,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    divider = DarkDivider,
    success = StatusSuccess,
    warning = StatusWarning,
    error = StatusError,
    muted = MutedText
)

val LightCustomColors = MoontirCustomColors(
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceSecondary = LightSurfaceSecondary,
    onSurfaceSecondary = LightOnSurfaceSecondary,
    surfaceTertiary = LightSurfaceTertiary,
    onSurfaceTertiary = LightOnSurfaceTertiary,
    brand = MoontirDarkEspresso,
    onBrand = MoontirWarmIvory,
    brandPrimary = MoontirDarkEspresso,
    brandSecondary = Color(0xFF5A524C),
    brandTertiary = Color(0x26403A35),
    moonGlow = Color(0xFF5A6635),
    border = LightBorder,
    borderStrong = LightBorderStrong,
    divider = LightDivider,
    success = StatusSuccess,
    warning = StatusWarning,
    error = StatusError,
    muted = Color(0xFF8A827A)
)

val LocalMoontirColors = staticCompositionLocalOf { DarkCustomColors }

private val DarkColorScheme = darkColorScheme(
    primary = MoontirSageGreen,
    onPrimary = DarkSurface,
    primaryContainer = DarkSurfaceSecondary,
    onPrimaryContainer = MoontirSageGreen,
    secondary = Color(0xFFC5D196),
    onSecondary = DarkSurface,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceSecondary,
    onSurfaceVariant = DarkOnSurfaceSecondary,
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MoontirDarkEspresso,
    onPrimary = MoontirWarmIvory,
    primaryContainer = LightSurfaceSecondary,
    onPrimaryContainer = MoontirDarkEspresso,
    secondary = Color(0xFF5A524C),
    onSecondary = MoontirWarmIvory,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightOnSurfaceSecondary,
    error = StatusError,
    onError = Color.White
)

object MoontirTheme {
    val colors: MoontirCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMoontirColors.current
}

@Composable
fun MoontirAppTheme(
    themeMode: String = "system", // "system", "dark", "light"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val customColors = if (isDark) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(LocalMoontirColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoontirTypography,
            content = content
        )
    }
}
