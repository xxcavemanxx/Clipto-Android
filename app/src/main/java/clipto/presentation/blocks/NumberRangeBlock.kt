package clipto.presentation.blocks

import android.view.View
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.setTextWithSelection
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.extensions.log
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_number_range.view.*

class NumberRangeBlock<C>(
    private val minValue: Int? = null,
    private val maxValue: Int? = null,
    private val enabled: Boolean = true,
    private val onRangeChanged: (min: Int?, max: Int?) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_number_range

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is NumberRangeBlock
                && item.enabled == enabled
                && item.minValue == minValue
                && item.maxValue == maxValue
    }

    override fun onInit(context: C, block: View) {
        block.etMin.doAfterTextChanged { doOnRangeChanged(block, block.etMin) }
        block.etMax.doAfterTextChanged { doOnRangeChanged(block, block.etMax) }
    }

    override fun onBind(context: C, block: View) {
        block.tag = null
        val ctx = block.context

        block.tilMin.isEnabled = enabled
        block.tilMax.isEnabled = enabled
        block.etMin.isEnabled = enabled
        block.etMax.isEnabled = enabled
        val textColor =
            if (enabled) {
                ctx.getTextColorPrimary()
            } else {
                ctx.getTextColorSecondary()
            }
        block.etMin.setTextColor(textColor)
        block.etMax.setTextColor(textColor)

        val minValueText = minValue?.toString()
        if (minValueText != block.etMin.text?.toString()) {
            block.etMin.setTextWithSelection(minValueText)
        }

        val maxValueText = maxValue?.toString()
        if (maxValueText != block.etMax.text?.toString()) {
            block.etMax.setTextWithSelection(maxValueText)
        }

        block.tag = this
    }

    private fun doOnRangeChanged(block: View, field: View) {
        val ref = block.tag
        if (ref is NumberRangeBlock<*>) {
            val min = block.etMin.text?.toString()?.toIntOrNull()
            val max = block.etMax.text?.toString()?.toIntOrNull()
            if (min != null && max != null && min >= max) {
                log("doOnRangeChanged :: reversed :: {} - {}", min, max)
                if (block.etMin === field) {
                    block.tilMax.error = null
                    if (min == max) {
                        block.tilMin.error = "$min = $max"
                    } else {
                        block.tilMin.error = "$min > $max"
                    }
                } else {
                    block.tilMin.error = null
                    if (min == max) {
                        block.tilMax.error = "$max = $min"
                    } else {
                        block.tilMax.error = "$max < $min"
                    }
                }
            } else {
                log("doOnRangeChanged :: {} - {}", min, max)
                ref.onRangeChanged(min, max)
                block.tilMin.error = null
                block.tilMax.error = null
            }
        }
    }

}