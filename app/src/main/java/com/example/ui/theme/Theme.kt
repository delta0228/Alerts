package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkOledColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0F2B3E),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF072E21),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = DarkOledBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF161F33)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = NeonGreen,
    onSecondary = Color.Black,
    background = Color(0xFF04060A),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0D121F),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141C2E),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF222F48),
    outlineVariant = Color(0xFF161F33)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek financial OLED dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkOledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

