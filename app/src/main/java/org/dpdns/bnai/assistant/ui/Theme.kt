package org.dpdns.bnai.assistant.ui
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val BnaiColorScheme = darkColorScheme(primary = Color(0xFF3B82F6), onPrimary = Color.White, secondary = Color(0xFF10B981), background = Color(0xFF0F172A), surface = Color(0xFF1E293B), onBackground = Color(0xFFF8FAFC), error = Color(0xFFEF4444))
@Composable fun BnaiTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = BnaiColorScheme, content = content) }
