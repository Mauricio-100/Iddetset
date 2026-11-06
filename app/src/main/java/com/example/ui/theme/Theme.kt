package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val displayName: String) {
    DEFAULT("Système"),
    DARK("Sombre"),
    LIGHT("Clair"),
    CYBER("Cyberpunk"),
    EMERALD("Émeraude"),
    AMETHYST("Améthyste"),
    MIDNIGHT("Nuit")
}

data class AppColorScheme(
    val isDark: Boolean,
    val sidebarBackground: Color,
    val sidebarForeground: Color,
    val sidebarActive: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color
)

val LocalAppColorScheme = staticCompositionLocalOf {
    AppColorScheme(
        isDark = true,
        sidebarBackground = Color(0xFF161B22),
        sidebarForeground = Color(0xFFE6EDF3),
        sidebarActive = Color(0xFF58A6FF),
        surfaceVariant = Color(0xFF21262D),
        onSurface = Color(0xFFF0F6FC),
        onSurfaceVariant = Color(0xFF8B949E),
        error = Color(0xFFF85149),
        onError = Color.White
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1F6FEB),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF238636),
    onSecondary = Color.White,
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFC9D1D9),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    error = Color(0xFFF85149),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0969DA),
    secondary = Color(0xFF1A7F37),
    onSecondary = Color.White,
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color.White,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEAEEF2),
    onSurfaceVariant = Color(0xFF656D76),
    error = Color(0xFFCF222E),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.DEFAULT -> systemDark
        AppTheme.DARK, AppTheme.CYBER, AppTheme.MIDNIGHT -> true
        AppTheme.LIGHT -> false
        AppTheme.EMERALD, AppTheme.AMETHYST -> systemDark
    }

    val colorScheme = when {
        appTheme == AppTheme.CYBER -> DarkColorScheme.copy(
            primary = Color(0xFF00FFCC),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF005544),
            background = Color(0xFF050811),
            surface = Color(0xFF0E1424)
        )
        appTheme == AppTheme.EMERALD -> if (isDark) DarkColorScheme.copy(
            primary = Color(0xFF2ECC71),
            primaryContainer = Color(0xFF1E8449)
        ) else LightColorScheme.copy(
            primary = Color(0xFF27AE60),
            primaryContainer = Color(0xFFD5F5E3)
        )
        appTheme == AppTheme.AMETHYST -> if (isDark) DarkColorScheme.copy(
            primary = Color(0xFFBB86FC),
            primaryContainer = Color(0xFF6200EE)
        ) else LightColorScheme.copy(
            primary = Color(0xFF8E44AD),
            primaryContainer = Color(0xFFEBDEF0)
        )
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val appColors = AppColorScheme(
        isDark = isDark,
        sidebarBackground = if (isDark) colorScheme.surface else Color(0xFFF6F8FA),
        sidebarForeground = if (isDark) colorScheme.onSurface else Color(0xFF1F2328),
        sidebarActive = colorScheme.primary,
        surfaceVariant = colorScheme.surfaceVariant,
        onSurface = colorScheme.onSurface,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        error = colorScheme.error,
        onError = colorScheme.onError
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalAppColorScheme provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
