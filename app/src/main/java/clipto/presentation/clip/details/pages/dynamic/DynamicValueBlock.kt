package clipto.presentation.clip.details.pages.dynamic

import android.view.View
import clipto.dynamic.DynamicValueType
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_dynamic_value.view.*

class DynamicValueBlock(
        private val type: DynamicValueType,
        private val value: CharSequence,
        val clickHandler: (type: DynamicValueType, snapshotValue: String?) -> Unit
) : BlockItem<DynamicValuesPageFragment>(), View.OnClickListener, View.OnLongClickListener {

    override val layoutRes: Int = R.layout.block_clip_details_dynamic_value

    override fun areContentsTheSame(item: BlockItem<DynamicValuesPageFragment>): Boolean =
            item is DynamicValueBlock &&
                    item.type == type &&
                    item.value == value

    override fun onClick(v: View?) {
        val ref = v?.tag
        if (ref is DynamicValueBlock) {
            ref.clickHandler.invoke(ref.type, null)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        val ref = v?.tag
        if (ref is DynamicValueBlock) {
            val snapshotValue = v.etValue?.text?.toString()
            ref.clickHandler.invoke(ref.type, snapshotValue)
        }
        return true
    }

    override fun onInit(fragment: DynamicValuesPageFragment, block: View) {
        block.setOnLongClickListener(this)
        block.setOnClickListener(this)
    }

    override fun onBind(fragment: DynamicValuesPageFragment, block: View) {
        block.tag = this
        block.etValue.text = value
        block.tvTitle.setText(type.titleRes)
        block.tvPlaceholder.text = type.getPlaceholderValue()
    }

}