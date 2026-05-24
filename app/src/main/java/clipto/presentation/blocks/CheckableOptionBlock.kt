package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockCheckableOptionBinding
import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.*
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class CheckableOptionBlock<T, C>(
    val option: Option<T>,
    val onClicked: (model: T) -> Unit
) : BlockItem<C>() {

    data class Option<T>(
        val model: T,
        val checked: Boolean,
        val iconRes: Int? = null,
        val title: CharSequence?,
        val iconColor: Int? = null
    )

    override val layoutRes: Int = R.layout.block_checkable_option

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) &&
                item is CheckableOptionBlock<*, *> &&
                item.option.model == option.model
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is CheckableOptionBlock<*, *> &&
                item.option.checked == option.checked &&
                item.option.iconRes == option.iconRes &&
                item.option.iconColor == option.iconColor &&
                item.option.title == option.title
    }

    override fun onInit(context: C, block: View) {
        val binding = BlockCheckableOptionBinding.bind(block)
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is CheckableOptionBlock<*, *>) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockCheckableOptionBinding.bind(block)
        block.tag = this
        binding.tvName.text = option.title
        binding.tvName.setBold(option.checked)
        binding.ivSelected.setVisibleOrGone(option.checked)
        if (option.iconRes != null) {
            binding.ivIcon.imageTintList = ColorStateList.valueOf(option.iconColor ?: block.context.getTextColorSecondary())
            binding.ivIcon.setImageResource(option.iconRes)
            binding.ivIcon.visible()
        } else {
            binding.ivIcon.gone()
        }
    }

    private fun onClicked() {
        onClicked.invoke(option.model)
    }

}
