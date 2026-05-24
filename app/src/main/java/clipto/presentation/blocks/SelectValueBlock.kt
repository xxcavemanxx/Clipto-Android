package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockSelectValueBinding
import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.*
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class SelectValueBlock<T, C>(
    private val model: T,
    val checked: Boolean,
    val title: CharSequence?,
    val iconRes: Int? = null,
    val iconColor: Int? = null,
    private val onClicked: (model: T) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_select_value

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item)
                && item is SelectValueBlock<*, *>
                && item.model == model
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is SelectValueBlock<*, *>
                && item.checked == checked
                && item.title == title
                && item.iconRes == iconRes
                && item.iconColor == iconColor
    }

    override fun onInit(context: C, block: View) {
        val binding = BlockSelectValueBinding.bind(block)
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is SelectValueBlock<*, *>) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockSelectValueBinding.bind(block)
        binding.tvName.text = title
        binding.tvName.setBold(checked)
        binding.ivSelected.setVisibleOrGone(checked)
        if (iconRes != null) {
            binding.ivIcon.imageTintList = ColorStateList.valueOf(iconColor ?: block.context.getTextColorSecondary())
            binding.ivIcon.setImageResource(iconRes)
            binding.ivIcon.visible()
        } else {
            binding.ivIcon.gone()
        }
        block.tag = this
    }

    private fun onClicked() {
        onClicked.invoke(model)
    }

}
