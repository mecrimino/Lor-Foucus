package com.lorfocus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/** The design system's palette, carried through a CompositionLocal so screens read it directly. */
data class LorColors(
    val paper: Color, val card: Color, val ink: Color, val muted: Color, val faint: Color,
    val line: Color, val focus: Color, val primary: Color, val onPrimary: Color,
    val quiet: Color, val chip: Color, val hair: Color, val logoPaper: Color, val dark: Boolean,
)

val LightColors = LorColors(
    paper = Color(0xFFFAF9F6), card = Color(0xFFF6F4EE), ink = Color(0xFF1A1A18),
    muted = Color(0xFF7A756C), faint = Color(0xFF9A9488), line = Color(0x141A1A18),
    focus = Color(0xFF4A6B57), primary = Color(0xFF3E5B49), onPrimary = Color(0xFFF7F5F0),
    quiet = Color(0xFFB0765E), chip = Color(0xFFEAE7DF), hair = Color(0x291A1A18),
    logoPaper = Color(0xFFFAF9F6), dark = false,
)

val DarkColors = LorColors(
    paper = Color(0xFF1A1A18), card = Color(0xFF211F1C), ink = Color(0xFFF2F0EA),
    muted = Color(0xFF9A958B), faint = Color(0xFF7D786F), line = Color(0x1AF2F0EA),
    focus = Color(0xFF86A992), primary = Color(0xFF8FB49B), onPrimary = Color(0xFF14170F),
    quiet = Color(0xFFC89279), chip = Color(0xFF2B2825), hair = Color(0x33F2F0EA),
    logoPaper = Color(0xFF1A1A18), dark = true,
)

val LocalLorColors = staticCompositionLocalOf { LightColors }

/** Editorial serif for headings and numbers (drop Instrument Serif TTFs into res/font to match exactly). */
val Serif = TextStyle(fontFamily = FontFamily.Serif)

@Composable
fun LorFocusTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark)
        darkColorScheme(background = c.paper, surface = c.card, primary = c.primary, onPrimary = c.onPrimary, onBackground = c.ink, onSurface = c.ink)
    else
        lightColorScheme(background = c.paper, surface = c.card, primary = c.primary, onPrimary = c.onPrimary, onBackground = c.ink, onSurface = c.ink)
    CompositionLocalProvider(LocalLorColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
