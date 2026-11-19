package ng.name.nsikak.kidquest.ui

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import ng.name.nsikak.kidquest.core.BibleIndex
import ng.name.nsikak.kidquest.core.Daily
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.DrillType
import ng.name.nsikak.kidquest.core.GameRules
import ng.name.nsikak.kidquest.core.HintMode
import ng.name.nsikak.kidquest.core.Memory
import ng.name.nsikak.kidquest.core.Mission
import ng.name.nsikak.kidquest.core.Outcome
import ng.name.nsikak.kidquest.core.PlayMode
import ng.name.nsikak.kidquest.core.RoundClock
import ng.name.nsikak.kidquest.core.RoundResult
import ng.name.nsikak.kidquest.core.Scoring
import ng.name.nsikak.kidquest.core.Testament
import ng.name.nsikak.kidquest.core.VersePicker
import ng.name.nsikak.kidquest.core.VerseRef
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Sfx
import ng.name.nsikak.kidquest.data.Sound
import ng.name.nsikak.kidquest.data.Speaker
import ng.name.nsikak.kidquest.data.Today
import ng.name.nsikak.kidquest.ui.theme.Cherry
import ng.name.nsikak.kidquest.ui.theme.CherryLip
import ng.name.nsikak.kidquest.ui.theme.Cream
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.OrangeLip
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip
import kotlin.random.Random

private enum class Stage { LOGO, PLAY, RESULT, MEMORY, SUMMARY }
private enum class Verdict { PENDING, RIGHT, WRONG }

/**
 * One mission of five rounds (or the single round of the daily challenge).
 *
 * Flow per round: logo -> reference (+ countdown on Easy) -> timed hunt -> result with the verse text
 * -> optional memory challenge.
 * Physical mode: the child hunts in a real Bible and taps "I found it!"; then confirms what they saw.
 * In-app mode: the child opens it in the in-app Bible; the clock stops on the right book/chapter/verse.
 */
