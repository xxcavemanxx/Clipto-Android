package clipto.presentation.blocks.domain

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.cache.AppColorCache
import clipto.common.misc.ThemeUtils
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_color.view.*

class ColorWheelBlock<C>(
    private val selectedColor: String?,
    private val colorsMatrix: List<List<String?>>,
    private val onChangeColor: (color: String?) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_color_wheel

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ColorWheelBlock<*> &&
                item.selectedColor == selectedColor

    override fun onInit(context: C, block: View) {
        block as RecyclerView
        val ctx = block.context

        val adapter = ColorWheelAdapter(
            context = ctx,
            selectedColor = null,
            colorsMatrix = colorsMatrix
        ) {
            val ref = block.tag
            if (ref is ColorWheelBlock<*>) {
                ref.onChangeColor(it)
            }
        }
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        block.adapter = adapter
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        val adapter = block.adapter as ColorWheelAdapter
        val selectedIndex = colorsMatrix
            .find { it.contains(selectedColor) }
            ?.indexOf(selectedColor) ?: -1
        adapter.changeSelectedColor(selectedColor)
        if (selectedIndex != -1) {
            block.layoutManager?.scrollToPosition(selectedIndex)
        }
        block.tag = this
    }

}

private class ColorWheelAdapter(
    private val context: Context,
    private var selectedColor: String?,
    private val colorsMatrix: List<List<String?>>,
    private val clickHandler: (selectedColor: String?) -> Unit
) : RecyclerView.Adapter<ColorWheelAdapter.ViewHolder>(), View.OnClickListener {

    private var matrixIndex: Int
    private var colors: List<String?>
    private val colorNull = ColorStateList.valueOf(ThemeUtils.getColor(context, R.attr.colorOnSurface))

    init {
        matrixIndex = colorsMatrix.indexOfFirst { it.contains(selectedColor) }
        if (matrixIndex == -1) {
            matrixIndex = 0
        }
        colors = colorsMatrix.getOrNull(matrixIndex) ?: emptyList()
    }

    override fun getItemCount(): Int = colors.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(position, colors[position])
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    override fun onClick(v: View?) {
        val holder = v?.tag
        if (holder is ViewHolder) {
            if (selectedColor == holder.color) {
                matrixIndex = if (matrixIndex >= colorsMatrix.size - 1) 0 else matrixIndex + 1
                colors = colorsMatrix.getOrNull(matrixIndex) ?: colorsMatrix[0]
                selectedColor = colors.getOrNull(holder.colorPosition)
            } else {
                selectedColor = holder.color
            }
            clickHandler.invoke(selectedColor)
            refresh()
        }
    }

    fun changeSelectedColor(selectedColor: String?) {
        this.selectedColor = selectedColor
        matrixIndex = colorsMatrix.indexOfFirst { it.contains(selectedColor) }
        if (matrixIndex == -1) {
            matrixIndex = 0
        }
        colors = colorsMatrix.getOrNull(matrixIndex) ?: emptyList()
        refresh()
    }

    private fun refresh() {
        notifyDataSetChanged()
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false)
    ) {

        private val colorView = itemView.colorView
        var color: String? = null
        var colorPosition = 0

        init {
            colorView.tag = this
            colorView.setOnClickListener(this@ColorWheelAdapter)
        }

        fun bindTo(position: Int, color: String?) {
            this.color = color
            this.colorPosition = position
            val drawable = itemView.background as LayerDrawable
            val colorInt = ThemeUtils.getColor(context, color)
            drawable.getDrawable(0).setTint(colorInt)
            if (color == selectedColor) {
                colorView.imageTintList =
                    if (color == null) {
                        colorNull
                    } else {
                        val colorOnSurface = AppColorCache.getColorOnSurface(colorInt)
                        ColorStateList.valueOf(colorOnSurface)
                    }
                colorView.setImageResource(R.drawable.item_color_checked)
            } else {
                colorView.setImageDrawable(null)
            }
        }
    }
}