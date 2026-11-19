package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.FillPuzzle
import ng.name.nsikak.kidquest.core.Memory
import ng.name.nsikak.kidquest.core.ScramblePuzzle
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Sound
import ng.name.nsikak.kidquest.ui.theme.Cherry
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip
import kotlin.random.Random

/** After finding a verse: fill in the missing word(s), or put the scrambled words back in order. Earns one bonus star. */
@Composable
fun MemoryStage(text: String, difficulty: Difficulty, onFinish: (bonusStars: Int) -> Unit) {
    val fill = remember(text) { Memory.fill(text, difficulty) }
    val scramble = remember(text) { Memory.scramble(text, difficulty) }
    val useFill = remember(text) { if (fill != null && scramble != null) Random.nextBoolean() else fill != null }

    if (fill == null && scramble == null) {
        LaunchedEffect(Unit) { onFinish(0) } // verse too short for a puzzle
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText("Memory challenge", size = 34.sp, color = Gold)
        Spacer(Modifier.height(10.dp))
        if (useFill && fill != null) FillView(fill, onFinish) else if (scramble != null) ScrambleView(scramble, onFinish)
        Spacer(Modifier.height(16.dp))
        ChunkyButton("Skip", Sky, SkyLip, size = 18.sp) { onFinish(0) }
    }
}

@Composable
private fun FillView(puzzle: FillPuzzle, onFinish: (Int) -> Unit) {
    val services = LocalServices.current
    var step by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    val done = step >= puzzle.blanks.size

    val sentence = buildAnnotatedString {
        puzzle.words.forEachIndexed { i, word ->
            val blank = puzzle.blanks.indexOfFirst { it.wordIndex == i }
            if (blank < 0) {
                append(word.text)
            } else {
                append(word.lead)
                when {
                    blank < step -> withStyle(SpanStyle(color = MintLip, fontWeight = FontWeight.Bold)) { append(word.core) }
                    blank == step -> withStyle(SpanStyle(background = Gold, color = Ink, fontWeight = FontWeight.Bold)) { append("  ______  ") }
                    else -> append("______")
                }
                append(word.trail)
            }
            append(" ")
        }
    }
    CandyCard(Modifier.fillMaxWidth()) {
        Text(sentence, color = Ink, fontSize = 24.sp, lineHeight = 36.sp, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(14.dp))

    if (!done) {
        GameText("Which word is missing?", size = 24.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (option in puzzle.blanks[step].options) {
                ChunkyButton(option, Grape, GrapeLip, Modifier.fillMaxWidth(), size = 26.sp) {
                    if (option == puzzle.blanks[step].answer) {
                        step += 1
                        services.sfx.play(Sound.SLASH)
                        services.haptics.good()
                    } else {
                        mistakes += 1
                        services.sfx.play(Sound.CLUNK)
                        services.haptics.bad()
                    }
                }
            }
        }
    } else {
        FinishButtons(mistakes, onFinish)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScrambleView(puzzle: ScramblePuzzle, onFinish: (Int) -> Unit) {
    val services = LocalServices.current
    var used by remember { mutableStateOf(setOf<Int>()) }
    var placed by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    val done = placed >= puzzle.words.size

    CandyCard(Modifier.fillMaxWidth()) {
        Text(
            text = if (placed == 0) "Tap the words in the right order..." else puzzle.words.take(placed).joinToString(" "),
            color = if (placed == 0) Ink.copy(alpha = 0.4f) else Ink,
            fontSize = 24.sp,
            lineHeight = 36.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(14.dp))

    if (!done) {
        GameText("Put the verse back in order", size = 22.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (wordIndex in puzzle.order.filter { it !in used }) {
                ChunkySurface(face = Grape, lip = GrapeLip, corner = 16.dp, onClick = {
                    if (Memory.isNextWord(puzzle, placed, wordIndex)) {
                        used = used + wordIndex
                        placed += 1
                        services.sfx.play(Sound.SLASH)
                        services.haptics.good()
                    } else {
                        mistakes += 1
                        services.sfx.play(Sound.CLUNK)
                        services.haptics.bad()
                    }
                }) {
                    GameText(puzzle.words[wordIndex], size = 22.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }
    } else {
        FinishButtons(mistakes, onFinish)
    }
}

@Composable
private fun FinishButtons(mistakes: Int, onFinish: (Int) -> Unit) {
    val bonus = Memory.bonusStars(mistakes)
    GameText("Well done!", size = 34.sp, color = Gold)
    GameText(
        if (bonus > 0) "You earned a bonus star!" else "Good try. Next time, fewer slips for a star!",
        size = 20.sp,
        color = if (bonus > 0) Gold else Cherry,
    )
    Spacer(Modifier.height(12.dp))
    ChunkyButton(
        text = if (bonus > 0) "Continue  (+1 star)" else "Continue",
        face = Mint,
        lip = MintLip,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        size = 28.sp,
    ) { onFinish(bonus) }
}
