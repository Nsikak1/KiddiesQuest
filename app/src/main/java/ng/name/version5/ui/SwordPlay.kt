package ng.name.version5.ui

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import ng.name.version5.core.BibleIndex
import ng.name.version5.core.Difficulty
import ng.name.version5.core.GameRules
import ng.name.version5.core.HintMode
import ng.name.version5.core.Mission
import ng.name.version5.core.Outcome
import ng.name.version5.core.PlayMode
import ng.name.version5.core.RoundClock
import ng.name.version5.core.RoundResult
import ng.name.version5.core.Scoring
import ng.name.version5.core.Testament
import ng.name.version5.core.VersePicker
import ng.name.version5.core.VerseRef
import ng.name.version5.data.LocalServices
import ng.name.version5.data.Sfx
import ng.name.version5.data.Sound
import ng.name.version5.ui.theme.Cherry
import ng.name.version5.ui.theme.CherryLip
import ng.name.version5.ui.theme.Cream
import ng.name.version5.ui.theme.Gold
import ng.name.version5.ui.theme.GoldLip
import ng.name.version5.ui.theme.Grape
import ng.name.version5.ui.theme.GrapeLip
import ng.name.version5.ui.theme.Ink
import ng.name.version5.ui.theme.Mint
import ng.name.version5.ui.theme.MintLip
import ng.name.version5.ui.theme.Orange
import ng.name.version5.ui.theme.OrangeLip
import ng.name.version5.ui.theme.Pink
import ng.name.version5.ui.theme.PinkLip
import ng.name.version5.ui.theme.Sky
import ng.name.version5.ui.theme.SkyLip

private enum class Stage { LOGO, PLAY, RESULT, SUMMARY }
private enum class Verdict { PENDING, RIGHT, WRONG }

/**
 * One mission of five verses.
 *
 * Flow per round: logo -> reference (+ countdown on Easy) -> timed hunt -> result with the verse text.
 * Physical mode: the child hunts in a real Bible and taps "I found it!"; then confirms what they saw.
 * In-app mode: the child opens the verse in the in-app Bible; the clock stops on the right verse.
 */
