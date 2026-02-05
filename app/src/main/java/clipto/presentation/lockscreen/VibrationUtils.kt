package clipto.presentation.lockscreen

import android.content.Context
import android.os.Vibrator

object VibrationUtils {

    private val vibrateOkPass = longArrayOf(0, 20)
    private val vibrateWrongPass = longArrayOf(0, 50, 100, 50, 100, 40)

    fun vibrate(context: Context, pattern: VibratePattern, repeatMode: Int = -1): Boolean {
        return try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.run {
                when (pattern) {
                    VibratePattern.PIN_OK -> vibrate(vibrateOkPass, repeatMode)
                    VibratePattern.PIN_WRONG -> vibrate(vibrateWrongPass, repeatMode)
                }
                true
            } ?: run { false }
        } catch (th: Throwable) {
            false
        }
    }
}