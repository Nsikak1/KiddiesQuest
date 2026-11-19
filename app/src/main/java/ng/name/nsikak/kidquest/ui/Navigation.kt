package ng.name.nsikak.kidquest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.DrillType
import ng.name.nsikak.kidquest.core.PlayMode
import ng.name.nsikak.kidquest.core.VerseRef

sealed interface Screen {
    data object Home : Screen
    data object SwordSetup : Screen
    data class SwordPlay(
        val mode: PlayMode,
        val difficulty: Difficulty,
        val drill: DrillType,
        val daily: Boolean = false,
    ) : Screen
    data object Daily : Screen
    data object Trainer : Screen
    data object Stories : Screen
    data class StoryPage(val id: Int) : Screen
    data object Progress : Screen
    data object Reader : Screen
    data class ReaderAt(val ref: VerseRef) : Screen
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
            onDaily = { push(Screen.Daily) },
            onTrainer = { push(Screen.Trainer) },
            onStories = { push(Screen.Stories) },
            onProgress = { push(Screen.Progress) },
            onReader = { push(Screen.Reader) },
            onSettings = { push(Screen.Settings) },
        )
        Screen.SwordSetup -> SwordSetupScreen(
            onBack = { pop() },
            onStart = { mode, difficulty, drill -> push(Screen.SwordPlay(mode, difficulty, drill)) },
        )
        is Screen.SwordPlay -> SwordPlayScreen(
            mode = screen.mode,
            difficulty = screen.difficulty,
            drill = screen.drill,
            daily = screen.daily,
            onExit = { pop() },
            onHome = { while (stack.size > 1) pop() },
        )
        Screen.Daily -> DailyScreen(
            onBack = { pop() },
            onStart = { mode -> push(Screen.SwordPlay(mode, ng.name.nsikak.kidquest.core.Daily.difficulty, DrillType.VERSE, daily = true)) },
        )
        Screen.Trainer -> BookTrainerScreen(onBack = { pop() })
        Screen.Stories -> StoriesScreen(onBack = { pop() }, onOpen = { push(Screen.StoryPage(it)) })
        is Screen.StoryPage -> StoryScreen(
            id = screen.id,
            onBack = { pop() },
            onReadChapter = { push(Screen.ReaderAt(it)) },
        )
        Screen.Progress -> ProgressScreen(onBack = { pop() })
        Screen.Reader -> BibleReaderScreen(startRef = null, onBack = { pop() })
        is Screen.ReaderAt -> BibleReaderScreen(startRef = screen.ref, onBack = { pop() })
        Screen.Settings -> SettingsScreen(onBack = { pop() })
    }
}