@Composable
fun SwordPlayScreen(mode: PlayMode, difficulty: Difficulty, onExit: () -> Unit, onHome: () -> Unit) {
    val services = LocalServices.current
    val settings = services.settings
    val sfx = services.sfx
    val bibleId = remember { settings.bibleId }
    val index = remember { services.bibles.index(bibleId) }
    val mission = remember { Mission(difficulty, mode) }
    val picker = remember { VersePicker(index) }

    var roundNo by remember { mutableIntStateOf(1) }
    var ref by remember { mutableStateOf(picker.next(difficulty)) }
    var stage by remember { mutableStateOf(Stage.LOGO) }
    var runKey by remember { mutableIntStateOf(0) }
    var showLeave by remember { mutableStateOf(false) }
    var readerRef by remember { mutableStateOf<VerseRef?>(null) }

    val countdownMs = if (difficulty.defaultCountdownSec > 0) settings.countdownSec * 1000L else 0L
    val clock = remember(runKey) { RoundClock(countdownMs) }
    var clockState by remember(runKey) { mutableStateOf(RoundClock.State.READY) }
    var nowMs by remember(runKey) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var hintShown by remember(runKey) { mutableStateOf(difficulty.hintMode == HintMode.ALWAYS_SHOWN) }
    var hintUsed by remember(runKey) { mutableStateOf(false) }
    var wrongGuesses by remember(runKey) { mutableIntStateOf(0) }
    var outcome by remember(runKey) { mutableStateOf(Outcome.FOUND) }
    var resultMs by remember(runKey) { mutableLongStateOf(0L) }
    var stars by remember(runKey) { mutableIntStateOf(0) }
    var verdict by remember(runKey) { mutableStateOf(Verdict.PENDING) }
    var committed by remember(runKey) { mutableStateOf(false) }
    var flash by remember(runKey) { mutableStateOf<String?>(null) }

    fun commit() {
        mission.record(RoundResult(ref, outcome, resultMs, stars, hintUsed, wrongGuesses))
        services.progress.recordRound(difficulty, mode, stars, resultMs)
        committed = true
        sfx.play(if (stars > 0) Sound.WIN else Sound.WRONG)
    }

    fun finish(result: Outcome, timeMs: Long) {
        outcome = result
        resultMs = timeMs
        if (result == Outcome.FOUND) {
            if (mode == PlayMode.IN_APP) {
                stars = Scoring.starsFor(difficulty, timeMs, hintUsed)
                commit()
            }
            // Physical mode: stars are given after the child confirms below.
        } else {
            stars = 0
            commit()
        }
        stage = Stage.RESULT
    }

    fun confirm(right: Boolean) {
        verdict = if (right) Verdict.RIGHT else Verdict.WRONG
        if (right) {
            stars = Scoring.starsFor(difficulty, resultMs, hintUsed)
        } else {
            outcome = Outcome.WRONG_PLACE
            stars = 0
        }
        commit()
    }

    fun nextRound() {
        if (mission.isComplete) {
            services.progress.recordMission()
            stage = Stage.SUMMARY
        } else {
            roundNo += 1
            ref = picker.next(difficulty)
            runKey += 1
            stage = Stage.LOGO
        }
    }

    fun onChosen(choice: VerseRef) {
        if (choice == ref) {
            finish(Outcome.FOUND, clock.stop(SystemClock.elapsedRealtime()))
        } else {
            wrongGuesses += 1
            clock.addPenalty(GameRules.WRONG_GUESS_PENALTY_MS)
            sfx.play(Sound.WRONG)
            flash = "Not that one! +${GameRules.WRONG_GUESS_PENALTY_MS / 1000} seconds"
        }
    }

    // The clock. Runs only while a round is being played.
    LaunchedEffect(stage, runKey) {
        if (stage != Stage.PLAY) return@LaunchedEffect
        clock.start(SystemClock.elapsedRealtime())
        clockState = clock.state
        if (clock.state == RoundClock.State.HUNTING) sfx.play(Sound.GO)
        var lastSecond = -1
        while (true) {
            val now = SystemClock.elapsedRealtime()
            val before = clock.state
            val after = clock.tick(now)
            nowMs = now
            clockState = after
            if (before == RoundClock.State.COUNTDOWN && after == RoundClock.State.HUNTING) sfx.play(Sound.GO)
            if (after == RoundClock.State.COUNTDOWN) {
                val left = clock.countdownSecondsLeft(now)
                if (left != lastSecond) {
                    lastSecond = left
                    if (left in 1..3) sfx.play(Sound.TICK)
                }
            }
            if (after == RoundClock.State.TIME_UP) {
                finish(Outcome.TIME_UP, clock.elapsedMs(now))
                break
            }
            if (after == RoundClock.State.STOPPED) break
            delay(50)
        }
    }

    LaunchedEffect(flash) {
        if (flash != null) {
            delay(1600)
            flash = null
        }
    }

    // No cheating: leaving the app mid-round restarts that round from the logo.
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && stage == Stage.PLAY) {
                runKey += 1
                stage = Stage.LOGO
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    BackHandler(enabled = stage != Stage.SUMMARY) { showLeave = true }

    val reading = readerRef
    if (reading != null) {
        BibleReaderScreen(startRef = reading, onBack = { readerRef = null })
    } else {
        GameBackground {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
                when (stage) {
                    Stage.LOGO -> LogoStage(roundNo, mission.rounds, runKey, sfx) { stage = Stage.PLAY }
                    Stage.PLAY -> PlayStage(
                        label = index.label(ref),
                        testament = index.book(ref.book).testament,
                        roundNo = roundNo,
                        rounds = mission.rounds,
                        difficulty = difficulty,
                        mode = mode,
                        index = index,
                        clock = clock,
                        clockState = clockState,
                        getNow = { nowMs },
                        hintShown = hintShown,
                        onHint = {
                            hintShown = true
                            hintUsed = true
                        },
                        flash = flash,
                        onFound = { finish(Outcome.FOUND, clock.stop(SystemClock.elapsedRealtime())) },
                        onGiveUp = { finish(Outcome.GAVE_UP, clock.stop(SystemClock.elapsedRealtime())) },
                        onChosen = { onChosen(it) },
                    )
                    Stage.RESULT -> ResultStage(
                        ref = ref,
                        index = index,
                        bibleId = bibleId,
                        outcome = outcome,
                        mode = mode,
                        timeMs = resultMs,
                        stars = stars,
                        wrongGuesses = wrongGuesses,
                        committed = committed,
                        verdict = verdict,
                        isLast = mission.results.size + (if (committed) 0 else 1) >= mission.rounds,
                        onConfirm = { confirm(it) },
                        onRead = { readerRef = ref },
                        onNext = { nextRound() },
                    )
                    Stage.SUMMARY -> SummaryStage(mission, index, onExit, onHome)
                }
            }
        }
    }

    if (showLeave) {
        ConfirmDialog(
            title = "Leave the mission?",
            message = "Stars you already won are kept.",
            confirmText = "Leave",
            cancelText = "Stay",
            onConfirm = {
                showLeave = false
                onExit()
            },
            onDismiss = { showLeave = false },
        )
    }
}

