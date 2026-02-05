package clipto.presentation.blocks

import android.text.InputFilter
import android.view.View
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.setTextWithSelection
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_two_input.view.*

class TwoInputBlock<C>(
    private val firstText: String?,
    private val secondText: String?,
    private val enabled: Boolean = true,
    private val filters: Array<InputFilter> = emptyArray(),
    private val onFirstTextChanged: (text: CharSequence?) -> CharSequence? = { null },
    private val onSecondTextChanged: (text: CharSequence?) -> CharSequence? = { null }
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_two_input

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is TwoInputBlock
                && item.enabled == enabled
                && item.firstText == firstText
                && item.secondText == secondText
                && filters.contentEquals(item.filters)
    }

    override fun onInit(context: C, block: View) {
        val tilFirst = block.tilFirst
        val tilSecond = block.tilSecond
        block.etFirst.doAfterTextChanged {
            val ref = block.tag
            if (ref is TwoInputBlock<*>) {
                tilFirst.error = ref.onFirstTextChanged.invoke(it)
            }
        }
        block.etSecond.doAfterTextChanged {
            val ref = block.tag
            if (ref is TwoInputBlock<*>) {
                tilSecond.error = ref.onSecondTextChanged.invoke(it)
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = null
        val ctx = block.context
        val etFirst = block.etFirst
        val etSecond = block.etSecond
        val tilFirst = block.tilFirst
        val tilSecond = block.tilSecond

        if (!etFirst.filters.contentEquals(filters)) {
            etFirst.filters = filters
        }
        if (!etSecond.filters.contentEquals(filters)) {
            etSecond.filters = filters
        }

        tilFirst.isEnabled = enabled
        tilSecond.isEnabled = enabled
        etFirst.isEnabled = enabled
        etSecond.isEnabled = enabled

        val textColor =
            if (enabled) {
                ctx.getTextColorPrimary()
            } else {
                ctx.getTextColorSecondary()
            }
        etFirst.setTextColor(textColor)
        etSecond.setTextColor(textColor)

        if (firstText != etFirst.text?.toString()) {
            etFirst.setTextWithSelection(firstText)
        }

        if (secondText != etSecond.text?.toString()) {
            etSecond.setTextWithSelection(secondText)
        }

        tilFirst.error = null
        tilSecond.error = null

        block.tag = this
    }

}