package org.dpdns.bnai.assistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BnaiPrimaryBg = Color(0xFF0B1020)
val BnaiSecondaryBg = Color(0xFF151B2D)
val BnaiElevatedSurface = Color(0xFF20283D)
val BnaiPrimaryAccent = Color(0xFF7C8CFF)
val BnaiSecondaryAccent = Color(0xFF29C7C7)
val BnaiMainText = Color(0xFFF5F7FF)
val BnaiSecondaryText = Color(0xFFA8B2C8)
val BnaiSuccess = Color(0xFF39C98A)
val BnaiWarning = Color(0xFFF5B84B)
val BnaiError = Color(0xFFF16C7B)
val BnaiBorder = Color(0xFF303A52)

private val BnaiV11ColorScheme = darkColorScheme(
    primary = BnaiPrimaryAccent, onPrimary = Color.White, secondary = BnaiSecondaryAccent,
    tertiary = BnaiWarning, background = BnaiPrimaryBg, surface = BnaiSecondaryBg,
    surfaceVariant = BnaiElevatedSurface, onBackground = BnaiMainText, onSurface = BnaiMainText,
    onSurfaceVariant = BnaiSecondaryText, error = BnaiError, outline = BnaiBorder
)

@Composable
fun BnaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BnaiV11ColorScheme, content = content)
}
