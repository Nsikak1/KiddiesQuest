package ng.name.nsikak.kidquest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.R
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Today
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.GrapeLip
import ng.name.nsikak.kidquest.ui.theme.LockedFace
import ng.name.nsikak.kidquest.ui.theme.LockedLip
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.OrangeLip
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

private class GameTile(
    val title: String,
    val subtitle: String,
    val emblem: String?, // null means "draw the sword"
    val face: Color,
    val lip: Color,
    val onClick: () -> Unit,
)

private val comingSoon = listOf("Dictionary", "New Words", "Spelling Bee", "Maths")

@Composable
fun HomeScreen(
    onSword: () -> Unit,
    onDaily: () -> Unit,
    onTrainer: () -> Unit,
    onStories: () -> Unit,
    onProgress: () -> Unit,
    onReader: () -> Unit,
    onSettings: () -> Unit,
) {
    val progress = LocalServices.current.progress
    val today = remember { Today.epochDay() }
    val streak = progress.currentStreak(today)
    val dailyDone = progress.dailyDone(today)

    val tiles = listOf(
        GameTile("Draw Your Sword", "Find the verse. Beat the clock!", null, Pink, PinkLip, onSword),
        GameTile(
            "Daily Challenge",
            (if (dailyDone) "Done today!" else "A new verse is waiting") + "  -  Streak: $streak",
            "Day", Orange, OrangeLip, onDaily,
        ),
        GameTile("Book Trainer", "Learn the 66 books in order", "66", Sky, SkyLip, onTrainer),
        GameTile("Bible Stories", "Read famous stories", "Story", Grape, GrapeLip, onStories),
        GameTile("My Progress", "Your Bible map and report", "Map", Mint, MintLip, onProgress),
    )

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(R.drawable.logo_wordmark),
                        contentDescription = "Kid Quest",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(46.dp).fillMaxWidth(),
                        alignment = Alignment.CenterStart,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Canvas(Modifier.size(22.dp)) { drawStar(Offset(size.width / 2, size.height / 2), size.minDimension / 2f, Gold) }
                        Spacer(Modifier.width(6.dp))
                        GameText("${progress.rank.title}  -  ${progress.totalStars} stars", size = 17.sp, align = TextAlign.Start)
                    }
                }
                ChunkyButton("Bible", Mint, MintLip, size = 16.sp, onClick = onReader)
                Spacer(Modifier.width(6.dp))
                ChunkyButton("Settings", Grape, GrapeLip, size = 16.sp, onClick = onSettings)
            }
            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                tiles.forEachIndexed { i, tile ->
                    val shift: Dp = if (i % 2 == 0) 0.dp else 28.dp
                    Box(Modifier.fillMaxWidth().padding(start = shift, end = 28.dp - shift)) {
                        ChunkySurface(
                            face = tile.face,
                            lip = tile.lip,
                            modifier = Modifier.fillMaxWidth(),
                            corner = 28.dp,
                            onClick = tile.onClick,
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(76.dp).background(Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (tile.emblem == null) {
                                        SwordGraphic(Modifier.size(58.dp).rotate(35f))
                                    } else {
                                        GameText(tile.emblem, size = if (tile.emblem.length > 3) 21.sp else 30.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    GameText(tile.title, size = 25.sp, align = TextAlign.Start)
                                    GameText(
                                        tile.subtitle,
                                        size = 15.sp,
                                        color = Color(0xE6FFFFFF),
                                        align = TextAlign.Start,
                                        shadow = false,
                                    )
                                }
                            }
                        }
                    }
                }

                GameText("More games coming soon", size = 18.sp, color = Color(0xCCFFFFFF), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (name in comingSoon) {
                        ChunkySurface(face = LockedFace, lip = LockedLip, corner = 16.dp) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PadlockIcon(Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                GameText(name, size = 16.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
