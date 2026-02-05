package clipto.dynamic.presentation.text.blocks

import android.text.Editable
import android.view.View
import android.widget.TextViewExt
import androidx.fragment.app.Fragment
import clipto.domain.TextType
import clipto.dynamic.FormField
import clipto.dynamic.presentation.text.DynamicTextViewModel
import clipto.extensions.toExt
import clipto.extensions.withConfig
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class TextBlock(
    private val viewModel: DynamicTextViewModel,
    private val fields: List<FormField>,
    private val textType: TextType,
    private val text: CharSequence,
    private val fieldValues: List<String?> = fields.map { it.getFieldValue() }
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_text

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is TextBlock
                && fieldValues == item.fieldValues
    }

    override fun onInit(context: Fragment, block: View) {
        block as TextViewExt
        viewModel.getListConfig().let { listConfig -> block.withConfig(listConfig.textFont, listConfig.textSize) }
        viewModel.dynamicTextHelper.bind(textView = block, fields = fields, onClicked = ::onFieldClicked)
        textType.toExt().apply(block, text, skipDynamicFieldsRendering = true)
    }

    override fun onBind(context: Fragment, block: View) {
        block as TextViewExt
        val watcher = block.weakTextWatcherTwo
        val editable = block.text
        if (editable is Editable) {
            watcher.afterTextChanged(editable)
        }
    }

    private fun onFieldClicked(formField: FormField) {
        fields.getOrNull(formField.index)?.let { viewModel.onFieldClicked(it) }
    }
}