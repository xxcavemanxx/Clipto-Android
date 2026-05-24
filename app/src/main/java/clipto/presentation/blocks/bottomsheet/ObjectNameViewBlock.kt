package clipto.presentation.blocks.bottomsheet

import com.wb.clipboard.databinding.BlockObjectNameViewBinding
import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.common.misc.ThemeUtils
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        val binding = BlockObjectNameViewBinding.bind(block)
        binding.ivEdit.setDebounceClickListener { getRef(block)?.onEdit?.invoke() }
        binding.ivDelete.setDebounceClickListener { getRef(block)?.onDelete?.invoke() }
        binding.tvName.setDebounceClickListener { getRef(block)?.onShowHint?.invoke() }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockObjectNameViewBinding.bind(block)
        val ctx = block.context

        val colorInt = color?.let { ThemeUtils.getColor(ctx, it) } ?: ctx.getTextColorSecondary()
        binding.ivIcon.imageTintList = ColorStateList.valueOf(colorInt)
        binding.ivIcon.setImageResource(iconRes)

        binding.tvName.text = name

        if (hideHint) {
            binding.tvName.isClickable = true
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            binding.tvName.isClickable = false
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }

        block.tag = this
    }

    private fun getRef(view: View): ObjectNameViewBlock<*>? = view.tag as? ObjectNameViewBlock<*>

}
