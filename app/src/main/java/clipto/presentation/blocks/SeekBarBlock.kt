package clipto.presentation.blocks

import android.view.View
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueStringWithHeader
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_seekbar.view.*

class SeekBarBlock<C>(
    private val titleRes: Int,
    private val descriptionRes: Int,
    private val maxValue: Int,
    private val progress: Int,
    private val enabled: Boolean,
    private val boldHeader: Boolean = false,
    private val changeProvider: (progress: Int) -> Int
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_seekbar

    override fun areItemsTheSame(item: BlockItem<C>): Boolean = item is SeekBarBlock && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SeekBarBlock
                && enabled == item.enabled
                && progress == item.progress
                && maxValue == item.maxValue
                && boldHeader == item.boldHeader
                && descriptionRes == item.descriptionRes

    override fun onBind(context: C, block: View) {
        var colorKey = block.context.getTextColorPrimary()
        val colorValue = block.context.getTextColorSecondary()
        val titleView = block.titleView
        val valueView = block.valueView
        if (!enabled) {
            colorKey = colorValue
        }

        KeyValueStringWithHeader(
            titleView,
            colorKey,
            colorValue,
            titleRes,
            descriptionRes,
            boldHeader
        )
        val seekBar = block.seekBar
        seekBar.valueTo = maxValue.toFloat()
        seekBar.value = progress.toFloat()
        if (enabled) {
            seekBar.clearOnChangeListeners()
            seekBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val newValue = changeProvider.invoke(value.toInt())
                    valueView.text = StyleHelper.getLimit(block.context, newValue)
                }
            }
        }
        val newValue = changeProvider.invoke(progress)
        valueView.text = StyleHelper.getLimit(block.context, newValue)

        seekBar.isEnabled = enabled
        titleView.isEnabled = enabled
        valueView.isEnabled = enabled
    }

}