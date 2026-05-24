package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListEmptyBinding
import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.ThemeUtils
import clipto.domain.Filter
import clipto.domain.Font
import clipto.domain.ListConfig
import clipto.extensions.*
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class EmptyStateBlock<C>(
    private val filter: Filter,
    private val listConfigGetter: () -> ListConfig
) : BlockItem<C>() {

    private val id: String? = filter.uid
    private val name: String? = filter.name
    private val color: String? = filter.color
    private val description: String? = filter.description
    private val isFolder: Boolean = filter.isFolder()
    private val iconRes = filter.getIconRes()

    override val layoutRes: Int = R.layout.block_main_list_empty

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is EmptyStateBlock<*>
                && item.color == color
                && item.name == name
                && item.description == description
                && item.isFolder == isFolder
                && item.iconRes == iconRes
                && item.id == id
    }

    override fun onBind(context: C, block: View) {
        val binding = BlockMainListEmptyBinding.bind(block)
        val listConfig = listConfigGetter.invoke()
        val font = Font.valueOf(listConfig.textFont) ?: Font.DEFAULT
        val ctx = block.context

        val color = filter.color
        binding.ivEmpty?.imageTintList =
            if (color != null) {
                ColorStateList.valueOf(ThemeUtils.getColor(ctx, color))
            } else {
                ColorStateList.valueOf(ctx.getTextColorPrimary())
            }
        binding.ivEmpty?.setImageResource(filter.getIconRes())

        val title = filter.getTitle(ctx)
        val desc = filter.description.toNullIfEmpty()
        val description = desc ?: filter.getEmptyLabel(ctx)
        val caption = ctx.getString(
            R.string.main_list_empty_caption,
            ctx.getString(R.string.main_list_empty_caption_three_dot),
            ctx.getString(R.string.main_list_empty_caption_three_dot)
        )

        binding.tvTitle.apply {
            textSize = listConfig.textSize + 4f
            typeface = font.typeface
            text = title
        }

        binding.tvDescription.apply {
            textSize = listConfig.textSize.toFloat()
            typeface = font.typeface
            TextTypeExt.MARKDOWN.apply(this, description)
        }

        binding.tvCaption.apply {
            textSize = maxOf(listConfig.textSize - 4f, 8f)
            typeface = font.typeface
            text = caption.takeIf { desc.isNullOrBlank() && !isFolder }
        }
    }
}
