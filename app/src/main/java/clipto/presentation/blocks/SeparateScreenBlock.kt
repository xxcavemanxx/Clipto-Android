package clipto.presentation.blocks

import android.text.style.TypefaceSpan
import android.view.View
import clipto.common.extensions.gone
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.visible
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.enableWithAlpha
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueStringWithHeader
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_separate_screen.view.*

class SeparateScreenBlock<C>(
    private val titleRes: Int = 0,
    private val iconRes: Int = 0,
    private val title: String? = null,
    private val descriptionRes: Int = 0,
    private val enabled: Boolean = true,
    private val description: String? = null,
    private val value: CharSequence? = null,
    private val withBoldHeader: Boolean = false,
    private val clickListener: View.OnClickListener,
    private val withActionIcon: Int = R.drawable.ic_arrow_right,
    private val withBadge: Boolean = false
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_separate_screen

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is SeparateScreenBlock && titleRes == item.titleRes && title == item.title

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SeparateScreenBlock
                && iconRes == item.iconRes
                && enabled == item.enabled
                && descriptionRes == item.descriptionRes
                && description == item.description
                && withBoldHeader == item.withBoldHeader
                && value == item.value
                && withActionIcon == item.withActionIcon
                && withBadge == item.withBadge

    override fun onBind(context: C, block: View) {
        val colorValue = block.context.getTextColorSecondary()
        val colorKey = if (enabled) block.context.getTextColorPrimary() else colorValue
        val titleRef = title ?: block.context.getString(titleRes)

        when {
            description != null -> {
                KeyValueStringWithHeader(
                    block.titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes,
                    description = description,
                    withBoldHeader = withBoldHeader,
                    title = titleRef
                )
            }
            descriptionRes != 0 -> {
                KeyValueStringWithHeader(
                    block.titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes,
                    withBoldHeader = withBoldHeader,
                    title = titleRef
                )
            }
            else -> {
                if (withBoldHeader) {
                    block.titleView.text = SimpleSpanBuilder()
                        .append(titleRef, TypefaceSpan("sans-serif-medium"))
                        .build()
                } else {
                    block.titleView.text = titleRef
                }
            }
        }

        block.titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
        block.setOnClickListener(clickListener)
        block.titleView.isEnabled = enabled
        block.isClickable = enabled
        block.isEnabled = enabled
        if (value != null) {
            block.valueView.isEnabled = enabled
            block.valueView.text = value
            block.valueView.visible()
        } else {
            block.valueView.gone()
        }

        block.iconView.setImageResource(withActionIcon)
        block.badgeView.setVisibleOrGone(withBadge)
        block.enableWithAlpha(enabled)
    }

}