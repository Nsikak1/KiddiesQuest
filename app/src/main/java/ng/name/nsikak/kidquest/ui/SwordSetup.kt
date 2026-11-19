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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.DrillType
import ng.name.nsikak.kidquest.core.PlayMode
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip

@Composable
fun SwordSetupScreen(onBack: () -> Unit, onStart: (PlayMode, Difficulty, DrillType) -> Unit) {
    val services = LocalServices.current
    val settings = services.settings
    val progress = services.progress
    var mode by remember { mutableStateOf(settings.lastMode) }
    var difficulty by remember { mutableStateOf(settings.lastDifficulty) }
    var drill by remember { mutableStateOf(settings.lastDrill) }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Draw Your Sword", onBack)
            Spacer(Modifier.height(8.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CandyCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SwordGraphic(Modifier.size(64.dp).rotate(35f))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            GameText("Rank: ${progress.rank.title}", size = 24.sp, color = Ink, shadow = false, align = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                            val next = progress.rank.next
                            val line = if (next != null) "${next.minStars - progress.totalStars} stars to ${next.title}" else "You are a Champion!"
                            Text(line, color = Ink, fontSize = 16.sp)
                        }
                    }
                }

                GameText("How will you open the Bible?", size = 24.sp, modifier = Modifier.fillMaxWidth())
                ChoiceCard(
                    title = PlayMode.PHYSICAL.label,
                    text = "Use a real Bible. Tap the button as soon as you find it.",
                    selected = mode == PlayMode.PHYSICAL,
                ) { mode = PlayMode.PHYSICAL }
                ChoiceCard(
                    title = PlayMode.IN_APP.label,
                    text = "Open the Bible inside the app: book, chapter, then verse.",
                    selected = mode == PlayMode.IN_APP,
                ) { mode = PlayMode.IN_APP }

                GameText("What will you find?", size = 24.sp, modifier = Modifier.fillMaxWidth())
                for (kind in DrillType.entries) {
                    ChoiceCard(kind.label, kind.blurb, drill == kind) { drill = kind }
                }

                GameText("Choose your level", size = 24.sp, modifier = Modifier.fillMaxWidth())
                for (level in Difficulty.entries) {
                    val best = if (drill == DrillType.MIXED) null else progress.bestMs(level, mode, drill)
                    val bestText = when {
                        drill == DrillType.MIXED -> "Records are kept for each kind of round"
                        best != null -> "Best: ${formatSeconds(best)}s"
                        else -> "No best time yet"
                    }
                    ChoiceCard(
                        title = level.label,
                        text = "${level.blurb}\n$bestText",
                        selected = difficulty == level,
                    ) { difficulty = level }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(8.dp))
            ChunkyButton(
                text = "Draw your sword!",
                face = Pink,
                lip = PinkLip,
                modifier = Modifier.fillMaxWidth(),
                size = 32.sp,
            ) {
                settings.setLast(mode, difficulty, drill)
                onStart(mode, difficulty, drill)
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, text: String, selected: Boolean, onClick: () -> Unit) {
    ChunkySurface(
        face = if (selected) Gold else Color(0xFF5B4FC4),
        lip = if (selected) GoldLip else Color(0xFF362C94),
        modifier = Modifier.fillMaxWidth(),
        corner = 20.dp,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            GameText(title, size = 24.sp, color = if (selected) Ink else Color.White, shadow = !selected, align = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            Text(text, color = if (selected) Ink else Color(0xE6FFFFFF), fontSize = 15.sp)
        }
    }
}
