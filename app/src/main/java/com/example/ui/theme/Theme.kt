package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ChaatTerracottaPrimary,
    onPrimary = ChaatTerracottaOnPrimary,
    primaryContainer = ChaatTerracottaContainer,
    onPrimaryContainer = ChaatTerracottaOnContainer,
    secondary = ChaatAmberSecondary,
    onSecondary = ChaatAmberOnSecondary,
    secondaryContainer = ChaatAmberContainer,
    onSecondaryContainer = ChaatAmberOnContainer,
    tertiary = ChaatMintGreenTertiary,
    onTertiary = ChaatMintGreenOnTertiary,
    tertiaryContainer = ChaatMintGreenContainer,
    onTertiaryContainer = ChaatMintGreenOnContainer,
    background = ChaatLightBackground,
    surface = ChaatLightSurface,
    surfaceVariant = ChaatLightSurfaceVariant,
    onBackground = ChaatLightOnSurface,
    onSurface = ChaatLightOnSurface,
    onSurfaceVariant = ChaatLightOnSurfaceVariant,
    outline = ChaatLightOutline,
    outlineVariant = ChaatLightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = ChaatDarkPrimary,
    onPrimary = ChaatDarkOnPrimary,
    primaryContainer = ChaatDarkPrimaryContainer,
    onPrimaryContainer = ChaatDarkOnPrimaryContainer,
    secondary = ChaatDarkSecondary,
    onSecondary = ChaatDarkOnSecondary,
    secondaryContainer = ChaatDarkSecondaryContainer,
    onSecondaryContainer = ChaatDarkOnSecondaryContainer,
    tertiary = ChaatDarkTertiary,
    onTertiary = ChaatDarkOnTertiary,
    tertiaryContainer = ChaatDarkTertiaryContainer,
    onTertiaryContainer = ChaatDarkOnTertiaryContainer,
    background = ChaatDarkBackground,
    surface = ChaatDarkSurface,
    surfaceVariant = ChaatDarkSurfaceVariant,
    onBackground = ChaatDarkOnSurface,
    onSurface = ChaatDarkOnSurface,
    onSurfaceVariant = ChaatDarkOnSurfaceVariant,
    outline = ChaatDarkOutline,
    outlineVariant = ChaatDarkOutlineVariant,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
