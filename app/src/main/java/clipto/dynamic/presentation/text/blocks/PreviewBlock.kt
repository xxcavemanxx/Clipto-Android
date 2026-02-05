package clipto.dynamic.presentation.text.blocks

import android.view.View
import android.widget.TextViewExt
import androidx.fragment.app.Fragment
import clipto.domain.TextType
import clipto.dynamic.presentation.text.DynamicTextViewModel
import clipto.extensions.toExt
import clipto.extensions.withConfig
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class PreviewBlock(
    private val viewModel: DynamicTextViewModel,
    private val textType: TextType,
    private val text: CharSequence
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_preview

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is PreviewBlock
                && text == text
    }

    override fun onInit(fragment: Fragment, block: View) {
        block as TextViewExt
        viewModel.getListConfig().let { listConfig -> block.withConfig(listConfig.textFont, listConfig.textSize) }
    }

    override fun onBind(fragment: Fragment, block: View) {
        block as TextViewExt
        textType.toExt().apply(block, text, skipDynamicFieldsRendering = true)
    }

}