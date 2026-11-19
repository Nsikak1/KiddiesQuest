package ng.name.nsikak.kidquest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ng.name.nsikak.kidquest.core.VerseRef
import ng.name.nsikak.kidquest.data.BUNDLED_ID
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.ui.theme.Cream
import ng.name.nsikak.kidquest.ui.theme.CreamLip
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

private enum class Pick { NONE, BOOK, CHAPTER, VERSION }

/**
 * Reads any installed translation (the bundled KJV or a downloaded one), one chapter at a time.
 * If [startRef] is given, that chapter opens and the verse is highlighted.
 */
@Composable
fun BibleReaderScreen(startRef: VerseRef?, onBack: () -> Unit) {
    val services = LocalServices.current
    val settings = services.settings
    val installedVersion = services.downloads.installedVersion
    val installed = remember(installedVersion) { services.bibles.installed() }

    var bibleId by remember {
        mutableStateOf(if (installed.any { it.id == settings.bibleId }) settings.bibleId else BUNDLED_ID)
    }
    val index = remember(bibleId, installedVersion) { services.bibles.index(bibleId) }
    var bookNo by remember { mutableIntStateOf(startRef?.book ?: 1) }
    var chapterNo by remember { mutableIntStateOf(startRef?.chapter ?: 1) }
    var highlight by remember { mutableStateOf(startRef?.verse) }
    var fontSp by remember { mutableIntStateOf(20) }
    var picker by remember { mutableStateOf(Pick.NONE) }

    val book = index.book(bookNo)
    val chapter = chapterNo.coerceIn(1, book.chapterCount)

    val verses by produceState(initialValue = emptyList<String>(), bibleId, bookNo, chapter) {
        value = try {
            services.bibles.chapter(bibleId, bookNo, chapter)
        } catch (e: Exception) {
            listOf("This chapter could not be loaded.")
        }
    }

    val speaker = services.speaker
    LaunchedEffect(bibleId, bookNo, chapter) { speaker.stop() }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }

    val listState = rememberLazyListState()
    // Item 0 is the chapter title, so verse n sits at list position n.
    LaunchedEffect(verses) {
        val target = highlight
        if (verses.isNotEmpty()) {
            listState.scrollToItem(if (target != null) target.coerceIn(1, verses.size) else 0)
        }
    }

    fun go(delta: Int) {
        var b = bookNo
        var c = chapter + delta
        if (c < 1) {
            if (b > 1) {
                b -= 1
                c = index.book(b).chapterCount
            } else {
                c = 1
            }
        } else if (c > book.chapterCount) {
            if (b < index.books.size) {
                b += 1
                c = 1
            } else {
                c = book.chapterCount
            }
        }
        bookNo = b
        chapterNo = c
        highlight = null
    }

    BackHandler { onBack() }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
            ScreenTitleBar("Bible", onBack)
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChunkyButton(bibleId.uppercase(), Gold, GoldLip, textColor = Ink, size = 18.sp) { picker = Pick.VERSION }
                ChunkyButton(book.displayName, Grape, GrapeLip, size = 18.sp) { picker = Pick.BOOK }
                ChunkyButton("Chapter $chapter", Mint, MintLip, size = 18.sp) { picker = Pick.CHAPTER }
                ChunkyButton("A-", Sky, SkyLip, size = 18.sp, enabled = fontSp > 16) { fontSp -= 2 }
                ChunkyButton("A+", Sky, SkyLip, size = 18.sp, enabled = fontSp < 32) { fontSp += 2 }
                if (verses.isNotEmpty()) {
                    ListenButton(speaker, "reader", "", parts = verses.mapIndexed { i, v -> "Verse ${i + 1}. $v" })
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.matchParentSize().padding(top = 6.dp).background(CreamLip, RoundedCornerShape(24.dp)))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(bottom = 6.dp).background(Cream, RoundedCornerShape(24.dp)),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            GameText("${book.displayName} $chapter", size = 32.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
                            Text(
                                text = index.name,
                                color = Ink.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                    itemsIndexed(verses) { i, text ->
                        val number = i + 1
                        val marked = highlight == number
                        val styled = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = PinkLip,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (fontSp * 0.7f).sp,
                                    baselineShift = BaselineShift.Superscript,
                                ),
                            ) { append("$number ") }
                            append(text)
                        }
                        Text(
                            text = styled,
                            color = Ink,
                            fontSize = fontSp.sp,
                            lineHeight = (fontSp * 1.5f).sp,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(if (marked) Color(0xFFFFE08A) else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton(
                    "Previous", Pink, PinkLip, Modifier.weight(1f), size = 20.sp,
                    enabled = !(bookNo == 1 && chapter == 1),
                ) { go(-1) }
                ChunkyButton(
                    "Next", Pink, PinkLip, Modifier.weight(1f), size = 20.sp,
                    enabled = !(bookNo == index.books.size && chapter == book.chapterCount),
                ) { go(1) }
            }
        }
    }

    when (picker) {
        Pick.BOOK -> PickerDialog("Choose a book", index.books.map { it.name }, 104.dp, { bookNo = it + 1; chapterNo = 1; highlight = null; picker = Pick.NONE }) {
            picker = Pick.NONE
        }
        Pick.CHAPTER -> PickerDialog("${book.displayName}: choose a chapter", (1..book.chapterCount).map { it.toString() }, 64.dp, { chapterNo = it + 1; highlight = null; picker = Pick.NONE }) {
            picker = Pick.NONE
        }
        Pick.VERSION -> PickerDialog("Choose a Bible", installed.map { it.name }, 140.dp, {
            val chosen = installed[it].id
            bibleId = chosen
            settings.setBible(chosen)
            picker = Pick.NONE
        }) { picker = Pick.NONE }
        Pick.NONE -> Unit
    }
}

@Composable
private fun PickerDialog(
    title: String,
    labels: List<String>,
    cell: Dp,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        CandyCard(Modifier.fillMaxWidth()) {
            GameText(title, size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(cell),
                modifier = Modifier.heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(labels) { i, label ->
                    ChunkySurface(
                        face = Grape,
                        lip = GrapeLip,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        corner = 14.dp,
                        onClick = { onPick(i) },
                    ) {
                        GameText(label, size = 15.sp, maxLines = 2, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
