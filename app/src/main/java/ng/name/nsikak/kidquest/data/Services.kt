package ng.name.nsikak.kidquest.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import ng.name.nsikak.kidquest.R
import ng.name.nsikak.kidquest.core.BookStat
import ng.name.nsikak.kidquest.core.Daily
import ng.name.nsikak.kidquest.core.Difficulty
import ng.name.nsikak.kidquest.core.DrillType
import ng.name.nsikak.kidquest.core.GameRules
import ng.name.nsikak.kidquest.core.HintRule
import ng.name.nsikak.kidquest.core.PlayMode
import ng.name.nsikak.kidquest.core.ProgressData
import ng.name.nsikak.kidquest.core.Rank
import ng.name.nsikak.kidquest.core.VerseRef
import ng.name.nsikak.kidquest.core.VerseStat
import java.util.Calendar
import java.util.TimeZone

/** "Today" as a whole number of days, in the phone's own time zone. The daily challenge and streak use it. */
object Today {
    fun epochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    /** 2026-11-18, built by hand so phone language settings can't change the digits. */
    fun dateText(): String {
        val c = Calendar.getInstance()
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return "${c.get(Calendar.YEAR)}-${if (m < 10) "0$m" else "$m"}-${if (d < 10) "0$d" else "$d"}"
    }
}

/** Small preferences, kept as Compose state so screens update by themselves. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
        private set

    var vibrationOn by mutableStateOf(prefs.getBoolean("vibration", true))
        private set

    var bibleId by mutableStateOf(prefs.getString("bible", BUNDLED_ID) ?: BUNDLED_ID)
        private set

    /** Length of the Easy "get ready" countdown. */
    var countdownSec by mutableIntStateOf(prefs.getInt("countdown", 10))
        private set

    /** Seconds allowed per verse. 0 means no limit. */
    var timeLimitSec by mutableIntStateOf(prefs.getInt("limit", GameRules.DEFAULT_TIME_LIMIT_SEC))
        private set

    @set:JvmName("setHintRuleInternal")
    var hintRule by mutableStateOf(readEnum("hintRule", HintRule.STANDARD))
        private set

    /** Say the verse name out loud at the start of each round (helps slow readers). */
    @set:JvmName("setSpeakNamesInternal")
    var speakNames by mutableStateOf(prefs.getBoolean("speakNames", false))
        private set

    @set:JvmName("setVoiceSlowInternal")
    var voiceSlow by mutableStateOf(prefs.getBoolean("voiceSlow", true))
        private set

    var lastMode by mutableStateOf(readEnum("mode", PlayMode.PHYSICAL))
        private set

    var lastDifficulty by mutableStateOf(readEnum("difficulty", Difficulty.EASY))
        private set

    var lastDrill by mutableStateOf(readEnum("drill", DrillType.VERSE))
        private set

    fun setSound(on: Boolean) { soundOn = on; prefs.edit().putBoolean("sound", on).apply() }
    fun setVibration(on: Boolean) { vibrationOn = on; prefs.edit().putBoolean("vibration", on).apply() }
    fun setBible(id: String) { bibleId = id; prefs.edit().putString("bible", id).apply() }
    fun setCountdown(seconds: Int) { countdownSec = seconds; prefs.edit().putInt("countdown", seconds).apply() }
    fun setTimeLimit(seconds: Int) { timeLimitSec = seconds; prefs.edit().putInt("limit", seconds).apply() }
    fun setHintRule(rule: HintRule) { hintRule = rule; prefs.edit().putString("hintRule", rule.name).apply() }
    fun setSpeakNames(on: Boolean) { speakNames = on; prefs.edit().putBoolean("speakNames", on).apply() }
    fun setVoiceSlow(on: Boolean) { voiceSlow = on; prefs.edit().putBoolean("voiceSlow", on).apply() }

    fun setLast(mode: PlayMode, difficulty: Difficulty, drill: DrillType) {
        lastMode = mode
        lastDifficulty = difficulty
        lastDrill = drill
        prefs.edit().putString("mode", mode.name).putString("difficulty", difficulty.name).putString("drill", drill.name).apply()
    }

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E {
        val name = prefs.getString(key, null) ?: return default
        return enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}