@Composable
private fun LogoStage(roundNo: Int, rounds: Int, runKey: Int, sfx: Sfx, onDone: () -> Unit) {
    LaunchedEffect(runKey) {
        sfx.play(Sound.DRAW)
        delay(1600)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SwordLogo(animateIn = true)
            Spacer(Modifier.height(12.dp))
            GameText("Verse $roundNo of $rounds", size = 24.sp, color = Cream)
        }
    }
}

@Composable
private fun PlayStage(
    label: String,
    testament: Testament,
    roundNo: Int,
    rounds: Int,
    difficulty: Difficulty,
    mode: PlayMode,
    index: BibleIndex,
    clock: RoundClock,
    clockState: RoundClock.State,
    getNow: () -> Long,
    hintShown: Boolean,
    onHint: () -> Unit,
    flash: String?,
    onFound: () -> Unit,
    onGiveUp: () -> Unit,
    onChosen: (VerseRef) -> Unit,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText("Verse $roundNo of $rounds  -  ${difficulty.label}  -  ${mode.label}", size = 16.sp, color = Cream)
        Spacer(Modifier.height(8.dp))

        CandyCard(Modifier.fillMaxWidth()) {
            GameText(label, size = 46.sp, color = Ink, shadow = false, maxLines = 2, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))

        if (hintShown) {
            ChunkySurface(
                face = if (testament == Testament.OLD) Sky else Orange,
                lip = if (testament == Testament.OLD) SkyLip else OrangeLip,
                corner = 16.dp,
            ) {
                GameText(testament.label, size = 22.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            }
        } else if (difficulty.hintMode == HintMode.TAP_COSTS_STAR && clockState == RoundClock.State.HUNTING) {
            ChunkyButton("Need a hint?  (-1 star)", Grape, GrapeLip, size = 18.sp, onClick = onHint)
        }

        when (clockState) {
            RoundClock.State.COUNTDOWN -> Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GameText("Get ready!", size = 34.sp, color = Cream)
                CountdownNumber(clock, getNow)
                GameText("Think where it is...", size = 20.sp, color = Cream)
            }

            RoundClock.State.HUNTING -> if (mode == PlayMode.PHYSICAL) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TimerText(clock, difficulty, getNow, 84.sp)
                    GameText("Find it in your Bible!", size = 24.sp, color = Cream)
                    Spacer(Modifier.height(24.dp))
                    ChunkyButton(
                        text = "I found it!",
                        face = Mint,
                        lip = MintLip,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        size = 40.sp,
                        onClick = onFound,
                    )
                }
                GiveUpButton(onGiveUp)
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { TimerText(clock, difficulty, getNow, 40.sp) }
                        if (flash != null) {
                            GameText(flash, size = 18.sp, color = Cherry, modifier = Modifier.weight(2f))
                        }
                    }
                    BibleNavigator(index, difficulty, Modifier.weight(1f), onVerseChosen = onChosen)
                }
                GiveUpButton(onGiveUp)
            }

            else -> Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun GiveUpButton(onClick: () -> Unit) {
    ChunkyButton("Show me", Cherry, CherryLip, Modifier.padding(top = 8.dp), size = 16.sp, onClick = onClick)
}

// These two read the fast-changing time themselves, so only they redraw 20 times a second.
@Composable
private fun CountdownNumber(clock: RoundClock, getNow: () -> Long) {
    GameText(clock.countdownSecondsLeft(getNow()).toString(), size = 130.sp, color = Gold)
}

@Composable
private fun TimerText(clock: RoundClock, difficulty: Difficulty, getNow: () -> Long, size: TextUnit) {
    val ms = clock.elapsedMs(getNow())
    val seconds = ms / 1000.0
    val colour = when {
        seconds <= difficulty.threeStarSec -> Mint
        seconds <= difficulty.twoStarSec -> Gold
        else -> Cherry
    }
    GameText("${formatSeconds(ms)}s", size = size, color = colour)
}

