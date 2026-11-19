package ng.name.version5.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import ng.name.nsikak.kidquest.R
import ng.name.version5.core.Difficulty
import ng.name.version5.core.PlayMode
import ng.name.version5.core.Rank

/** Small preferences, kept as Compose state so screens update by themselves. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
        private set

    var bibleId by mutableStateOf(prefs.getString("bible", BUNDLED_ID) ?: BUNDLED_ID)
        private set

    /** Length of the Easy "get ready" countdown. */
    var countdownSec by mutableIntStateOf(prefs.getInt("countdown", 10))
        private set

    var lastMode by mutableStateOf(readEnum("mode", PlayMode.PHYSICAL))
        private set

    var lastDifficulty by mutableStateOf(readEnum("difficulty", Difficulty.EASY))
        private set

    fun setSound(on: Boolean) {
        soundOn = on
        prefs.edit().putBoolean("sound", on).apply()
    }

    fun setBible(id: String) {
        bibleId = id
        prefs.edit().putString("bible", id).apply()
    }

    fun setCountdown(seconds: Int) {
        countdownSec = seconds
        prefs.edit().putInt("countdown", seconds).apply()
    }

    fun setLast(mode: PlayMode, difficulty: Difficulty) {
        lastMode = mode
        lastDifficulty = difficulty
        prefs.edit().putString("mode", mode.name).putString("difficulty", difficulty.name).apply()
    }

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E {
        val name = prefs.getString(key, null) ?: return default
        return enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}

/** Stars, ranks and personal bests, saved on the phone. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    // Bumped on every change so anything that reads a best time re-draws.
    private var revision by mutableIntStateOf(0)

    var totalStars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set

    var missionsDone by mutableIntStateOf(prefs.getInt("missions", 0))
        private set

    val rank: Rank get() = Rank.forStars(totalStars)

    /** Best time in ms for a difficulty and mode, or null if there is none yet. */
    fun bestMs(difficulty: Difficulty, mode: PlayMode): Long? {
        @Suppress("UNUSED_VARIABLE")
        val subscribe = revision
        val value = prefs.getLong(bestKey(difficulty, mode), 0L)
        return if (value > 0) value else null
    }

    fun recordRound(difficulty: Difficulty, mode: PlayMode, stars: Int, timeMs: Long) {
        val editor = prefs.edit()
        totalStars += stars
        editor.putInt("stars", totalStars)
        if (stars > 0 && timeMs > 0) {
            val key = bestKey(difficulty, mode)
            val old = prefs.getLong(key, 0L)
            if (old == 0L || timeMs < old) editor.putLong(key, timeMs)
        }
        editor.apply()
        revision++
    }

    fun recordMission() {
        missionsDone++
        prefs.edit().putInt("missions", missionsDone).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
        totalStars = 0
        missionsDone = 0
        revision++
    }

    private fun bestKey(difficulty: Difficulty, mode: PlayMode) = "best_${difficulty.name}_${mode.name}"
}

enum class Sound { DRAW, TICK, GO, WIN, WRONG, STAR }

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
    val downloads = DownloadTracker(bibles)
}

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }
