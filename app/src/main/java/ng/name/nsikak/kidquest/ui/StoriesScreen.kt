package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.Stories
import ng.name.nsikak.kidquest.core.VerseRef
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Sound
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.OrangeLip
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

/** The list of famous stories. Tap one to read it (and hear it). */
@Composable
fun StoriesScreen(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val services = LocalServices.current
    val progress = services.progress
    val index = remember { services.bibles.index(services.settings.bibleId) }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Bible Stories", onBack)
            Spacer(Modifier.height(10.dp))
            GameText("Read a story to win a star! (${progress.storiesReadCount} of ${Stories.all.size} read)", size = 18.sp, color = androidx.compose.ui.graphics.Color(0xE6FFFFFF))
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Stories.all, key = { it.id }) { story ->
                    val read = progress.isStoryRead(story.id)
                    CandyCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GameText(story.title, size = 24.sp, color = Ink, shadow = false, align = TextAlign.Start, modifier = Modifier.weight(1f))
                            if (read) GameText("Read", size = 16.sp, color = MintLip, shadow = false)
                        }
                        Text(story.summary, color = Ink, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            index.rangeLabel(story.book, story.chapter, story.fromVerse, story.toVerse),
                            color = Ink.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        ChunkyButton("Read this story", Orange, OrangeLip, size = 18.sp) { onOpen(story.id) }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/** One story: the passage in big letters, a Listen button, and "I finished reading" for a star. */
@Composable
fun StoryScreen(id: Int, onBack: () -> Unit, onReadChapter: (VerseRef) -> Unit) {
    val story = Stories.byId(id)
    if (story == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val services = LocalServices.current
    val settings = services.settings
    val progress = services.progress
    val speaker = services.speaker
    val bibleId = remember { settings.bibleId }
    val index = remember { services.bibles.index(bibleId) }
    var message by remember { mutableStateOf<String?>(null) }

    val verses by produceState(initialValue = emptyList<String>(), id, bibleId) {
        value = try {
            val chapter = services.bibles.chapter(bibleId, story.book, story.chapter)
            val from = (story.fromVerse - 1).coerceIn(0, chapter.size)
            val to = story.toVerse.coerceIn(from, chapter.size)
            chapter.subList(from, to)
        } catch (e: Exception) {
            listOf("This story could not be loaded.")
        }
    }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Bible Story", onBack)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                GameText(story.title, size = 34.sp, color = Gold)
                GameText(
                    index.rangeLabel(story.book, story.chapter, story.fromVerse, story.toVerse) + "  -  " + index.name,
                    size = 16.sp, color = androidx.compose.ui.graphics.Color(0xE6FFFFFF),
                )
                Spacer(Modifier.height(10.dp))
                CandyCard(Modifier.fillMaxWidth()) {
                    Text(story.summary, color = Ink, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(10.dp))
                CandyCard(Modifier.fillMaxWidth()) {
                    val passage = buildAnnotatedString {
                        verses.forEachIndexed { i, text ->
                            withStyle(SpanStyle(color = PinkLip, fontWeight = FontWeight.Bold, fontSize = 15.sp, baselineShift = BaselineShift.Superscript)) {
                                append("${story.fromVerse + i} ")
                            }
                            append(text)
                            append("  ")
                        }
                    }
                    Text(passage, color = Ink, fontSize = 22.sp, lineHeight = 33.sp, fontFamily = FontFamily.Serif, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ListenButton(speaker, "story${story.id}", "", parts = verses.ifEmpty { null })
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (message != null) GameText(message ?: "", size = 22.sp, color = Gold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("Whole chapter", Sky, SkyLip, Modifier.weight(1f), size = 18.sp) {
                    speaker.stop()
                    onReadChapter(VerseRef(story.book, story.chapter, story.fromVerse))
                }
                ChunkyButton("I finished!", Mint, MintLip, Modifier.weight(1f), size = 22.sp) {
                    speaker.stop()
                    val first = progress.markStoryRead(story.id)
                    if (first) services.sfx.play(Sound.STAR)
                    message = if (first) "+1 star! Great reading!" else "Great reading again!"
                }
            }
        }
    }
}
