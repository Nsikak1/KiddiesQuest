package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.BookInfo
import ng.name.nsikak.kidquest.core.Stories
import ng.name.nsikak.kidquest.core.Testament
import ng.name.nsikak.kidquest.core.Trainer
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Today
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.LockedFace
import ng.name.nsikak.kidquest.ui.theme.LockedLip

/** Two tabs: a map of all 66 books (lit once found) and a report of what is slowest. */
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val services = LocalServices.current
    val index = remember { services.bibles.index(services.settings.bibleId) }
    var tab by remember { mutableIntStateOf(0) }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("My Progress", onBack)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("Bible map", tab == 0, { tab = 0 }, Modifier.weight(1f))
                OptionChip("My report", tab == 1, { tab = 1 }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            if (tab == 0) MapTab(index.books, Modifier.weight(1f)) else ReportTab(index.books, { index.label(it) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MapTab(books: List<BookInfo>, modifier: Modifier) {
    val progress = LocalServices.current.progress
    val lit = progress.litBooks
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier) {
        GameText("${lit.size} of ${books.size} books found", size = 24.sp, color = Gold)
        val stat = if (selected in lit) progress.bookStat(selected) else null
        if (stat != null) {
            CandyCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    "${books[selected - 1].name}: found ${stat.found} time${if (stat.found == 1) "" else "s"}. Usual time ${formatSeconds(stat.avgMs)}s.",
                    color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Text(
                "Find a verse, chapter or book to light it up. Tap a lit book to see your times.",
                color = Color(0xCCFFFFFF), fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (testament in Testament.entries) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GameText(testament.label, size = 20.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                items(books.filter { it.testament == testament }, key = { it.number }) { book ->
                    val isLit = book.number in lit
                    ChunkySurface(
                        face = if (isLit) Gold else LockedFace,
                        lip = if (isLit) GoldLip else LockedLip,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        corner = 16.dp,
                        onClick = { selected = book.number },
                    ) {
                        GameText(
                            book.name, size = 15.sp, maxLines = 2,
                            color = if (isLit) Ink else Color(0xB3FFFFFF), shadow = !isLit,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTab(books: List<BookInfo>, label: (ng.name.nsikak.kidquest.core.VerseRef) -> String, modifier: Modifier) {
    val progress = LocalServices.current.progress
    val today = remember { Today.epochDay() }
    val slowBooks = progress.slowestBooks(5)
    val slowVerses = progress.slowestVerses(5)

    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CandyCard(Modifier.fillMaxWidth()) {
            GameText("Overview", size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            StatRow("Rank", progress.rank.title)
            StatRow("Stars", "${progress.totalStars}")
            StatRow("Missions finished", "${progress.missionsDone}")
            StatRow("Day streak", "${progress.currentStreak(today)} (best ${progress.bestStreak})")
            StatRow("Book trainer best", "${progress.trainerBest} of ${Trainer.QUESTIONS}")
            StatRow("Stories read", "${progress.storiesReadCount} of ${Stories.all.size}")
            StatRow("Memory stars won", "${progress.memoryWins}")
        }

        CandyCard(Modifier.fillMaxWidth()) {
            GameText("Slowest books", size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            if (slowBooks.isEmpty()) {
                EmptyNote()
            } else {
                for ((number, stat) in slowBooks) {
                    StatRow(books[number - 1].name, "${formatSeconds(stat.avgMs)}s  (${stat.found}x)")
                }
            }
        }

        CandyCard(Modifier.fillMaxWidth()) {
            GameText("Verses to practise", size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            if (slowVerses.isEmpty()) {
                EmptyNote()
            } else {
                for ((ref, stat) in slowVerses) {
                    StatRow(label(ref), "${formatSeconds(stat.avgMs)}s  (${stat.count}x)")
                }
            }
        }

        Text(
            "Tip: use the Book drill and the Book Trainer on the books at the top of the list. Times are averages of the rounds played.",
            color = Color(0xCCFFFFFF), fontSize = 15.sp, modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 17.sp)
    }
}

@Composable
private fun EmptyNote() {
    Text("Play a few rounds and this list will fill up.", color = Ink.copy(alpha = 0.6f), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
}
