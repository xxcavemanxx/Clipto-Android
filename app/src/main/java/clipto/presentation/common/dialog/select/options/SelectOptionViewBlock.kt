package clipto.presentation.common.dialog.select.options

import android.annotation.SuppressLint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockItemViewHolder
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dialog_select_option_view.view.*

@SuppressLint("ClickableViewAccessibility")
class SelectOptionViewBlock(
    private val viewModel: SelectOptionsDialogViewModel,
    val live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>,
    val option: SelectOptionsDialogRequest.Option,
    val data: SelectOptionsDialogRequest,
    val enabled: Boolean = data.enabled,
    val title: String? = option.title,
    val value: String? = option.value,
) : BlockItem<SelectOptionsDialogFragment>() {

    override val layoutRes: Int = R.layout.block_dialog_select_option_view

    override fun areItemsTheSame(item: BlockItem<SelectOptionsDialogFragment>): Boolean {
        return item is SelectOptionViewBlock
                && item.option === option
    }

    override fun areContentsTheSame(item: BlockItem<SelectOptionsDialogFragment>): Boolean {
        return item is SelectOptionViewBlock
                && item.option == option
                && item.enabled == enabled
                && item.title == title
                && item.option == option
    }

    override fun onInit(context: SelectOptionsDialogFragment, holder: BlockItemViewHolder<SelectOptionsDialogFragment, *>) {
        val block = holder.itemView
        block.ivDrag.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                context.touchHelper.startDrag(holder)
            }
            false
        }
        block.vAction.setDebounceClickListener {
            val ref = block.tag
            if (ref is SelectOptionViewBlock) {
                viewModel.onEditOption(ref.data, ref.option, ref.live)
            }
        }
        block.ivDelete.setDebounceClickListener {
            val ref = block.tag
            if (ref is SelectOptionViewBlock) {
                viewModel.onDeleteOption(ref.data, ref.option, ref.live)
            }
        }
    }

    override fun onBind(context: SelectOptionsDialogFragment, block: View) {
        val ctx = block.context
        val colorValue = block.context.getTextColorSecondary()
        val colorKey = if (enabled) block.context.getTextColorPrimary() else colorValue
        block.tag = null
        block.isClickable = enabled
        block.vAction.isClickable = enabled
        block.ivDelete.setVisibleOrGone(enabled)
        block.ivDrag.setVisibleOrGone(enabled)
        block.tvText.text =
                when {
                    title != null && value != null -> {
                        SimpleSpanBuilder()
                                .append(title, ForegroundColorSpan(colorKey))
                                .append("\n")
                                .append(value, ForegroundColorSpan(colorValue), RelativeSizeSpan(0.7f))
                                .build()
                    }
                    title != null -> title
                    value != null -> value
                    !data.withTitle -> ctx.getString(R.string.dynamic_field_attr_common_value)
                    else -> {
                        SimpleSpanBuilder()
                                .append(ctx.getString(R.string.dynamic_field_attr_common_label), ForegroundColorSpan(colorKey))
                                .append("\n")
                                .append(ctx.getString(R.string.dynamic_field_attr_common_value), ForegroundColorSpan(colorValue), RelativeSizeSpan(0.7f))
                                .build()
                    }
                }
        block.tag = this
    }

}