/**
 * Stars, ranks, personal bests, per-book times, streaks and so on. The rules live in the tested
 * core class [ProgressData]; this just keeps it on the phone and tells screens when it changed.
 */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private var data: ProgressData = prefs.getString("data", null)?.let { ProgressData.fromText(it) } ?: ProgressData()
    private var revision by mutableIntStateOf(0)

    private inline fun <T> read(block: ProgressData.() -> T): T {
        @Suppress("UNUSED_VARIABLE")
        val subscribe = revision // reading this makes the calling screen redraw when something changes
        return data.block()
    }

    private inline fun <T> write(block: ProgressData.() -> T): T {
        val result = data.block()
        prefs.edit().putString("data", data.toText()).apply()
        revision++
        return result
    }

    val totalStars: Int get() = read { this.totalStars }
    val missionsDone: Int get() = read { this.missions }
    val rank: Rank get() = Rank.forStars(totalStars)
    val bestStreak: Int get() = read { this.bestStreak }
    val trainerBest: Int get() = read { this.trainerBest }
    val memoryWins: Int get() = read { this.memoryWins }
    val storiesReadCount: Int get() = read { this.storiesRead.size }
    val litBooks: Set<Int> get() = read { this.litBooks.toSet() }

    fun bestMs(d: Difficulty, m: PlayMode, t: DrillType): Long? = read { this.bestMs(d, m, t) }
    fun currentStreak(today: Long): Int = read { this.currentStreak(today) }
    fun dailyDone(today: Long): Boolean = read { this.dailyDone(today) }
    fun isStoryRead(id: Int): Boolean = read { id in this.storiesRead }
    fun bookStat(book: Int): BookStat? = read { this.books[book] }
    fun slowestBooks(limit: Int = 5): List<Pair<Int, BookStat>> = read { this.slowestBooks(limit) }
    fun slowestVerses(limit: Int = 5): List<Pair<VerseRef, VerseStat>> = read { this.slowestVerses(limit) }

    /** Returns true if this round beat an earlier personal best. */
    fun recordRound(d: Difficulty, m: PlayMode, t: DrillType, ref: VerseRef, stars: Int, timeMs: Long): Boolean =
        write { this.recordRound(d, m, t, ref, stars, timeMs) }

    fun recordMission() = write { this.recordMission() }

    /** Completes today's daily challenge: updates the streak and gives the bonus stars. Returns the new streak. */
    fun completeDaily(today: Long): Int = write {
        val streak = this.recordDaily(today)
        this.addBonus(Daily.BONUS_STARS)
        streak
    }

    fun recordTrainer(correct: Int, total: Int): Int = write { this.recordTrainer(correct, total) }
    fun recordMemory(bonusStars: Int) = write { this.recordMemory(bonusStars) }
    fun markStoryRead(id: Int): Boolean = write { this.markStoryRead(id) }

    fun exportText(): String = data.toText()

    /** Replaces everything with a backup. Returns false (and changes nothing) if the text is not a backup. */
    fun importText(text: String): Boolean {
        val parsed = ProgressData.fromText(text) ?: return false
        data = parsed
        prefs.edit().putString("data", data.toText()).apply()
        revision++
        return true
    }

    fun reset() {
        data = ProgressData()
        prefs.edit().clear().apply()
        revision++
    }
}

enum class Sound { DRAW, TICK, GO, WIN, WRONG, STAR, SLASH, CLUNK, RECORD }

class Sfx(context: Context, private val settings: SettingsStore) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = HashMap<Sound, Int>()

    init {
        ids[Sound.DRAW] = pool.load(context, R.raw.sfx_draw, 1)
        ids[Sound.TICK] = pool.load(context, R.raw.sfx_tick, 1)
        ids[Sound.GO] = pool.load(context, R.raw.sfx_go, 1)
        ids[Sound.WIN] = pool.load(context, R.raw.sfx_win, 1)
        ids[Sound.WRONG] = pool.load(context, R.raw.sfx_wrong, 1)
        ids[Sound.STAR] = pool.load(context, R.raw.sfx_star, 1)
        ids[Sound.SLASH] = pool.load(context, R.raw.sfx_slash, 1)
        ids[Sound.CLUNK] = pool.load(context, R.raw.sfx_clunk, 1)
        ids[Sound.RECORD] = pool.load(context, R.raw.sfx_record, 1)
    }

    fun play(sound: Sound) {
        if (!settings.soundOn) return
        val id = ids[sound] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}

/** Everything the screens share. Created once in MainActivity. */
class AppServices(context: Context) {
    val settings = SettingsStore(context)
    val progress = ProgressStore(context)
    val bibles = BibleRepository(context)
    val sfx = Sfx(context, settings)
    val haptics = Haptics(context, settings)
    val speaker = Speaker(context, settings)
    val downloads = DownloadTracker(bibles)

    fun release() {
        sfx.release()
        speaker.shutdown()
    }
}

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }
