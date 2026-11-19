package ng.name.nsikak.kidquest.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Reads text aloud with the phone's own voice, so it works offline once a voice is installed.
 * For slow readers: [speakingKey] says what is being read, and (Android 8+) [rangeStart]..[rangeEnd]
 * is the word being spoken right now, so the text can be highlighted as it is read.
 */
class Speaker(context: Context, private val settings: SettingsStore) {
    private var tts: TextToSpeech? = null
    private var finalId: String? = null
    private var singlePart = true

    var ready by mutableStateOf(false)
        private set

    /** True when the phone has no usable English voice. */
    var unavailable by mutableStateOf(false)
        private set

    var speakingKey by mutableStateOf<String?>(null)
        private set

    var rangeStart by mutableIntStateOf(-1)
        private set

    var rangeEnd by mutableIntStateOf(-1)
        private set

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (utteranceId != null) speakingKey = utteranceId.substringBefore('#')
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId != null && utteranceId == finalId) clear()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            clear()
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            if (singlePart) {
                rangeStart = start
                rangeEnd = end
            }
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                unavailable = true
            } else {
                val result = engine.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    unavailable = true
                } else {
                    engine.setOnUtteranceProgressListener(listener)
                    ready = true
                }
            }
        }
    }

    private fun clear() {
        speakingKey = null
        rangeStart = -1
        rangeEnd = -1
    }

    fun speak(key: String, text: String) = speakParts(key, listOf(text))

    /** Reads several pieces one after another (used for whole chapters, which can be very long). */
    fun speakParts(key: String, parts: List<String>) {
        val engine = tts ?: return
        if (!ready || parts.isEmpty()) return
        engine.setSpeechRate(if (settings.voiceSlow) 0.75f else 1.0f)
        singlePart = parts.size == 1
        clear()
        speakingKey = key
        finalId = "$key#${parts.lastIndex}"
        parts.forEachIndexed { i, part ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(part, mode, null, "$key#$i")
        }
    }

    fun stop() {
        tts?.stop()
        clear()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
