package ng.name.nsikak.kidquest.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.core.GameRules
import ng.name.nsikak.kidquest.core.HintRule
import ng.name.nsikak.kidquest.core.ProgressData
import ng.name.nsikak.kidquest.data.BUNDLED_ID
import ng.name.nsikak.kidquest.data.DownloadStatus
import ng.name.nsikak.kidquest.data.InstalledBible
import ng.name.nsikak.kidquest.data.LocalServices
import ng.name.nsikak.kidquest.data.Today
import ng.name.nsikak.kidquest.data.TranslationCatalog
import ng.name.nsikak.kidquest.ui.theme.Cherry
import ng.name.nsikak.kidquest.ui.theme.CherryLip
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip

private fun limitLabel(seconds: Int): String = when (seconds) {
    0 -> "No limit"
    60 -> "1 min"
    120 -> "2 min"
    180 -> "3 min"
    else -> "$seconds s"
}

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
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    // Backup: the phone's own "save file" and "open file" pickers, so no storage permission is needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) {
            val saved = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(services.progress.exportText().toByteArray(Charsets.UTF_8))
                } != null
            } catch (e: Exception) {
                false
            }
            Toast.makeText(context, if (saved) "Backup saved." else "Could not save the backup.", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.size > 2_000_000) null else bytes.toString(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                null
            }
            if (text != null && ProgressData.fromText(text) != null) {
                pendingRestore = text
            } else {
                Toast.makeText(context, "That file is not a Kid Quest backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler { onBack() }

    GameBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            ScreenTitleBar("Settings", onBack)
            Spacer(Modifier.height(12.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CandyCard(Modifier.fillMaxWidth()) {
                    SwitchRow("Sound", null, settings.soundOn) { settings.setSound(it) }
                    SwitchRow("Vibration", "A short buzz for right and wrong answers", settings.vibrationOn) { settings.setVibration(it) }
                }

                CandyCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Game rules")
                    Label("Easy get-ready time")
                    ChipRow {
                        for (seconds in listOf(5, 10, 15)) {
                            OptionChip("$seconds s", settings.countdownSec == seconds, { settings.setCountdown(seconds) })
                        }
                    }
                    Label("Time for each verse")
                    ChipRow {
                        for (seconds in GameRules.TIME_LIMIT_CHOICES) {
                            OptionChip(limitLabel(seconds), settings.timeLimitSec == seconds, { settings.setTimeLimit(seconds) })
                        }
                    }
                    Label("Hints")
                    ChipRow {
                        for (rule in HintRule.entries) {
                            OptionChip(rule.label, settings.hintRule == rule, { settings.setHintRule(rule) })
                        }
                    }
                    Text(settings.hintRule.blurb, color = Ink, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                }

                CandyCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Reading with a voice")
                    SwitchRow("Say the verse name", "Reads the book, chapter and verse aloud at the start of each round", settings.speakNames) { settings.setSpeakNames(it) }
                    SwitchRow("Slow voice", "Easier to follow for young readers", settings.voiceSlow) { settings.setVoiceSlow(it) }
                    if (services.speaker.unavailable) {
                        Text(
                            "This phone has no English voice installed. Add one in the phone's Settings under Text-to-speech.",
                            color = Cherry, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp),
                        )
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
                    color = Color.White,
                    fontSize = 15.sp,
                )

                CandyCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Backup")
                    Text(
                        "Save your stars, records and streak to a file (for example on a memory stick or in Downloads), and restore them later or on another phone.",
                        color = Ink, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChunkyButton("Save backup", Mint, MintLip, Modifier.weight(1f), size = 18.sp) {
                            exportLauncher.launch("kidquest-backup-${Today.dateText()}.txt")
                        }
                        ChunkyButton("Restore backup", Sky, SkyLip, Modifier.weight(1f), size = 18.sp) {
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    }
                }

                ChunkyButton("Reset my stars", Cherry, CherryLip, Modifier.fillMaxWidth(), size = 20.sp) { confirmReset = true }

                Text(
                    text = "Bible text: King James Version (public domain). Lettering: Lilita One (SIL Open Font License). " +
                        "Sounds and pictures are made for the app.",
                    color = Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset stars?",
            message = "This clears all stars, ranks, records, streaks and the Bible map on this device.",
            confirmText = "Reset",
            cancelText = "Keep",
            onConfirm = {
                services.progress.reset()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
    val restoring = pendingRestore
    if (restoring != null) {
        ConfirmDialog(
            title = "Restore this backup?",
            message = "Your current stars and records will be replaced by the ones in the file.",
            confirmText = "Restore",
            cancelText = "Cancel",
            onConfirm = {
                val ok = services.progress.importText(restoring)
                pendingRestore = null
                Toast.makeText(context, if (ok) "Progress restored." else "Could not restore.", Toast.LENGTH_LONG).show()
            },
            onDismiss = { pendingRestore = null },
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
private fun SectionTitle(text: String) {
    GameText(text, size = 26.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth(), align = TextAlign.Start)
}

@Composable
private fun Label(text: String) {
    Text(text, color = Ink, fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun SwitchRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            GameText(title, size = 22.sp, color = Ink, shadow = false, align = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            if (detail != null) Text(detail, color = Ink.copy(alpha = 0.7f), fontSize = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
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
