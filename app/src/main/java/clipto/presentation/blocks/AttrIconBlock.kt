package clipto.presentation.blocks

import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.DrawableRes
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attr_icon.view.*

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
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is AttrIconBlock<*>) {
                ref.onClicked?.invoke()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        block.tvTitle.text = title
        block.ivIcon.setImageResource(iconRes)
        if (iconColor != null) {
            block.ivIcon.imageTintList = ColorStateList.valueOf(iconColor)
        }
        block.isClickable = onClicked != null
    }

}