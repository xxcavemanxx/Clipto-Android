package clipto.presentation.blocks.bottomsheet

import com.wb.clipboard.databinding.BlockObjectNameReadonlyBinding
import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.CallSuper
import clipto.common.extensions.setDebounceClickListener
import clipto.common.misc.ThemeUtils
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class ObjectNameReadOnlyBlock<C>(
    private val uid: String?,
    private val name: CharSequence?,
    private val color: String?,
    private val iconRes: Int,
    private val hideHint: Boolean = true,
    private val actionIconRes: Int? = null,
    private val actionIconColor: Int? = null,
    private val onActionClick: () -> Unit = {},
    private val onShowHint: (() -> Unit)? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_object_name_readonly

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        super.areItemsTheSame(item) &&
                item is ObjectNameReadOnlyBlock &&
                item.uid == uid

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ObjectNameReadOnlyBlock &&
                item.name == name &&
                item.color == color &&
                item.hideHint == hideHint &&
                item.actionIconRes == actionIconRes &&
                item.actionIconColor == actionIconColor

    @CallSuper
    override fun onInit(context: C, block: View) {
        val binding = BlockObjectNameReadonlyBinding.bind(block)
        binding.tvName.setDebounceClickListener { getRef(block)?.onShowHint?.invoke() }
        binding.ivAction.setDebounceClickListener { getRef(block)?.onActionClick?.invoke() }
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockObjectNameReadonlyBinding.bind(block)
        val ctx = block.context

        val colorInt = color?.let { ThemeUtils.getColor(ctx, it) } ?: ctx.getTextColorSecondary()
        binding.ivIcon.imageTintList = ColorStateList.valueOf(colorInt)
        binding.ivIcon.setImageResource(iconRes)

        binding.tvName.text = name

        if (hideHint && onShowHint != null) {
            binding.tvName.isClickable = true
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            binding.tvName.isClickable = false
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }

        if (actionIconRes != null) {
            val iconColor = actionIconColor ?: ctx.getTextColorSecondary()
            binding.ivAction.imageTintList = ColorStateList.valueOf(iconColor)
            binding.ivAction.setImageResource(actionIconRes)
        } else {
            binding.ivAction.setImageDrawable(null)
        }

        block.tag = this
    }

    private fun getRef(view: View): ObjectNameReadOnlyBlock<*>? = view.tag as? ObjectNameReadOnlyBlock<*>

}
