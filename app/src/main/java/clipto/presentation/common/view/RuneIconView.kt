package clipto.presentation.common.view

import com.wb.clipboard.databinding.ViewRuneIconBinding
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

class RuneIconView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewRuneIconBinding

    init {
        inflate(context, R.layout.view_rune_icon, this)
        binding = ViewRuneIconBinding.bind(this)
    }

    fun withRune(rune: IRune, active: Boolean): RuneIconView {
        log("setRune :: {} -> {}", rune.getId(), active)
        binding.iconView.imageTintList = ColorStateList.valueOf(rune.getIconColor(context, active))
        binding.iconView.setImageResource(rune.getIcon())
        binding.iconView.refreshDrawableState()

        binding.bgView.imageTintList = ColorStateList.valueOf(rune.getBgColor(context, active))
        binding.bgView.refreshDrawableState()
        return this
    }

    fun withRoundedCorners(): RuneIconView {
        binding.bgView.setImageResource(R.drawable.bg_rune_item)
        val margin = Units.DP.toPx(10f).toInt()
        binding.iconView.updateMargin(margin, margin, margin, margin)
        return this
    }

    fun withHighlightIndicator(): RuneIconView {
        binding.highlightView.visible()
        return this
    }

    override fun hasOverlappingRendering(): Boolean = false

}
