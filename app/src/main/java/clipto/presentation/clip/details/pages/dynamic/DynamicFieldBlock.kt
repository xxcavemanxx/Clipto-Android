package clipto.presentation.clip.details.pages.dynamic

import com.wb.clipboard.databinding.BlockClipDetailsDynamicFieldBinding
import android.view.View
import clipto.common.extensions.debounce
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.provider.IFieldProvider
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class DynamicFieldBlock(
    private val provider: IFieldProvider<out DynamicField>,
    val onClicked: (provider: IFieldProvider<out DynamicField>) -> Unit
) : BlockItem<DynamicValuesPageFragment>(), View.OnClickListener {

    override val layoutRes: Int = R.layout.block_clip_details_dynamic_field

    override fun areContentsTheSame(item: BlockItem<DynamicValuesPageFragment>): Boolean =
        item is DynamicFieldBlock &&
                item.provider == provider

    override fun onClick(v: View?) {
        val ref = v?.tag
        if (ref is DynamicFieldBlock) {
            ref.onClicked.invoke(ref.provider)
        }
    }

    override fun onInit(fragment: DynamicValuesPageFragment, block: View) {
        val binding = BlockClipDetailsDynamicFieldBinding.bind(block)
        block.setOnClickListener(debounce())
    }

    override fun onBind(fragment: DynamicValuesPageFragment, block: View) {
        val binding = BlockClipDetailsDynamicFieldBinding.bind(block)
        block.tag = this
        binding.tvTitle.setText(provider.getTitleRes())
        binding.tvDescription.setText(provider.getDescriptionRes())
    }

}
