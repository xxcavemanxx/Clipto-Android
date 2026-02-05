package clipto.presentation.main.nav.blocks

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import clipto.common.extensions.*
import clipto.domain.Filter
import clipto.domain.SnippetKit
import clipto.domain.isSame
import clipto.extensions.getIconColor
import clipto.extensions.getIconRes
import clipto.extensions.getIndicatorRes
import clipto.extensions.getTitle
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockItemViewHolder
import clipto.presentation.main.nav.MainNavFragment
import clipto.presentation.main.nav.MainNavViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_filter.view.*

@SuppressLint("ClickableViewAccessibility")
class FilterBlock(
    val viewModel: MainNavViewModel,
    val filter: Filter,
    val uid: String? = filter.uid,
    val hasActiveAutoRule: Boolean = filter.hasActiveAutoRule(),
    val iconColor: Int = filter.getIconColor(viewModel.app),
    val isActive: Boolean = viewModel.isActive(filter),
    val description: String? = filter.description,
    val iconRes: Int = filter.getIconRes(),
    val count: Long? = filter.notesCount,
    val limit: Int? = filter.limit,
    val name: String? = filter.name,
    val manualSort: Boolean = false,
    val kit: SnippetKit? = filter.snippetKit
) : BlockItem<MainNavFragment>(), View.OnLongClickListener, View.OnClickListener {

    override val layoutRes: Int = R.layout.block_filter

    override fun onLongClick(v: View?): Boolean {
        getRef(v)?.let { ref ->
            viewModel.onOpenFilter(ref.filter)
        }
        return true
    }

    override fun onClick(v: View?) {
        getRef(v)?.let { ref ->
            viewModel.onApplyFilter(ref.filter)
        }
    }

    override fun areItemsTheSame(item: BlockItem<MainNavFragment>): Boolean {
        return super.areItemsTheSame(item) && item is FilterBlock && item.uid == uid
    }

    override fun areContentsTheSame(item: BlockItem<MainNavFragment>): Boolean =
        item is FilterBlock &&
                item.isActive == isActive &&
                item.count == count &&
                item.hasActiveAutoRule == hasActiveAutoRule &&
                item.iconColor == iconColor &&
                item.iconRes == iconRes &&
                item.limit == limit &&
                item.manualSort == manualSort &&
                item.name == name &&
                item.description == description &&
                item.kit.isSame(kit)

    override fun onInit(context: MainNavFragment, holder: BlockItemViewHolder<MainNavFragment, *>) {
        val block = holder.itemView
        block.setOnClickListener(this)
        block.setOnLongClickListener(this)
        block.ivMore.setDebounceClickListener { getRef(block)?.filter?.let { viewModel.onOpenFilter(it) } }
        block.ivDragView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                context.manualSortHelper.startDrag(holder)
            }
            false
        }
        block.iconView.setOnTouchListener { v, event ->
            if (block.ivDragView.isVisible()) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    context.manualSortHelper.startDrag(holder)
                }
            }
            false
        }
    }

    override fun onBind(context: MainNavFragment, block: View) {
        block.tag = this
        val ctx = block.context
        block.iconView?.imageTintList = ColorStateList.valueOf(iconColor)
        val indicatorRes = filter.getIndicatorRes(viewModel.userState.getUserId())
        if (indicatorRes != null) {
            block.ivIndicator?.setImageResource(indicatorRes)
            block.ivIndicator?.visible()
        } else {
            block.ivIndicator?.gone()
        }
        block.iconView?.setImageResource(iconRes)
        if (count == null) {
            block.nameView?.text = filter.getTitle(ctx)
        } else {
            block.nameView?.text = StyleHelper.getFilterLabel(ctx, filter.getTitle(ctx), filter.notesCount, filter.limit)
        }
        block.ivDragView?.setVisibleOrGone(manualSort)
        if (isActive) {
            block.setBackgroundResource(R.drawable.bg_filter_item_active)
        } else {
            block.setBackgroundResource(R.drawable.bg_filter_item_inactive)
        }
    }

    private fun getRef(view: View?): FilterBlock? = view?.tag as? FilterBlock

}