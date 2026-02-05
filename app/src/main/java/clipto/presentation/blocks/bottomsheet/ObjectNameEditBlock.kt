package clipto.presentation.blocks.bottomsheet

import android.text.InputFilter
import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.showKeyboard
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_object_name_edit.view.*

class ObjectNameEditBlock<C>(
    private val maxLength: Int,
    private val text: String?,
    private val hint: String?,
    private val onRename: (text: String) -> Unit,
    private val onCancelEdit: () -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_object_name_edit

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        super.areItemsTheSame(item) &&
                item is ObjectNameEditBlock<C> &&
                item.maxLength == maxLength

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ObjectNameEditBlock &&
                item.text == text &&
                item.hint == hint

    override fun onInit(context: C, block: View) {
        block.ivAction.setDebounceClickListener {
            val text = block.etName.text.toString()
            getRef(block)?.onRename?.invoke(text)
        }
        block.ivCancel.setDebounceClickListener {
            getRef(block)?.onCancelEdit?.invoke()
        }
        block.etName.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    override fun onBind(context: C, block: View) {
        block.etName.setText(text)
        block.etName.hint = hint
        block.etName.setSelection(text?.length ?: 0)
        block.etName.showKeyboard()
        block.tag = this
    }

    private fun getRef(view: View): ObjectNameEditBlock<*>? = view.tag as? ObjectNameEditBlock<*>

}