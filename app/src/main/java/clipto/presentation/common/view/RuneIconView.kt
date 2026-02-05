package clipto.presentation.common.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.FrameLayout
import clipto.common.extensions.updateMargin
import clipto.common.extensions.visible
import clipto.common.misc.Units
import clipto.domain.IRune
import clipto.extensions.log
import clipto.presentation.runes.extensions.getBgColor
import clipto.presentation.runes.extensions.getIconColor
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.view_rune_icon.view.*

class RuneIconView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_rune_icon, this)
    }

    fun withRune(rune: IRune, active: Boolean): RuneIconView {
        log("setRune :: {} -> {}", rune.getId(), active)
        iconView.imageTintList = ColorStateList.valueOf(rune.getIconColor(context, active))
        iconView.setImageResource(rune.getIcon())
        iconView.refreshDrawableState()

        bgView.imageTintList = ColorStateList.valueOf(rune.getBgColor(context, active))
        bgView.refreshDrawableState()
        return this
    }

    fun withRoundedCorners(): RuneIconView {
        bgView.setImageResource(R.drawable.bg_rune_item)
        val margin = Units.DP.toPx(10f).toInt()
        iconView.updateMargin(margin, margin, margin, margin)
        return this
    }

    fun withHighlightIndicator(): RuneIconView {
        highlightView.visible()
        return this
    }

    override fun hasOverlappingRendering(): Boolean = false

}