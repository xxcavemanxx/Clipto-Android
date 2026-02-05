package clipto.presentation.blocks

import android.view.View
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.button.MaterialButtonToggleGroup
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_three_buttons_toggle.view.*

class ThreeButtonsToggleBlock<C>(
    private val firstButtonTextRes: Int,
    private val secondButtonTextRes: Int,
    private val thirdButtonTextRes: Int,
    private val onFirstButtonClick: View.OnClickListener,
    private val onSecondButtonClick: View.OnClickListener,
    private val onThirdButtonClick: View.OnClickListener,
    private val selectedButtonIndex: Int = 0
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_three_buttons_toggle

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is ThreeButtonsToggleBlock
                && firstButtonTextRes == item.firstButtonTextRes
                && secondButtonTextRes == item.secondButtonTextRes
                && thirdButtonTextRes == item.thirdButtonTextRes
                && selectedButtonIndex == item.selectedButtonIndex
    }

    override fun onInit(context: C, block: View) {
        block.btn1.setOnClickListener(onFirstButtonClick)
        block.btn2.setOnClickListener(onSecondButtonClick)
        block.btn3.setOnClickListener(onThirdButtonClick)
    }

    override fun onBind(context: C, block: View) {
        block as MaterialButtonToggleGroup
        block.btn1.setText(firstButtonTextRes)
        block.btn2.setText(secondButtonTextRes)
        block.btn3.setText(thirdButtonTextRes)
        when (selectedButtonIndex) {
            0 -> block.check(R.id.btn1)
            1 -> block.check(R.id.btn2)
            2 -> block.check(R.id.btn3)
        }
    }
}