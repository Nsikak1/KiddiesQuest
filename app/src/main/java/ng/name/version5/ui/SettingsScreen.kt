package ng.name.version5.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.version5.data.BUNDLED_ID
import ng.name.version5.data.CatalogEntry
import ng.name.version5.data.DownloadStatus
import ng.name.version5.data.InstalledBible
import ng.name.version5.data.LocalServices
import ng.name.version5.data.TranslationCatalog
import ng.name.version5.ui.theme.Cherry
import ng.name.version5.ui.theme.CherryLip
import ng.name.version5.ui.theme.Ink
import ng.name.version5.ui.theme.Mint
import ng.name.version5.ui.theme.MintLip
import ng.name.version5.ui.theme.Sky
import ng.name.version5.ui.theme.SkyLip

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val services = LocalServices.current
    val settings = services.settings
    val downloads = services.downloads
    val context = LocalContext.current

    val catalog = remember { TranslationCatalog.load(context) }
    val installedVersion = downloads.installedVersion
    val installed = remember(installedVersion) { services.bibles.installed() }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<InstalledBible?>(null) }

    BackHandler { onBack() }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Settings", onBack)
            Spacer(Modifier.height(12.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CandyCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GameText("Sound", size = 24.sp, color = Ink, shadow = false, modifier = Modifier.weight(1f), align = TextAlign.Start)
                        Switch(checked = settings.soundOn, onCheckedChange = { settings.setSound(it) })
                    }
                    Spacer(Modifier.height(10.dp))
                    GameText("Easy get-ready time", size = 22.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth(), align = TextAlign.Start)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (seconds in listOf(5, 10, 15)) {
                            OptionChip("$seconds s", settings.countdownSec == seconds, { settings.setCountdown(seconds) })
                        }
                    }
                }

                GameText("Bibles", size = 28.sp, modifier = Modifier.fillMaxWidth())

                for (bible in installed) {
                    val extra = catalog.firstOrNull { it.id == bible.id }
                    BibleRow(
                        title = bible.name,
                        detail = if (bible.bundled) "Built in. Works offline." else (extra?.note ?: "Downloaded. Works offline."),
                        installed = true,
                        selected = settings.bibleId == bible.id,
                        status = null,
                        onUse = { settings.setBible(bible.id) },
                        onDownload = {},
                        onDelete = if (bible.bundled) null else ({ confirmDelete = bible }),
                    )
                }
                for (entry in catalog) {
                    if (installed.any { it.id == entry.id }) continue
                    BibleRow(
                        title = entry.name,
                        detail = "${entry.language}  -  about ${entry.approxMb.toInt().coerceAtLeast(1)} MB. ${entry.note}",
                        installed = false,
                        selected = false,
                        status = downloads.status[entry.id],
                        onUse = {},
                        onDownload = { downloads.start(entry) },
                        onDelete = null,
                    )
                }
                Text(
                    text = "Downloading needs the internet. Everything else in the app works without it.",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 15.sp,
                )

                ChunkyButton("Reset my stars", Cherry, CherryLip, Modifier.fillMaxWidth(), size = 20.sp) { confirmReset = true }

                Text(
                    text = "Bible text: King James Version (public domain). Lettering: Lilita One (SIL Open Font License). " +
                        "Sounds are made by the app itself.",
                    color = androidx.compose.ui.graphics.Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset stars?",
            message = "This clears all stars, ranks and best times on this device.",
            confirmText = "Reset",
            cancelText = "Keep",
            onConfirm = {
                services.progress.reset()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
    val deleting = confirmDelete
    if (deleting != null) {
        ConfirmDialog(
            title = "Delete this Bible?",
            message = "${deleting.name} will be removed from this device. You can download it again later.",
            confirmText = "Delete",
            cancelText = "Keep",
            onConfirm = {
                if (settings.bibleId == deleting.id) settings.setBible(BUNDLED_ID)
                downloads.delete(deleting.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun BibleRow(
    title: String,
    detail: String,
    installed: Boolean,
    selected: Boolean,
    status: DownloadStatus?,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    CandyCard(Modifier.fillMaxWidth()) {
        GameText(title, size = 24.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth(), align = TextAlign.Start)
        Text(detail, color = Ink, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        when {
            status is DownloadStatus.Running -> {
                val percent = (status.progress * 100).toInt()
                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    color = Mint,
                )
                Text("Downloading... $percent%", color = Ink, fontSize = 15.sp)
            }

            installed -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected) {
                    ChunkyButton("In use", Mint, MintLip, size = 18.sp) {}
                } else {
                    ChunkyButton("Use this one", Sky, SkyLip, size = 18.sp, onClick = onUse)
                }
                if (onDelete != null) ChunkyButton("Delete", Cherry, CherryLip, size = 18.sp, onClick = onDelete)
            }

            else -> Column {
                if (status is DownloadStatus.Failed) {
                    Text(status.message, color = Cherry, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                ChunkyButton(
                    text = if (status is DownloadStatus.Failed) "Try again" else "Download",
                    face = Sky,
                    lip = SkyLip,
                    size = 18.sp,
                    onClick = onDownload,
                )
            }
        }
    }
}
