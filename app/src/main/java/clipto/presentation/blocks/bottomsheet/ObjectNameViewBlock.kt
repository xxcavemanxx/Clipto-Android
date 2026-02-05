package clipto.presentation.blocks.bottomsheet

import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.common.misc.ThemeUtils
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_object_name_view.view.*

class ObjectNameViewBlock<C>(
    private val uid: String?,
    private val name: CharSequence?,
    private val color: String?,
    private val iconRes: Int,
    private val hideHint: Boolean,
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit,
    private val onShowHint: () -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_object_name_view

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        super.areItemsTheSame(item) &&
                item is ObjectNameViewBlock &&
                item.uid == uid

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ObjectNameViewBlock &&
                item.name == name &&
                item.color == color &&
                item.hideHint == hideHint

    override fun onInit(context: C, block: View) {
        block.ivEdit.setDebounceClickListener { getRef(block)?.onEdit?.invoke() }
        block.ivDelete.setDebounceClickListener { getRef(block)?.onDelete?.invoke() }
        block.tvName.setDebounceClickListener { getRef(block)?.onShowHint?.invoke() }
    }

    override fun onBind(context: C, block: View) {
        val ctx = block.context

        val colorInt = color?.let { ThemeUtils.getColor(ctx, it) } ?: ctx.getTextColorSecondary()
        block.ivIcon.imageTintList = ColorStateList.valueOf(colorInt)
        block.ivIcon.setImageResource(iconRes)

        block.tvName.text = name

        if (hideHint) {
            block.tvName.isClickable = true
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            block.tvName.isClickable = false
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }

        block.tag = this
    }

    private fun getRef(view: View): ObjectNameViewBlock<*>? = view.tag as? ObjectNameViewBlock<*>

}