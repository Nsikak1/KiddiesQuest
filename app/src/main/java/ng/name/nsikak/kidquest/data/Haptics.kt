package ng.name.nsikak.kidquest.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short buzzes for right and wrong answers. Respects the Settings switch. */
class Haptics(context: Context, private val settings: SettingsStore) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** Two quick taps. */
    fun good() = pattern(longArrayOf(0, 40, 70, 40))

    /** One longer, duller buzz. */
    fun bad() = pattern(longArrayOf(0, 160))

    /** A little drum-roll for a new record. */
    fun record() = pattern(longArrayOf(0, 40, 50, 40, 50, 40, 50, 160))

    private fun pattern(timings: LongArray) {
        if (!settings.vibrationOn) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            // A phone that refuses to vibrate is not worth a crash.
        }
    }
}
