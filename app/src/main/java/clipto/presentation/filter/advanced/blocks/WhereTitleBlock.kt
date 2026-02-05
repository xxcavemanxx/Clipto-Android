package clipto.presentation.filter.advanced.blocks

import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import clipto.common.extensions.inBrackets
import clipto.common.extensions.setDebounceClickListener
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.Filter
import clipto.extensions.getTextColorSecondarySpan
import clipto.extensions.getTitleRes
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.advanced.AdvancedFilterFragment
import clipto.presentation.filter.advanced.AdvancedFilterViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_advanced_filter_where_title.view.*

class WhereTitleBlock(
        private val viewModel: AdvancedFilterViewModel,
        private val titleRes: Int,
        private val whereType: Filter.WhereType,
        private val whereOptions: Array<Filter.WhereType> = Filter.WhereType.values(),
        private val onWhereTypeChanged: (type: Filter.WhereType) -> Unit = {},
        private val onClickListener: () -> Unit = {}
) : BlockItem<AdvancedFilterFragment>() {

    override val layoutRes: Int = R.layout.block_advanced_filter_where_title

    override fun areItemsTheSame(item: BlockItem<AdvancedFilterFragment>): Boolean {
        return item is WhereTitleBlock && item.titleRes == titleRes
    }

    override fun areContentsTheSame(item: BlockItem<AdvancedFilterFragment>): Boolean =
            item is WhereTitleBlock &&
                    item.titleRes == titleRes &&
                    item.whereType == whereType &&
                    item.whereOptions.contentEquals(whereOptions)

    override fun onInit(fragment: AdvancedFilterFragment, block: View) {
        val ctx = block.context

        var options = emptyArray<Filter.WhereType>()
        val adapter = ArrayAdapter<String>(block.context, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        val popup = ListPopupWindow(ctx, null, R.attr.listPopupWindowStyle)
        popup.setDropDownGravity(Gravity.END)
        popup.setAdapter(adapter)
        popup.setOnItemClickListener { _, _, position, _ ->
            val ref = block.tag
            if (ref is WhereTitleBlock) {
                val newType = options.getOrNull(position) ?: return@setOnItemClickListener
                ref.bind(newType, block)
                ref.onWhereTypeChanged.invoke(newType)
            }
            popup.dismiss()
        }
        block.tvWhereBlockTitle.setDebounceClickListener {
            val ref = block.tag
            if (ref is WhereTitleBlock) {
                val newOptions = ref.whereOptions
                if (!newOptions.contentEquals(options)) {
                    adapter.clear()
                    adapter.addAll(newOptions.map { ctx.getString(it.getTitleRes()) })
                    options = newOptions
                }
                popup.anchorView = it
                popup.show()
            }
        }

        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is WhereTitleBlock) {
                ref.onClickListener.invoke()
            }
        }
    }

    override fun onBind(fragment: AdvancedFilterFragment, block: View) {
        block.tag = this
        bind(whereType, block)
    }

    private fun bind(whereType: Filter.WhereType, block: View) {
        val titleView = block.tvWhereBlockTitle
        val title = viewModel.string(titleRes)
        titleView.text = SimpleSpanBuilder()
                .append(title)
                .append(" ")
                .append(viewModel.string(whereType.getTitleRes()).inBrackets(),
                        block.context.getTextColorSecondarySpan()
                )
                .build()
    }

}