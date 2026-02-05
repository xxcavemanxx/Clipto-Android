package clipto.presentation.blocks.domain

import android.content.res.ColorStateList
import android.view.View
import androidx.core.graphics.ColorUtils
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.extensions.getBackgroundHighlightColor
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_main_action.view.*

class MainActionBlock<C>(
    private val iconRes: Int,
    private val titleRes: Int = 0,
    private val title: CharSequence? = null,
    private val iconColor: Int? = null,
    private val description: CharSequence? = null,
    private val onClick: (context: C) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_main_action

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is MainActionBlock &&
                iconRes == item.iconRes &&
                titleRes == item.titleRes &&
                iconColor == item.iconColor &&
                description == item.description &&
                title == item.title

    override fun onInit(context: C, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is MainActionBlock<*>) {
                (ref as MainActionBlock<C>).onClick.invoke(context)
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        val ctx = block.context
        block.tvTitle.text = title ?: ctx.getString(titleRes)
        block.ivIcon.setImageResource(iconRes)
        block.tvDescription.text = description
        block.tvDescription.setVisibleOrGone(description != null)
        val icColor: Int
        val bgColor: Int
        if (iconColor != null) {
            icColor = iconColor
            bgColor = ColorUtils.setAlphaComponent(iconColor, 20)
        } else {
            icColor = ctx.getTextColorSecondary()
            bgColor = ctx.getBackgroundHighlightColor()
        }
        block.ivIcon.imageTintList = ColorStateList.valueOf(icColor)
        block.bgView.imageTintList = ColorStateList.valueOf(bgColor)
    }

}