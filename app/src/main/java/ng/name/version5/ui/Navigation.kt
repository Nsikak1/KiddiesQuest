package ng.name.version5.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import ng.name.version5.core.Difficulty
import ng.name.version5.core.PlayMode

sealed interface Screen {
    data object Home : Screen
    data object SwordSetup : Screen
    data class SwordPlay(val mode: PlayMode, val difficulty: Difficulty) : Screen
    data object Reader : Screen
    data object Settings : Screen
}

/** A tiny back stack. No navigation library needed. */
@Composable
fun KidsApp() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    when (val screen = stack.last()) {
        Screen.Home -> HomeScreen(
            onSword = { push(Screen.SwordSetup) },
            onReader = { push(Screen.Reader) },
            onSettings = { push(Screen.Settings) },
        )
        Screen.SwordSetup -> SwordSetupScreen(
            onBack = { pop() },
            onStart = { mode, difficulty -> push(Screen.SwordPlay(mode, difficulty)) },
        )
        is Screen.SwordPlay -> SwordPlayScreen(
            mode = screen.mode,
            difficulty = screen.difficulty,
            onExit = { pop() },
            onHome = { while (stack.size > 1) pop() },
        )
        Screen.Reader -> BibleReaderScreen(startRef = null, onBack = { pop() })
        Screen.Settings -> SettingsScreen(onBack = { pop() })
    }
}
