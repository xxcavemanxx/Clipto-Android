package clipto.presentation.blocks

import android.content.Context
import android.view.View
import clipto.common.extensions.gone
import clipto.common.extensions.showToast
import clipto.common.extensions.visible
import clipto.extensions.getBackgroundHighlightColor
import clipto.extensions.getColorPositive
import clipto.extensions.getTextColorPrimary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_progress.view.*

class ProgressBlock<C>(
    private val context: Context,
    private val id: String,
    private val progress: Int = 0,
    private val label: String? = null,
    private val trackColor: Int = context.getBackgroundHighlightColor(),
    private val indicatorColor: Int = context.getColorPositive(),
    private val textColor: Int = context.getTextColorPrimary(),
    private val actionIcon: Int,
    private val actionTitle: Int,
    private val actionListener: (context: C) -> Unit = {},
    private val blockClickListener: (context: C) -> Unit = {},
    private val blockLongClickListener: (() -> Unit)? = null,
    private val contentModificationState: Long = 0
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_progress

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is ProgressBlock<*> &&
                item.id == id

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ProgressBlock &&
                item.contentModificationState == contentModificationState &&
                item.label == label &&
                item.progress == progress &&
                item.trackColor == trackColor &&
                item.indicatorColor == indicatorColor &&
                item.actionIcon == actionIcon &&
                item.actionTitle == actionTitle &&
                item.textColor == textColor

    override fun onInit(context: C, block: View) {
        block.setOnClickListener {
            getBlockRef(block)?.blockClickListener?.invoke(context)
        }
        block.setOnLongClickListener {
            getBlockRef(block)?.blockLongClickListener?.invoke()
            true
        }
        block.ivAction.setOnClickListener {
            getBlockRef(block)?.actionListener?.invoke(context)
        }
        block.ivAction.setOnLongClickListener {
            getBlockRef(block)?.let {
                block.context.showToast(it.actionTitle)
            }
            true
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        block.tvProgressTitle.let {
            it.setTextColor(textColor)
            it.text = label
        }
        block.lpProgress.let {
            it.setIndicatorColor(indicatorColor)
            it.trackColor = trackColor
            it.progress = progress
        }
        block.ivAction.let {
            if (actionIcon != 0) {
                it.setImageResource(actionIcon)
                it.contentDescription = block.context.getString(actionTitle)
                it.visible()
            } else {
                it.gone()
            }
        }
    }

    private fun getBlockRef(block: View): ProgressBlock<C>? = block.tag as? ProgressBlock<C>

}