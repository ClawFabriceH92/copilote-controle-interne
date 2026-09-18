package fr.fh.copiloteci

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Charte graphique reprise de la CNCC : bleu #004787, terracotta #D66C54, gris froids. */
object Charte {
    val bleu = Color(0xFF004787)
    val bleuClair = Color(0xFFD5E0ED)
    val bleuMoyen = Color(0xFF4060AF)
    val terracotta = Color(0xFFD66C54)
    val terracottaClair = Color(0xFFF6E3DE)
    val encre = Color(0xFF1F2328)
    val ardoise = Color(0xFF495672)
    val grisBord = Color(0xFFD0D7DE)
    val grisFond = Color(0xFFF6F8FA)
    val vertFond = Color(0xFFE7F4EA)
    val vert = Color(0xFF116329)
    val ambreFond = Color(0xFFFDF3DD)
    val ambre = Color(0xFF8A5A00)
    val rouge = Color(0xFFB3261E)
}

private val clair = lightColorScheme(
    primary = Charte.bleu,
    onPrimary = Color.White,
    primaryContainer = Charte.bleuClair,
    onPrimaryContainer = Charte.bleu,
    secondary = Charte.bleuMoyen,
    onSecondary = Color.White,
    tertiary = Charte.terracotta,
    onTertiary = Color.White,
    tertiaryContainer = Charte.terracottaClair,
    onTertiaryContainer = Charte.terracotta,
    background = Charte.grisFond,
    onBackground = Charte.encre,
    surface = Color.White,
    onSurface = Charte.encre,
    surfaceVariant = Charte.grisFond,
    onSurfaceVariant = Charte.ardoise,
    outline = Charte.grisBord,
    error = Charte.rouge,
    onError = Color.White
)

private val sombre = darkColorScheme(
    primary = Color(0xFF9CC0E8),
    onPrimary = Color(0xFF00274F),
    primaryContainer = Color(0xFF00325F),
    onPrimaryContainer = Color(0xFFD5E0ED),
    secondary = Color(0xFFA8BCE4),
    tertiary = Color(0xFFE9A08C),
    background = Color(0xFF11161C),
    surface = Color(0xFF171D24),
    onSurface = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFFB6C0CE),
    outline = Color(0xFF3A434E)
)

@Composable
fun ThemeCopilote(contenu: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) sombre else clair,
        typography = Typography(),
        content = contenu
    )
}