@Composable
private fun ResultStage(
    ref: VerseRef,
    index: BibleIndex,
    bibleId: String,
    outcome: Outcome,
    mode: PlayMode,
    timeMs: Long,
    stars: Int,
    wrongGuesses: Int,
    committed: Boolean,
    verdict: Verdict,
    isLast: Boolean,
    onConfirm: (Boolean) -> Unit,
    onRead: () -> Unit,
    onNext: () -> Unit,
) {
    val services = LocalServices.current
    val passage by produceState(initialValue = "", ref, bibleId) {
        value = try {
            services.bibles.verse(bibleId, ref)
        } catch (e: Exception) {
            ""
        }
    }
    val pendingCheck = mode == PlayMode.PHYSICAL && outcome == Outcome.FOUND && verdict == Verdict.PENDING

    val headline = when {
        pendingCheck -> "Stopped at ${formatSeconds(timeMs)}s"
        outcome == Outcome.TIME_UP -> "Time's up!"
        outcome == Outcome.GAVE_UP -> "Here it is"
        outcome == Outcome.WRONG_PLACE -> "Keep practising!"
        stars == 3 -> "Brilliant!"
        stars == 2 -> "Well done!"
        else -> "You found it!"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText(headline, size = 40.sp, color = Gold)
        Spacer(Modifier.height(8.dp))
        if (!pendingCheck) {
            StarRow(lit = stars, starSize = 64.dp)
            if (outcome == Outcome.FOUND || outcome == Outcome.WRONG_PLACE) {
                GameText("Your time: ${formatSeconds(timeMs)}s", size = 24.sp, color = Cream)
            }
            if (wrongGuesses > 0) {
                GameText("Wrong tries: $wrongGuesses", size = 18.sp, color = Cream)
            }
        }
        Spacer(Modifier.height(12.dp))

        CandyCard(Modifier.fillMaxWidth()) {
            GameText(index.label(ref), size = 30.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            Text(
                text = if (passage.isEmpty()) "..." else passage,
                color = Ink,
                fontSize = 22.sp,
                lineHeight = 32.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = index.name,
                color = Ink.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(14.dp))

        if (pendingCheck) {
            GameText("Check your Bible. Is this the verse you found?", size = 22.sp, color = Cream)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("Yes!", Mint, MintLip, Modifier.weight(1f), size = 26.sp, onClick = { onConfirm(true) })
                ChunkyButton("Not quite", Cherry, CherryLip, Modifier.weight(1f), size = 22.sp, onClick = { onConfirm(false) })
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("Read chapter", Sky, SkyLip, Modifier.weight(1f), size = 20.sp, onClick = onRead)
                ChunkyButton(
                    text = if (isLast) "See results" else "Next verse",
                    face = Pink,
                    lip = PinkLip,
                    modifier = Modifier.weight(1f),
                    size = 22.sp,
                    enabled = committed,
                    onClick = onNext,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryStage(mission: Mission, index: BibleIndex, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val progress = LocalServices.current.progress
    val rank = progress.rank
    val next = rank.next

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText("Mission complete!", size = 38.sp, color = Gold)
        Spacer(Modifier.height(6.dp))
        GameText("${mission.totalStars} of ${mission.maxStars} stars", size = 28.sp)
        mission.bestTimeMs?.let { GameText("Fastest find: ${formatSeconds(it)}s", size = 20.sp, color = Cream) }
        Spacer(Modifier.height(12.dp))

        CandyCard(Modifier.fillMaxWidth()) {
            GameText("Rank: ${rank.title}", size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            val toGo = if (next != null) "${next.minStars - progress.totalStars} more stars to become a ${next.title}" else "You are a Champion!"
            Text(toGo, color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))

        CandyCard(Modifier.fillMaxWidth()) {
            for (result in mission.results) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(index.label(result.ref), color = Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    val timeText = if (result.stars > 0) "${formatSeconds(result.timeMs)}s" else "-"
                    Text(timeText, color = Ink, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    StarRow(lit = result.stars, starSize = 22.dp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton("Home", Sky, SkyLip, Modifier.weight(1f), size = 22.sp, onClick = onHome)
            ChunkyButton("Play again", Pink, PinkLip, Modifier.weight(1f), size = 22.sp, onClick = onPlayAgain)
        }
    }
}
