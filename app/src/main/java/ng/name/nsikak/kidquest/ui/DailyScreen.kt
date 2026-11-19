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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.Daily
import ng.name.nsikak.kidquest.core.PlayMode
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Today
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.OrangeLip
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip

/** One verse a day, the same for every child. Finding it keeps the streak going. */
@Composable
fun DailyScreen(onBack: () -> Unit, onStart: (PlayMode) -> Unit) {
    val services = LocalServices.current
    val progress = services.progress
    val today = remember { Today.epochDay() }
    val streak = progress.currentStreak(today)
    val done = progress.dailyDone(today)
    var mode by remember { mutableStateOf(services.settings.lastMode) }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Daily Challenge", onBack)
            Spacer(Modifier.height(12.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CandyCard(Modifier.fillMaxWidth()) {
                    GameText("Day streak", size = 24.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
                    GameText("$streak", size = 80.sp, color = Orange, shadow = false, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Best streak: ${progress.bestStreak}",
                        color = Ink,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                CandyCard(Modifier.fillMaxWidth()) {
                    GameText(
                        if (done) "You did today's verse!" else "Today's verse is waiting",
                        size = 24.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = if (done) {
                            "Come back tomorrow to keep your streak. You can still practise it again, but there is no extra prize."
                        } else {
                            "Everyone gets the same verse today. Find it to keep your streak and win ${Daily.BONUS_STARS} bonus stars!"
                        },
                        color = Ink,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }

                GameText("How will you open the Bible?", size = 22.sp, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OptionChip("Physical Bible", mode == PlayMode.PHYSICAL, { mode = PlayMode.PHYSICAL }, Modifier.weight(1f))
                    OptionChip("In-app Bible", mode == PlayMode.IN_APP, { mode = PlayMode.IN_APP }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(8.dp))
            ChunkyButton(
                text = if (done) "Practise again" else "Today's verse!",
                face = if (done) Orange else Pink,
                lip = if (done) OrangeLip else PinkLip,
                modifier = Modifier.fillMaxWidth(),
                size = 30.sp,
            ) { onStart(mode) }
        }
    }
}
