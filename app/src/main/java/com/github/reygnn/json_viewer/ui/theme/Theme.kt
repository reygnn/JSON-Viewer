package com.github.reygnn.json_viewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.github.reygnn.json_viewer.domain.model.TokenType

private val LightColors = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    inversePrimary = md_light_inversePrimary,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary,
    onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    error = md_light_error,
    onError = md_light_onError,
    errorContainer = md_light_errorContainer,
    onErrorContainer = md_light_onErrorContainer,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    surfaceTint = md_light_surfaceTint,
    surfaceBright = md_light_surfaceBright,
    surfaceDim = md_light_surfaceDim,
    surfaceContainerLowest = md_light_surfaceContainerLowest,
    surfaceContainerLow = md_light_surfaceContainerLow,
    surfaceContainer = md_light_surfaceContainer,
    surfaceContainerHigh = md_light_surfaceContainerHigh,
    surfaceContainerHighest = md_light_surfaceContainerHighest,
    inverseSurface = md_light_inverseSurface,
    inverseOnSurface = md_light_inverseOnSurface,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    scrim = md_light_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    inversePrimary = md_dark_inversePrimary,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    surfaceTint = md_dark_surfaceTint,
    surfaceBright = md_dark_surfaceBright,
    surfaceDim = md_dark_surfaceDim,
    surfaceContainerLowest = md_dark_surfaceContainerLowest,
    surfaceContainerLow = md_dark_surfaceContainerLow,
    surfaceContainer = md_dark_surfaceContainer,
    surfaceContainerHigh = md_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_dark_surfaceContainerHighest,
    inverseSurface = md_dark_inverseSurface,
    inverseOnSurface = md_dark_inverseOnSurface,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant,
    scrim = md_dark_scrim,
)

/**
 * Syntax-highlighting colours, one per [TokenType]. Held outside the M3
 * ColorScheme (there is no M3 role for "JSON string") and exposed through
 * [LocalJsonTokenColors]. See MATERIAL3RULES B1.
 */
data class JsonTokenColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val nul: Color,
    val punctuation: Color,
) {
    fun forType(type: TokenType): Color = when (type) {
        TokenType.KEY -> key
        TokenType.STRING -> string
        TokenType.NUMBER -> number
        TokenType.BOOLEAN -> boolean
        TokenType.NULL -> nul
        TokenType.PUNCTUATION -> punctuation
    }
}

private val LightTokenColors = JsonTokenColors(
    key = json_light_key,
    string = json_light_string,
    number = json_light_number,
    boolean = json_light_boolean,
    nul = json_light_null,
    punctuation = json_light_punctuation,
)

private val DarkTokenColors = JsonTokenColors(
    key = json_dark_key,
    string = json_dark_string,
    number = json_dark_number,
    boolean = json_dark_boolean,
    nul = json_dark_null,
    punctuation = json_dark_punctuation,
)

/** Provides the active [JsonTokenColors] down the tree. Set by [JsonViewerTheme]. */
val LocalJsonTokenColors = staticCompositionLocalOf { LightTokenColors }

/**
 * App theme. Static teal brand scheme (no dynamic colour — see
 * MATERIAL3RULES B3) plus the syntax-highlighting palette.
 */
@Composable
fun JsonViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val tokenColors = if (darkTheme) DarkTokenColors else LightTokenColors
    CompositionLocalProvider(LocalJsonTokenColors provides tokenColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
