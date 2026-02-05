package clipto.presentation.config.fonts

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import clipto.cache.AppTextCache
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.Font
import com.wb.clipboard.R
import clipto.presentation.common.widget.ColorfulTagSpan
import kotlinx.android.synthetic.main.item_font.view.*

class FontsAdapter(
    val context: Context,
    val items: List<Font>,
    val viewModel: FontsViewModel,
    val clickHandler: (font: Font) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = ViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(items[position])
        }
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_font, parent, false)) {

        var font: Font? = null

        init {
            itemView.setOnClickListener {
                font?.let { clickHandler.invoke(it) }
                notifyDataSetChanged()
            }
            itemView.check.setOnCheckedChangeListener { _, isChecked ->
                font?.visible = isChecked
            }
        }

        fun bind(font: Font) {
            this.font = font
            itemView.textView.setText(font.titleRes)
            if (font.id == viewModel.getTextFont()) {
                itemView.textView.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            } else {
                itemView.textView.typeface = null
            }
            itemView.check.isChecked = font.visible
            itemView.tagsView.text = AppTextCache.getOrPut(font.uid, AppTextCache.TYPE_FONT) {
                SimpleSpanBuilder()
                        .also { span ->
                            font.languages.sortedBy { it.ordinal }.forEachIndexed { index, l ->
                                val lang = itemView.resources.getString(l.titleRes)
                                span.append(lang, ColorfulTagSpan(lang))
                                if (index < font.languages.size - 1) {
                                    span.append("       ")
                                }
                            }
                        }
                        .build()
            }
        }

        fun onItemSelected() {
            itemView.setBackgroundColor(ThemeUtils.getColor(context, R.attr.listItemSelected))
        }

        fun onItemClear() {
            itemView.background = ThemeUtils.getDrawable(context, R.attr.selectableItemBackground)
        }
    }

}