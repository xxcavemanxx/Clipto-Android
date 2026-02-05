package clipto.presentation.clip.details.pages.dynamic

import android.view.View
import clipto.common.extensions.debounce
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.provider.IFieldProvider
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_dynamic_field.view.*

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
        block.setOnClickListener(debounce())
    }

    override fun onBind(fragment: DynamicValuesPageFragment, block: View) {
        block.tag = this
        block.tvTitle.setText(provider.getTitleRes())
        block.tvDescription.setText(provider.getDescriptionRes())
    }

}