package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BentoEmeraldPrimaryDark,
    onPrimary = BentoEmeraldOnContainerDark,
    primaryContainer = BentoEmeraldContainerDark,
    onPrimaryContainer = BentoEmeraldOnContainerDark,
    secondary = BentoBluePrimaryDark,
    secondaryContainer = BentoBlueContainerDark,
    onSecondaryContainer = BentoBlueOnContainerDark,
    background = BentoBackgroundDark,
    surface = BentoSurfaceDark,
    surfaceVariant = BentoSurfaceVariantDark,
    outline = BentoBorderDark,
    onBackground = BentoOnSurfaceDark,
    onSurface = BentoOnSurfaceDark,
    onSurfaceVariant = BentoOnSurfaceVariantDark,
    error = NegativeExpense
)

private val LightColorScheme = lightColorScheme(
    primary = BentoEmeraldPrimary,
    onPrimary = BentoEmeraldOnPrimary,
    primaryContainer = BentoEmeraldContainer,
    onPrimaryContainer = BentoEmeraldOnContainer,
    secondary = BentoBluePrimary,
    secondaryContainer = BentoBlueContainer,
    onSecondaryContainer = BentoBlueOnContainer,
    background = BentoBackgroundLight,
    surface = BentoSurfaceLight,
    surfaceVariant = BentoSurfaceVariantLight,
    outline = BentoBorderLight,
    onBackground = BentoOnSurfaceLight,
    onSurface = BentoOnSurfaceLight,
    onSurfaceVariant = BentoOnSurfaceVariantLight,
    error = NegativeExpense
)

@Composable
fun FinanceFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional Bento aesthetic colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

