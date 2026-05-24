package clipto.presentation.clip.details.pages.dynamic

import com.wb.clipboard.databinding.BlockClipDetailsDynamicValueBinding
import android.view.View
import clipto.dynamic.DynamicValueType
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        if (v == null) return
        val binding = BlockClipDetailsDynamicValueBinding.bind(v)
        val ref = v.tag
        if (ref is DynamicValueBlock) {
            ref.clickHandler.invoke(ref.type, null)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        if (v == null) return false
        val binding = BlockClipDetailsDynamicValueBinding.bind(v)
        val ref = v.tag
        if (ref is DynamicValueBlock) {
            val snapshotValue = binding.etValue?.text?.toString()
            ref.clickHandler.invoke(ref.type, snapshotValue)
        }
        return true
    }

    override fun onInit(fragment: DynamicValuesPageFragment, block: View) {
        val binding = BlockClipDetailsDynamicValueBinding.bind(block)
        block.setOnLongClickListener(this)
        block.setOnClickListener(this)
    }

    override fun onBind(fragment: DynamicValuesPageFragment, block: View) {
        val binding = BlockClipDetailsDynamicValueBinding.bind(block)
        block.tag = this
        binding.etValue.text = value
        binding.tvTitle.setText(type.titleRes)
        binding.tvPlaceholder.text = type.getPlaceholderValue()
    }

}