@Composable
fun SwordPlayScreen(
    mode: PlayMode,
    difficulty: Difficulty,
    drill: DrillType,
    daily: Boolean,
    onExit: () -> Unit,
    onHome: () -> Unit,
) {
    val services = LocalServices.current
    val settings = services.settings
    val sfx = services.sfx
    val haptics = services.haptics
    val speaker = services.speaker
    val progress = services.progress

    val bibleId = remember { settings.bibleId }
    val index = remember { services.bibles.index(bibleId) }
    val hintMode = remember { settings.hintRule.resolve(difficulty) }
    val limitMs = remember { GameRules.limitMs(settings.timeLimitSec) }
    val today = remember { Today.epochDay() }
    val mission = remember { Mission(difficulty, mode, if (daily) 1 else GameRules.ROUNDS_PER_MISSION, drill) }
    val picker = remember { VersePicker(index) }
    val firstType = remember { if (daily) DrillType.VERSE else drill.concrete(Random.Default) }

    var roundNo by remember { mutableIntStateOf(1) }
    var roundType by remember { mutableStateOf(firstType) }
    var ref by remember {
        mutableStateOf(
            if (daily) Daily.verseFor(today).let { if (it in index) it else VerseRef(43, 3, 16) }
            else picker.next(difficulty, firstType),
        )
    }
    var stage by remember { mutableStateOf(Stage.LOGO) }
    var runKey by remember { mutableIntStateOf(0) }
    var showLeave by remember { mutableStateOf(false) }
    var readerRef by remember { mutableStateOf<VerseRef?>(null) }

    val countdownMs = if (difficulty.defaultCountdownSec > 0) settings.countdownSec * 1000L else 0L
    val clock = remember(runKey) { RoundClock(countdownMs, limitMs) }
    var clockState by remember(runKey) { mutableStateOf(RoundClock.State.READY) }
    var nowMs by remember(runKey) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var hintShown by remember(runKey) { mutableStateOf(hintMode == HintMode.ALWAYS_SHOWN) }
    var hintUsed by remember(runKey) { mutableStateOf(false) }
    var wrongGuesses by remember(runKey) { mutableIntStateOf(0) }
    var outcome by remember(runKey) { mutableStateOf(Outcome.FOUND) }
    var resultMs by remember(runKey) { mutableLongStateOf(0L) }
    var stars by remember(runKey) { mutableIntStateOf(0) }
    var verdict by remember(runKey) { mutableStateOf(Verdict.PENDING) }
    var committed by remember(runKey) { mutableStateOf(false) }
    var flash by remember(runKey) { mutableStateOf<String?>(null) }
    var newRecord by remember(runKey) { mutableStateOf(false) }
    var dailyStreak by remember(runKey) { mutableIntStateOf(0) }
    var memoryDone by remember(runKey) { mutableStateOf(false) }
    var memoryBonus by remember(runKey) { mutableIntStateOf(0) }

    // The verse text, fetched off the main thread. Shown on the result card and used by the memory step.
    val passage by produceState(initialValue = "", ref, bibleId) {
        value = try {
            services.bibles.verse(bibleId, ref)
        } catch (e: Exception) {
            ""
        }
    }

    fun commit() {
        mission.record(RoundResult(ref, outcome, resultMs, stars, hintUsed, wrongGuesses, roundType))
        val record = progress.recordRound(difficulty, mode, roundType, ref, stars, resultMs)
        newRecord = record
        if (daily && stars > 0 && !progress.dailyDone(today)) dailyStreak = progress.completeDaily(today)
        committed = true
        when {
            record -> {
                sfx.play(Sound.RECORD)
                haptics.record()
            }
            stars > 0 -> sfx.play(Sound.WIN)
            else -> {
                sfx.play(Sound.CLUNK)
                haptics.bad()
            }
        }
    }

    fun finish(result: Outcome, timeMs: Long) {
        outcome = result
        resultMs = timeMs
        if (result == Outcome.FOUND) {
            sfx.play(Sound.SLASH)
            haptics.good()
            if (mode == PlayMode.IN_APP) {
                stars = Scoring.starsFor(difficulty, timeMs, hintUsed, hintMode, roundType)
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
            stars = Scoring.starsFor(difficulty, resultMs, hintUsed, hintMode, roundType)
        } else {
            outcome = Outcome.WRONG_PLACE
            stars = 0
        }
        commit()
    }

    fun finishMemory(bonus: Int) {
        memoryBonus = bonus
        memoryDone = true
        progress.recordMemory(bonus)
        if (bonus > 0) sfx.play(Sound.STAR)
        stage = Stage.RESULT
    }

    fun nextRound() {
        if (daily && stars == 0) { // missed today's verse: try the same one again
            runKey += 1
            stage = Stage.LOGO
        } else if (mission.isComplete) {
            progress.recordMission()
            stage = Stage.SUMMARY
        } else {
            roundNo += 1
            roundType = drill.concrete(Random.Default)
            ref = picker.next(difficulty, roundType)
            runKey += 1
            stage = Stage.LOGO
        }
    }

    fun onChosen(choice: VerseRef) {
        if (roundType.matches(choice, ref)) {
            finish(Outcome.FOUND, clock.stop(SystemClock.elapsedRealtime()))
        } else {
            wrongGuesses += 1
            clock.addPenalty(GameRules.WRONG_GUESS_PENALTY_MS)
            sfx.play(Sound.CLUNK)
            haptics.bad()
            flash = "Not that one! +${GameRules.WRONG_GUESS_PENALTY_MS / 1000} seconds"
        }
    }

    // The clock. Runs only while a round is being played.
    LaunchedEffect(stage, runKey) {
        if (stage != Stage.PLAY) return@LaunchedEffect
        clock.start(SystemClock.elapsedRealtime())
        clockState = clock.state
        if (settings.speakNames) speaker.speak("name", index.spoken(ref, roundType))
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
                    Stage.LOGO -> LogoStage(
                        caption = if (daily) "Daily Challenge" else "Verse $roundNo of ${mission.rounds}",
                        runKey = runKey,
                        sfx = sfx,
                    ) { stage = Stage.PLAY }

                    Stage.PLAY -> PlayStage(
                        label = index.label(ref, roundType),
                        testament = index.book(ref.book).testament,
                        header = (if (daily) "Daily Challenge" else "Round $roundNo of ${mission.rounds}") +
                            "  -  ${difficulty.label}  -  ${mode.label}" +
                            (if (drill == DrillType.MIXED || (drill != DrillType.VERSE && !daily)) "  -  ${roundType.label}" else ""),
                        type = roundType,
                        difficulty = difficulty,
                        hintMode = hintMode,
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
                        type = roundType,
                        index = index,
                        passage = passage,
                        outcome = outcome,
                        mode = mode,
                        timeMs = resultMs,
                        stars = stars,
                        wrongGuesses = wrongGuesses,
                        committed = committed,
                        verdict = verdict,
                        nextLabel = when {
                            daily && stars == 0 && committed -> "Try again"
                            mission.results.size + (if (committed) 0 else 1) >= mission.rounds -> "See results"
                            else -> "Next verse"
                        },
                        newRecord = newRecord,
                        daily = daily,
                        dailyStreak = dailyStreak,
                        memoryDone = memoryDone,
                        memoryBonus = memoryBonus,
                        canMemory = stars > 0 && Memory.words(passage).size >= 4,
                        speaker = speaker,
                        onConfirm = { confirm(it) },
                        onRead = { readerRef = ref },
                        onMemory = { stage = Stage.MEMORY },
                        onNext = { nextRound() },
                    )

                    Stage.MEMORY -> MemoryStage(
                        text = passage,
                        difficulty = difficulty,
                        onFinish = { finishMemory(it) },
                    )

                    Stage.SUMMARY -> SummaryStage(mission, index, daily, onExit, onHome)
                }
                if (stage == Stage.RESULT && newRecord) Confetti()
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
                speaker.stop()
                onExit()
            },
            onDismiss = { showLeave = false },
        )
    }
}

