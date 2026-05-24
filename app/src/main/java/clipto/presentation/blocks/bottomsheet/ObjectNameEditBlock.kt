package clipto.presentation.blocks.bottomsheet

import com.wb.clipboard.databinding.BlockObjectNameEditBinding
import android.text.InputFilter
import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.showKeyboard
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        val binding = BlockObjectNameEditBinding.bind(block)
        binding.ivAction.setDebounceClickListener {
            val text = binding.etName.text.toString()
            getRef(block)?.onRename?.invoke(text)
        }
        binding.ivCancel.setDebounceClickListener {
            getRef(block)?.onCancelEdit?.invoke()
        }
        binding.etName.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockObjectNameEditBinding.bind(block)
        binding.etName.setText(text)
        binding.etName.hint = hint
        binding.etName.setSelection(text?.length ?: 0)
        binding.etName.showKeyboard()
        block.tag = this
    }

    private fun getRef(view: View): ObjectNameEditBlock<*>? = view.tag as? ObjectNameEditBlock<*>

}
