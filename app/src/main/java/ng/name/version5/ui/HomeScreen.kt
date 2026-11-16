package ng.name.version5.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.version5.data.LocalServices
import ng.name.version5.ui.theme.Gold
import ng.name.version5.ui.theme.Grape
import ng.name.version5.ui.theme.GrapeLip
import ng.name.version5.ui.theme.LockedFace
import ng.name.version5.ui.theme.LockedLip
import ng.name.version5.ui.theme.Mint
import ng.name.version5.ui.theme.MintLip
import ng.name.version5.ui.theme.Orange
import ng.name.version5.ui.theme.OrangeLip
import ng.name.version5.ui.theme.Pink
import ng.name.version5.ui.theme.PinkLip
import ng.name.version5.ui.theme.Sky
import ng.name.version5.ui.theme.SkyLip

private class GameTile(
    val title: String,
    val subtitle: String,
    val emblem: String?, // null means "draw the sword"
    val face: Color,
    val lip: Color,
    val ready: Boolean,
)

private val tiles = listOf(
    GameTile("Draw Your Sword", "Find the verse. Beat the clock!", null, Pink, PinkLip, true),
    GameTile("Dictionary", "Look up words", "Aa", Sky, SkyLip, false),
    GameTile("New Words", "Learn a new word", "New", Orange, OrangeLip, false),
    GameTile("Spelling Bee", "Spell it right", "ABC", Grape, GrapeLip, false),
    GameTile("Maths", "Numbers and sums", "1+2", Mint, MintLip, false),
)

@Composable
fun HomeScreen(onSword: () -> Unit, onReader: () -> Unit, onSettings: () -> Unit) {
    val services = LocalServices.current
    val progress = services.progress
    val context = LocalContext.current

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GameText("Kids Quest", size = 40.sp, color = Gold, align = TextAlign.Start)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(24.dp)) { drawStar(Offset(size.width / 2, size.height / 2), size.minDimension / 2f, Gold) }
                        Spacer(Modifier.width(6.dp))
                        GameText("${progress.rank.title}  -  ${progress.totalStars} stars", size = 18.sp, align = TextAlign.Start)
                    }
                }
                ChunkyButton("Bible", Mint, MintLip, size = 18.sp, onClick = onReader)
                Spacer(Modifier.width(8.dp))
                ChunkyButton("Settings", Grape, GrapeLip, size = 18.sp, onClick = onSettings)
            }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiles.forEachIndexed { i, tile ->
                    val shift: Dp = if (i % 2 == 0) 0.dp else 28.dp
                    Box(Modifier.fillMaxWidth().padding(start = shift, end = 28.dp - shift)) {
                        ChunkySurface(
                            face = if (tile.ready) tile.face else LockedFace,
                            lip = if (tile.ready) tile.lip else LockedLip,
                            modifier = Modifier.fillMaxWidth(),
                            corner = 28.dp,
                            onClick = {
                                if (tile.ready) onSword()
                                else Toast.makeText(context, "${tile.title} is coming soon!", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(76.dp).background(Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (tile.emblem == null) {
                                        SwordGraphic(Modifier.size(58.dp).rotate(35f))
                                    } else {
                                        GameText(tile.emblem, size = 28.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    GameText(tile.title, size = 26.sp, align = TextAlign.Start)
                                    GameText(
                                        if (tile.ready) tile.subtitle else "Coming soon",
                                        size = 15.sp,
                                        color = Color(0xE6FFFFFF),
                                        align = TextAlign.Start,
                                        shadow = false,
                                    )
                                }
                                if (!tile.ready) PadlockIcon(Modifier.size(32.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