@Composable
private fun LogoStage(caption: String, runKey: Int, sfx: Sfx, onDone: () -> Unit) {
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
            GameText(caption, size = 24.sp, color = Cream)
        }
    }
}

@Composable
private fun PlayStage(
    label: String,
    testament: Testament,
    header: String,
    type: DrillType,
    difficulty: Difficulty,
    hintMode: HintMode,
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
        GameText(header, size = 15.sp, color = Cream)
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
        } else if (hintMode == HintMode.TAP_COSTS_STAR && clockState == RoundClock.State.HUNTING) {
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
                    TimerText(clock, difficulty, type, getNow, 84.sp)
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
                        Box(Modifier.weight(1f)) { TimerText(clock, difficulty, type, getNow, 40.sp) }
                        if (flash != null) {
                            GameText(flash, size = 18.sp, color = Cherry, modifier = Modifier.weight(2f))
                        }
                    }
                    BibleNavigator(index, difficulty, type, Modifier.weight(1f), onVerseChosen = onChosen)
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
private fun TimerText(clock: RoundClock, difficulty: Difficulty, type: DrillType, getNow: () -> Long, size: TextUnit) {
    val ms = clock.elapsedMs(getNow())
    val seconds = ms / 1000.0
    val colour = when {
        seconds <= difficulty.threeStarSec * type.timeFactor -> Mint
        seconds <= difficulty.twoStarSec * type.timeFactor -> Gold
        else -> Cherry
    }
    GameText("${formatSeconds(ms)}s", size = size, color = colour)
}

