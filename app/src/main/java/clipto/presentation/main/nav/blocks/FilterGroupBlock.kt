package clipto.presentation.main.nav.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.domain.Filter
import clipto.domain.SortBy
import clipto.extensions.getTitle
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.main.nav.MainNavFragment
import clipto.presentation.main.nav.MainNavViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_filter_group.view.*

class FilterGroupBlock(
    val viewModel: MainNavViewModel,
    val filter: Filter,
    val expanded: Boolean,
    val onClick: () -> Unit,
    val onActionClick: (fragment: Fragment) -> Unit,
    val uid: String? = filter.uid,
    val notesCount: Int = filter.notesCount.toInt(),
    val limit: Int? = null,
    val sortBy: SortBy = filter.sortBy
) : BlockItem<MainNavFragment>(), View.OnLongClickListener {

    override val layoutRes: Int = R.layout.block_filter_group

    override fun areContentsTheSame(item: BlockItem<MainNavFragment>): Boolean =
        item is FilterGroupBlock &&
                item.expanded == expanded &&
                item.notesCount == notesCount &&
                item.sortBy == sortBy &&
                item.uid == uid &&
                item.limit == limit

    override fun onLongClick(v: View?): Boolean {
        val ref = v?.tag
        if (ref is FilterGroupBlock) {
            viewModel.onOpenFilter(ref.filter)
        }
        return true
    }

    override fun onInit(context: MainNavFragment, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is FilterGroupBlock) {
                ref.onClick.invoke()
            }
        }
        block.setOnLongClickListener(this)
        block.actionView.setDebounceClickListener {
            val ref = block.tag
            if (ref is FilterGroupBlock) {
                ref.onActionClick.invoke(context)
            }
        }
        block.ivMore.setDebounceClickListener {
            val ref = block.tag
            if (ref is FilterGroupBlock) {
                viewModel.onOpenFilter(ref.filter)
            }
        }
    }

    override fun onBind(context: MainNavFragment, block: View) {
        val name = filter.getTitle(block.context)
        if (notesCount > 0) {
            block.nameView.text = StyleHelper.getFilterLabel(block.context, name, notesCount.toLong(), limit)
        } else {
            block.nameView.text = name
        }
        block.iconView.setImageResource(getIconRes())
        block.tag = this
    }

    private fun getIconRes(): Int = if (expanded) R.drawable.filter_group_opened else R.drawable.filter_group_closed

}