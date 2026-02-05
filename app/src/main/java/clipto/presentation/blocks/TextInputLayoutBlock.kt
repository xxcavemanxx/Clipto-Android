package clipto.presentation.blocks

import android.text.InputFilter
import android.text.InputType
import android.view.View
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.setTextWithSelection
import clipto.common.extensions.toNullIfEmpty
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.textfield.TextInputLayout
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_text_input_layout.view.*

class TextInputLayoutBlock<C>(
    private val text: CharSequence?,
    private val hint: CharSequence?,
    private val helperText: CharSequence? = null,
    private val filters: Array<InputFilter> = emptyArray(),
    private val changedTextProvider: () -> CharSequence? = { text },
    private val onTextChanged: (text: CharSequence?) -> CharSequence? = { null },
    private val maxLines: Int = 1,
    private val counterEnabled: Boolean = false,
    private val counterMaxLength: Int = -1,
    private val enabled: Boolean = true,
    private val inputType: TextInputType = TextInputType.DEFAULT,
    private val endIconClickListener: View.OnClickListener? = null,
    private val getSuffixChanges: (callback: (suffix: CharSequence) -> Unit) -> Unit = {},
    customLayoutRes: Int = R.layout.block_text_input_layout
) : BlockItem<C>() {

    enum class TextInputType {
        NUMBER_SIGNED, NUMBER, DEFAULT, NULL
    }

    override val layoutRes: Int = customLayoutRes

    private fun onTextChanged(text: CharSequence?): CharSequence? = onTextChanged.invoke(text)

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return item is TextInputLayoutBlock
                && layoutRes == item.layoutRes
                && inputType == item.inputType
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is TextInputLayoutBlock &&
                hint == item.hint &&
                maxLines == item.maxLines &&
                filters.contentEquals(item.filters) &&
                text == item.text &&
                counterEnabled == item.counterEnabled &&
                counterMaxLength == item.counterMaxLength &&
                enabled == item.enabled &&
                inputType == item.inputType &&
                helperText == item.helperText

    override fun onInit(context: C, block: View) {
        block.textInputEditText.doAfterTextChanged { text ->
            val ref = block.tag
            if (ref is TextInputLayoutBlock<*>) {
                val error = ref.onTextChanged(text)
                block as TextInputLayout
                block.error = error
            }
        }
        block as TextInputLayout
        getSuffixChanges {
            block.suffixText = it
        }
    }

    override fun onBind(context: C, block: View) {
        val text = changedTextProvider.invoke()
        block.tag = null
        val ctx = block.context
        block as TextInputLayout
        val editText = block.textInputEditText
        if (!editText.filters.contentEquals(filters)) {
            editText.filters = filters
        }
        val isSingleLine = maxLines == 1
        if (isSingleLine != (editText.maxLines == 1)) {
            editText.isSingleLine = isSingleLine
        }
        if (editText.maxLines != maxLines) {
            editText.maxLines = maxLines
        }
        if (block.hint != hint) {
            block.hint = hint
        }
        block.isCounterEnabled = counterEnabled
        if (counterEnabled) {
            block.counterMaxLength = counterMaxLength
        }
        if (block.isEnabled != enabled) {
            block.isEnabled = enabled
            if (enabled) {
                editText.setTextColor(ctx.getTextColorPrimary())
            } else {
                editText.setTextColor(ctx.getTextColorSecondary())
            }
        }
        if (helperText != null) {
            block.helperText = helperText
            block.isHelperTextEnabled = true
        } else {
            block.isHelperTextEnabled = false
        }
        when (inputType) {
            TextInputType.NUMBER_SIGNED -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            TextInputType.NUMBER -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            TextInputType.NULL -> {
                editText.inputType = InputType.TYPE_NULL
                editText.setTextColor(ctx.getTextColorSecondary())
            }
            else -> Unit
        }
        if (endIconClickListener != null && block.endIconMode == TextInputLayout.END_ICON_CUSTOM) {
            block.setEndIconOnClickListener(endIconClickListener)
        }
        if (editText.text?.toNullIfEmpty(trim = false) != text?.toNullIfEmpty(trim = false)) {
            editText.setTextWithSelection(text)
        }
        block.error = null
        block.tag = this
    }

}