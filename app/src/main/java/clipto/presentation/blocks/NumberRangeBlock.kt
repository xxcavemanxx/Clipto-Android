package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockNumberRangeBinding
import android.view.View
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.setTextWithSelection
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.extensions.log
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        val binding = BlockNumberRangeBinding.bind(block)
        binding.etMin.doAfterTextChanged { doOnRangeChanged(block, binding.etMin) }
        binding.etMax.doAfterTextChanged { doOnRangeChanged(block, binding.etMax) }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockNumberRangeBinding.bind(block)
        block.tag = null
        val ctx = block.context

        binding.tilMin.isEnabled = enabled
        binding.tilMax.isEnabled = enabled
        binding.etMin.isEnabled = enabled
        binding.etMax.isEnabled = enabled
        val textColor =
            if (enabled) {
                ctx.getTextColorPrimary()
            } else {
                ctx.getTextColorSecondary()
            }
        binding.etMin.setTextColor(textColor)
        binding.etMax.setTextColor(textColor)

        val minValueText = minValue?.toString()
        if (minValueText != binding.etMin.text?.toString()) {
            binding.etMin.setTextWithSelection(minValueText)
        }

        val maxValueText = maxValue?.toString()
        if (maxValueText != binding.etMax.text?.toString()) {
            binding.etMax.setTextWithSelection(maxValueText)
        }

        block.tag = this
    }

    private fun doOnRangeChanged(block: View, field: View) {
        val binding = BlockNumberRangeBinding.bind(block)
        val ref = block.tag
        if (ref is NumberRangeBlock<*>) {
            val min = binding.etMin.text?.toString()?.toIntOrNull()
            val max = binding.etMax.text?.toString()?.toIntOrNull()
            if (min != null && max != null && min >= max) {
                log("doOnRangeChanged :: reversed :: {} - {}", min, max)
                if (binding.etMin === field) {
                    binding.tilMax.error = null
                    if (min == max) {
                        binding.tilMin.error = "$min = $max"
                    } else {
                        binding.tilMin.error = "$min > $max"
                    }
                } else {
                    binding.tilMin.error = null
                    if (min == max) {
                        binding.tilMax.error = "$max = $min"
                    } else {
                        binding.tilMax.error = "$max < $min"
                    }
                }
            } else {
                log("doOnRangeChanged :: {} - {}", min, max)
                ref.onRangeChanged(min, max)
                binding.tilMin.error = null
                binding.tilMax.error = null
            }
        }
    }

}
