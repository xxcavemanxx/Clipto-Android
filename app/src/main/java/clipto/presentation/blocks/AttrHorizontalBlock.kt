package clipto.presentation.blocks

import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attr_horizontal.view.*

class AttrHorizontalBlock<C>(
    private val id: String? = null,
    private val title: CharSequence,
    private val value: CharSequence,
    private val onClicked: (() -> Unit)? = null,
    private val valueKey: String? = null,
    private val valueProvider: ((key: String?, callback: (value: String) -> Unit) -> Unit)? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_attr_horizontal

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is AttrHorizontalBlock &&
                title == item.title

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is AttrHorizontalBlock &&
                valueKey == item.valueKey &&
                value == item.value &&
                id == item.id

    override fun onInit(context: C, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is AttrHorizontalBlock<*>) {
                ref.onClicked?.invoke()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        block.tvTitle.text = title
        block.etValue.text = value
        block.isClickable = onClicked != null
        valueProvider?.invoke(valueKey) { newValue ->
            block.etValue.text = newValue
        }
    }

}