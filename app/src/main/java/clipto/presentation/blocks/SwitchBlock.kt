package clipto.presentation.blocks

import android.view.View
import android.widget.CompoundButton
import clipto.common.extensions.setDebounceClickListener
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueStringWithHeader
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_switch.view.*

class SwitchBlock<C>(
    private val titleRes: Int = 0,
    private val title: String? = null,
    private val iconRes: Int = 0,
    private val descriptionRes: Int = 0,
    private val description: String? = null,
    private val checked: Boolean,
    private val enabled: Boolean = true,
    private val textSize: Int = 16,
    private val maxLines: Int = 10,
    private val clickListener: CompoundButton.OnCheckedChangeListener
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_switch

    override fun areItemsTheSame(item: BlockItem<C>): Boolean = item is SwitchBlock
            && titleRes == item.titleRes
            && title == item.title

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SwitchBlock
                && enabled == item.enabled
                && checked == item.checked
                && descriptionRes == item.descriptionRes
                && description == item.description
                && iconRes == item.iconRes
                && textSize == item.textSize
                && maxLines == item.maxLines

    override fun onBind(context: C, block: View) {
        var colorKey = block.context.getTextColorPrimary()
        val colorValue = block.context.getTextColorSecondary()
        val titleView = block.titleView
        val valueView = block.valueView
        if (!enabled) {
            colorKey = colorValue
        }

        val ctx = block.context
        titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
        titleView.textSize = textSize.toFloat()
        titleView.maxLines = maxLines
        titleView.isEnabled = enabled
        valueView.isEnabled = enabled

        when {
            description != null -> {
                KeyValueStringWithHeader(
                    titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes,
                    description = description,
                    title = title ?: ctx.getString(titleRes)
                )
            }
            descriptionRes != 0 -> {
                KeyValueStringWithHeader(
                    titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes,
                    title = title ?: ctx.getString(titleRes)
                )
            }
            else -> {
                titleView.setTextColor(colorKey)
                titleView.text = title ?: ctx.getString(titleRes)
            }
        }

        block.setOnClickListener(null)
        valueView.setOnCheckedChangeListener(null)
        valueView.isChecked = checked

        if (enabled) {
            var oneTimeListener: CompoundButton.OnCheckedChangeListener? = null
            oneTimeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                valueView.setOnCheckedChangeListener(null)
                clickListener.onCheckedChanged(buttonView, isChecked)
                valueView.setOnCheckedChangeListener(oneTimeListener)
                titleView.isEnabled = isChecked
            }
            valueView.setOnCheckedChangeListener(oneTimeListener)
            block.setDebounceClickListener {
                valueView.isChecked = !valueView.isChecked
            }
        }

        block.isClickable = enabled
    }

}