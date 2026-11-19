package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.BibleIndex
import ng.name.nsikak.kidquest.core.BookInfo
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.DrillType
import ng.name.nsikak.kidquest.core.Testament
import ng.name.nsikak.kidquest.core.VerseRef
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.OrangeLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

/**
 * Lets the child "open the Bible" inside the app: book, then chapter, then verse.
 * On Easy the books are split into Old and New Testament with different colours (a free hint).
 * On Medium and Hard all books look the same, so the colours give nothing away.
 */
@Composable
fun BibleNavigator(
    index: BibleIndex,
    difficulty: Difficulty,
    stopAt: DrillType,
    modifier: Modifier = Modifier,
    onVerseChosen: (VerseRef) -> Unit,
) {
    var book by remember { mutableStateOf<BookInfo?>(null) }
    var chapter by remember { mutableIntStateOf(0) } // 0 means "no chapter chosen yet"
    val chosenBook = book

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chosenBook != null) {
                ChunkyButton("Back", Sky, SkyLip, size = 18.sp, onClick = {
                    if (chapter != 0) chapter = 0 else book = null
                })
                Spacer(Modifier.width(10.dp))
            }
            val title = when {
                chosenBook == null -> "Choose a book"
                chapter == 0 -> "${chosenBook.name}: choose a chapter"
                else -> "${chosenBook.name} $chapter: choose a verse"
            }
            GameText(title, size = 22.sp, align = TextAlign.Start, modifier = Modifier.weight(1f))
        }

        when {
            chosenBook == null -> BookGrid(index, difficulty, Modifier.weight(1f)) { picked ->
                if (stopAt == DrillType.BOOK) onVerseChosen(VerseRef(picked.number, 1, 1)) else book = picked
            }
            chapter == 0 -> NumberGrid(chosenBook.chapterCount, Mint, MintLip, Color.White, Modifier.weight(1f)) { picked ->
                if (stopAt == DrillType.CHAPTER) onVerseChosen(VerseRef(chosenBook.number, picked, 1)) else chapter = picked
            }
            else -> NumberGrid(chosenBook.versesIn(chapter), Gold, GoldLip, Ink, Modifier.weight(1f)) { verse ->
                onVerseChosen(VerseRef(chosenBook.number, chapter, verse))
            }
        }
    }
}

@Composable
private fun BookGrid(
    index: BibleIndex,
    difficulty: Difficulty,
    modifier: Modifier,
    onPick: (BookInfo) -> Unit,
) {
    val split = difficulty == Difficulty.EASY
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (split) {
            for (testament in Testament.entries) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GameText(testament.label, size = 20.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                items(index.books.filter { it.testament == testament }, key = { it.number }) { b ->
                    BookTile(b, split, onPick)
                }
            }
        } else {
            items(index.books, key = { it.number }) { b -> BookTile(b, split, onPick) }
        }
    }
}

@Composable
private fun BookTile(book: BookInfo, colourByTestament: Boolean, onPick: (BookInfo) -> Unit) {
    val face: Color
    val lip: Color
    if (colourByTestament && book.testament == Testament.OLD) {
        face = Sky; lip = SkyLip
    } else if (colourByTestament) {
        face = Orange; lip = OrangeLip
    } else {
        face = Grape; lip = GrapeLip
    }
    ChunkySurface(face, lip, Modifier.fillMaxWidth().heightIn(min = 62.dp), corner = 16.dp, onClick = { onPick(book) }) {
        GameText(book.name, size = 15.sp, maxLines = 2, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp))
    }
}

@Composable
private fun NumberGrid(
    count: Int,
    face: Color,
    lip: Color,
    textColor: Color,
    modifier: Modifier,
    onPick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(64.dp),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(List(count) { it + 1 }) { _, number ->
            ChunkySurface(face, lip, Modifier.fillMaxWidth().heightIn(min = 58.dp), corner = 16.dp, onClick = { onPick(number) }) {
                GameText(number.toString(), size = 22.sp, color = textColor, shadow = textColor == Color.White)
            }
        }
    }
}
