package clipto.presentation.blocks

import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.*
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_checkable_option.view.*

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
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is CheckableOptionBlock<*, *>) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        block.tvName.text = option.title
        block.tvName.setBold(option.checked)
        block.ivSelected.setVisibleOrGone(option.checked)
        if (option.iconRes != null) {
            block.ivIcon.imageTintList = ColorStateList.valueOf(option.iconColor ?: block.context.getTextColorSecondary())
            block.ivIcon.setImageResource(option.iconRes)
            block.ivIcon.visible()
        } else {
            block.ivIcon.gone()
        }
    }

    private fun onClicked() {
        onClicked.invoke(option.model)
    }

}