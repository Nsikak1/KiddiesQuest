package ng.name.version5.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * One fixed, bright look for the whole game. No dynamic colour and no dark mode:
 * the game has its own night-sky style, and kids should always see the same thing.
 */
private val GameColors = lightColorScheme(
    primary = Pink,
    secondary = Gold,
    tertiary = Mint,
    background = Night1,
    surface = Cream,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSurface = Ink,
)

@Composable
fun Version5Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColors,
        typography = Typography,
        content = content,
    )
}
