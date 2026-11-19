package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ng.name.nsikak.kidquest.core.BookQuestion
import ng.name.nsikak.kidquest.core.ChoiceQuestion
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.OrderQuestion
import ng.name.nsikak.kidquest.core.Trainer
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Sound
import ng.name.nsikak.kidquest.ui.theme.Cherry
import ng.name.nsikak.kidquest.ui.theme.CherryLip
import ng.name.nsikak.kidquest.ui.theme.Cream
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.LockedFace
import ng.name.nsikak.kidquest.ui.theme.LockedLip
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

/** Learn the order of the 66 books: what comes next, what comes before, which testament, and put them in order. */
@Composable
fun BookTrainerScreen(onBack: () -> Unit) {
    val services = LocalServices.current
    val progress = services.progress
    val names = remember { services.bibles.index(services.settings.bibleId).books.map { it.name } }

    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var questions by remember { mutableStateOf<List<BookQuestion>?>(null) }
    var qIndex by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var earned by remember { mutableIntStateOf(-1) }

    fun start() {
        questions = Trainer.session(names, difficulty)
        qIndex = 0
        correct = 0
        earned = -1
    }

    val qs = questions
    val finished = qs != null && qIndex >= qs.size
    LaunchedEffect(finished) {
        if (finished && qs != null && earned < 0) earned = progress.recordTrainer(correct, qs.size)
    }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Book Trainer", onBack)
            Spacer(Modifier.height(10.dp))

            when {
                qs == null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CandyCard(Modifier.fillMaxWidth()) {
                        GameText("Know your books!", size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
                        Text(
                            "10 quick questions: which book comes next, which comes before, Old or New Testament, and putting books in order. Knowing the order makes you much faster at finding verses.",
                            color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                        Text(
                            "Best score: ${progress.trainerBest} of ${Trainer.QUESTIONS}",
                            color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    GameText("Choose your level", size = 22.sp, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (d in Difficulty.entries) {
                            OptionChip(d.label, difficulty == d, { difficulty = d }, Modifier.weight(1f))
                        }
                    }
                    ChunkyButton("Start!", Pink, PinkLip, Modifier.fillMaxWidth(), size = 32.sp) { start() }
                }

                qIndex >= qs.size -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameText("All done!", size = 40.sp, color = Gold)
                    Spacer(Modifier.height(8.dp))
                    StarRow(lit = earned.coerceAtLeast(0), starSize = 64.dp)
                    GameText("You got $correct of ${qs.size}", size = 28.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChunkyButton("Change level", Sky, SkyLip, Modifier.weight(1f), size = 20.sp) { questions = null }
                        ChunkyButton("Play again", Pink, PinkLip, Modifier.weight(1f), size = 22.sp) { start() }
                    }
                }

                else -> key(qIndex) {
                    QuestionView(qs[qIndex], qIndex + 1, qs.size, correct, names) { wasRight ->
                        if (wasRight) correct += 1
                        qIndex += 1
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(
    question: BookQuestion,
    number: Int,
    total: Int,
    score: Int,
    names: List<String>,
    onDone: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText("Question $number of $total   -   Score: $score", size = 17.sp, color = Cream)
        Spacer(Modifier.height(8.dp))
        CandyCard(Modifier.fillMaxWidth()) {
            GameText(question.prompt, size = 28.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(14.dp))
        when (question) {
            is ChoiceQuestion -> ChoiceView(question, onDone)
            is OrderQuestion -> OrderView(question, names, onDone)
        }
    }
}

@Composable
private fun ChoiceView(q: ChoiceQuestion, onDone: (Boolean) -> Unit) {
    val services = LocalServices.current
    var chosen by remember { mutableIntStateOf(-1) }
    LaunchedEffect(chosen) {
        if (chosen >= 0) {
            delay(900)
            onDone(chosen == q.correct)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        q.options.forEachIndexed { i, option ->
            val face = when {
                chosen < 0 -> Grape
                i == q.correct -> Mint
                i == chosen -> Cherry
                else -> LockedFace
            }
            val lip = when {
                chosen < 0 -> GrapeLip
                i == q.correct -> MintLip
                i == chosen -> CherryLip
                else -> LockedLip
            }
            ChunkySurface(
                face = face,
                lip = lip,
                modifier = Modifier.fillMaxWidth(),
                onClick = if (chosen < 0) ({
                    chosen = i
                    if (i == q.correct) {
                        services.sfx.play(Sound.SLASH)
                        services.haptics.good()
                    } else {
                        services.sfx.play(Sound.CLUNK)
                        services.haptics.bad()
                    }
                }) else null,
            ) {
                GameText(option, size = 26.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderView(q: OrderQuestion, names: List<String>, onDone: (Boolean) -> Unit) {
    val services = LocalServices.current
    var placed by remember { mutableStateOf(listOf<Int>()) }
    var mistakes by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf(false) }
    val done = placed.size == q.answer.size

    LaunchedEffect(done) {
        if (done) {
            delay(900)
            onDone(mistakes == 0)
        }
    }
    LaunchedEffect(flash) {
        if (flash) {
            delay(700)
            flash = false
        }
    }

    CandyCard(Modifier.fillMaxWidth()) {
        for (i in q.answer.indices) {
            val text = if (i < placed.size) "${i + 1}.  ${names[placed[i] - 1]}" else "${i + 1}.  ?"
            Text(
                text = text,
                color = if (i < placed.size) Ink else Ink.copy(alpha = 0.35f),
                fontSize = 22.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    if (flash) GameText("Not yet! Try another book.", size = 20.sp, color = Cherry) else GameText("Tap the next book", size = 20.sp, color = Cream)
    Spacer(Modifier.height(8.dp))
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (book in q.shuffled.filter { it !in placed }) {
            ChunkySurface(face = Grape, lip = GrapeLip, corner = 16.dp, onClick = {
                if (!done) {
                    if (book == q.answer[placed.size]) {
                        placed = placed + book
                        services.sfx.play(Sound.SLASH)
                        services.haptics.good()
                    } else {
                        mistakes += 1
                        flash = true
                        services.sfx.play(Sound.CLUNK)
                        services.haptics.bad()
                    }
                }
            }) {
                GameText(names[book - 1], size = 20.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
            }
        }
    }
}
