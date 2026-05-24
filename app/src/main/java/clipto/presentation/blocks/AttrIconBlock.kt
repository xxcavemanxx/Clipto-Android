package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockAttrIconBinding
import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.DrawableRes
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class AttrIconBlock<C>(
    private val id: String? = null,
    private val title: CharSequence,
    private val iconColor: Int? = null,
    @DrawableRes private val iconRes: Int,
    private val onClicked: (() -> Unit)? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_attr_icon

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is AttrIconBlock &&
                title == item.title

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is AttrIconBlock &&
                id == item.id &&
                iconRes == item.iconRes &&
                iconColor == item.iconColor

    override fun onInit(context: C, block: View) {
        val binding = BlockAttrIconBinding.bind(block)
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is AttrIconBlock<*>) {
                ref.onClicked?.invoke()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockAttrIconBinding.bind(block)
        block.tag = this
        binding.tvTitle.text = title
        binding.ivIcon.setImageResource(iconRes)
        if (iconColor != null) {
            binding.ivIcon.imageTintList = ColorStateList.valueOf(iconColor)
        }
        block.isClickable = onClicked != null
    }

}
