package com.atulpandey.clearwrite.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = EditorialIndigoDark, onPrimary = Color(0xFF17235E),
  primaryContainer = Color(0xFF2B376F), onPrimaryContainer = Color(0xFFDDE1FF),
  secondary = Color(0xFFC4C6D3), onSecondary = Color(0xFF2D3038),
  secondaryContainer = Color(0xFF3F424A), onSecondaryContainer = Color(0xFFE0E2EE),
  background = DarkPaper, onBackground = DarkInk, surface = DarkSurface, onSurface = DarkInk,
  surfaceVariant = DarkRaised, onSurfaceVariant = DarkMutedInk,
  outline = Color(0xFF918F88), outlineVariant = Color(0xFF494843),
  error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
  surfaceContainerLowest = Color(0xFF11110F), surfaceContainerLow = DarkSurface,
  surfaceContainer = DarkRaised, surfaceContainerHigh = Color(0xFF2C2C28),
  surfaceContainerHighest = Color(0xFF34342F),
)

private val LightColorScheme = lightColorScheme(
  primary = EditorialIndigo, onPrimary = Color.White,
  primaryContainer = IssueContainer, onPrimaryContainer = Color(0xFF242E65),
  secondary = Color(0xFF5C5F6C), onSecondary = Color.White,
  secondaryContainer = Color(0xFFE1E2EC), onSecondaryContainer = Color(0xFF444650),
  background = Paper, onBackground = Ink, surface = PaperRaised, onSurface = Ink,
  surfaceVariant = SoftSurface, onSurfaceVariant = MutedInk,
  outline = Color(0xFF7B7973), outlineVariant = SoftOutline,
  error = Color(0xFFBA1A1A), onError = Color.White,
  errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
  surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8F5F0),
  surfaceContainer = SoftSurface, surfaceContainerHigh = Color(0xFFEBE8E2),
  surfaceContainerHighest = Color(0xFFE5E2DC),
)

@Immutable
data class ClearWriteSemanticColors(
  val success: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val issueContainer: Color,
  val aiAccent: Color,
  val aiContainer: Color,
  val onAiContainer: Color,
)

private val LocalSemanticColors = staticCompositionLocalOf {
  ClearWriteSemanticColors(
    Success,
    SuccessContainer,
    OnSuccessContainer,
    Warning,
    WarningContainer,
    OnWarningContainer,
    IssueContainer,
    AiAccent,
    AiContainer,
    OnAiContainer,
  )
}

object ClearWriteThemeTokens {
  val colors: ClearWriteSemanticColors @Composable get() = LocalSemanticColors.current
}

@Composable
fun ClearWriteTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val semanticColors = if (darkTheme) {
    ClearWriteSemanticColors(
      SuccessDark,
      SuccessContainerDark,
      Color(0xFFD7F6E2),
      Color(0xFFFFC66A),
      Color(0xFF553A10),
      Color(0xFFFFE8BF),
      IssueContainerDark,
      AiAccentDark,
      AiContainerDark,
      OnAiContainerDark,
    )
  } else {
    ClearWriteSemanticColors(
      Success,
      SuccessContainer,
      OnSuccessContainer,
      Warning,
      WarningContainer,
      OnWarningContainer,
      IssueContainer,
      AiAccent,
      AiContainer,
      OnAiContainer,
    )
  }
  androidx.compose.runtime.CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = ClearWriteShapes,
      content = content,
    )
  }
}
