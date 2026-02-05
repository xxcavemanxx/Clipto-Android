package clipto.common.extensions

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.TransitionManager
import com.transitionseverywhere.ChangeText

fun TextView?.animateText(charSequence: CharSequence?, duration: Long = 500L) {
    this?.post {
        (parent as? ViewGroup)?.let {
            TransitionManager.beginDelayedTransition(it, ChangeText().setChangeBehavior(
                    ChangeText.CHANGE_BEHAVIOR_OUT_IN).setDuration(duration).addTarget(this))
        }
        text = charSequence
    }
}

fun TextView.setBold(bold: Boolean) {
    if (bold) {
        if (typeface?.isBold != true) {
            setTypeface(null, Typeface.BOLD)
        }
    } else {
        if (typeface?.isBold == true) {
            setTypeface(null, Typeface.NORMAL)
        }
    }
}