@Composable
private fun ResultStage(
    ref: VerseRef,
    type: DrillType,
    index: BibleIndex,
    passage: String,
    outcome: Outcome,
    mode: PlayMode,
    timeMs: Long,
    stars: Int,
    wrongGuesses: Int,
    committed: Boolean,
    verdict: Verdict,
    nextLabel: String,
    newRecord: Boolean,
    daily: Boolean,
    dailyStreak: Int,
    memoryDone: Boolean,
    memoryBonus: Int,
    canMemory: Boolean,
    speaker: Speaker,
    onConfirm: (Boolean) -> Unit,
    onRead: () -> Unit,
    onMemory: () -> Unit,
    onNext: () -> Unit,
) {
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
    val thing = when (type) {
        DrillType.BOOK -> "book"
        DrillType.CHAPTER -> "chapter"
        else -> "verse"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText(headline, size = 40.sp, color = Gold)
        if (newRecord) {
            Spacer(Modifier.height(6.dp))
            ChunkySurface(face = Pink, lip = PinkLip, corner = 18.dp) {
                GameText("New record!", size = 30.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!pendingCheck) {
            StarRow(lit = stars, starSize = 64.dp)
            if (outcome == Outcome.FOUND || outcome == Outcome.WRONG_PLACE) {
                GameText("Your time: ${formatSeconds(timeMs)}s", size = 24.sp, color = Cream)
            }
            if (wrongGuesses > 0) GameText("Wrong tries: $wrongGuesses", size = 18.sp, color = Cream)
            if (memoryDone && memoryBonus > 0) GameText("+$memoryBonus memory star!", size = 20.sp, color = Gold)
            if (dailyStreak > 0) {
                GameText("Daily streak: $dailyStreak  (+${Daily.BONUS_STARS} bonus stars)", size = 20.sp, color = Gold)
            }
        }
        Spacer(Modifier.height(12.dp))

        CandyCard(Modifier.fillMaxWidth()) {
            GameText(index.label(ref, type), size = 30.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            if (type != DrillType.VERSE) {
                Text(
                    text = "It begins:",
                    color = Ink.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            SpokenText(
                text = if (passage.isEmpty()) "..." else passage,
                key = "result",
                speaker = speaker,
                color = Ink,
                fontSize = 22.sp,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = index.name,
                color = Ink.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (passage.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    ListenButton(speaker, "result", passage)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (pendingCheck) {
            GameText("Check your Bible. Is it open at this $thing?", size = 22.sp, color = Cream)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("Yes!", Mint, MintLip, Modifier.weight(1f), size = 26.sp, onClick = { onConfirm(true) })
                ChunkyButton("Not quite", Cherry, CherryLip, Modifier.weight(1f), size = 22.sp, onClick = { onConfirm(false) })
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("Read chapter", Sky, SkyLip, Modifier.weight(1f), size = 20.sp, onClick = onRead)
                ChunkyButton(
                    text = nextLabel,
                    face = Pink,
                    lip = PinkLip,
                    modifier = Modifier.weight(1f),
                    size = 22.sp,
                    enabled = committed,
                    onClick = onNext,
                )
            }
            if (committed && canMemory && !memoryDone) {
                Spacer(Modifier.height(10.dp))
                ChunkyButton(
                    "Memory challenge  (+1 star)",
                    Gold, GoldLip, Modifier.fillMaxWidth(), textColor = Ink, size = 22.sp, onClick = onMemory,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryStage(mission: Mission, index: BibleIndex, daily: Boolean, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val progress = LocalServices.current.progress
    val rank = progress.rank
    val next = rank.next

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText(if (daily) "Daily done!" else "Mission complete!", size = 38.sp, color = Gold)
        Spacer(Modifier.height(6.dp))
        if (!daily) GameText("${mission.totalStars} of ${mission.maxStars} stars", size = 28.sp)
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
                    Text(index.label(result.ref, result.type), color = Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
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
