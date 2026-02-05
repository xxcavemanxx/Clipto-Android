package clipto.presentation.blocks.domain

import android.content.res.ColorStateList
import android.view.View
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.Units
import clipto.domain.Theme
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_rune_settings_theme.view.*

class ThemeBlock<C>(
    private val theme: Theme,
    private val selected: Boolean = false,
    private val colorListItemSelected: Int,
    private val colorPrimaryInverse: Int,
    private val colorAccent: Int,
    private val dense: Boolean = false,
    private val onClickListener: (theme: Theme) -> Unit
) : BlockItem<C>(), View.OnClickListener {

    override val layoutRes: Int = R.layout.item_rune_settings_theme

    override fun onClick(v: View?) {
        val ref = v?.tag
        if (ref is ThemeBlock<*>) {
            ref.onClickListener.invoke(ref.theme)
        }
    }

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is ThemeBlock &&
                item.theme == theme

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ThemeBlock &&
                item.selected == selected &&
                item.colorListItemSelected == colorListItemSelected &&
                item.colorPrimaryInverse == colorPrimaryInverse &&
                item.colorAccent == colorAccent &&
                item.dense == dense

    override fun onInit(context: C, block: View) {
        block.contentView.setOnClickListener(this)
        block.radioButton.setOnClickListener(this)
    }

    override fun onBind(context: C, block: View) {
        block.contentView.tag = this
        block.radioButton.tag = this

        block.layoutParams?.width = if (dense) widthDense else widthDefault

        // content
        block.contentView.setCardBackgroundColor(colorPrimaryInverse)

        // title
        val ctx = block.context
        block.titleView.text = ctx.getString(theme.titleRes)

        // radio
        block.radioButton.isChecked = selected
        block.radioButton.buttonTintList = ColorStateList.valueOf(colorAccent)

        // lines
        block.line2.setVisibleOrGone(!dense)

        block.line1.imageTintList = ColorStateList.valueOf(colorListItemSelected)
        block.line2.imageTintList = ColorStateList.valueOf(colorListItemSelected)
        block.line1.refreshDrawableState()
        block.line2.refreshDrawableState()
    }

    companion object {
        private val widthDefault = Units.DP.toPx(104f).toInt()
        private val widthDense = Units.DP.toPx(80f).toInt()
    }

}