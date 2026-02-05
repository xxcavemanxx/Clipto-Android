package clipto.presentation.blocks

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import clipto.common.extensions.animateScale
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.widget.AutoCompleteItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_text_auto_complete.view.*

class TextAutoCompleteBlock<C>(
    @StringRes private val hintRes: Int,
    @StringRes private val actionTitleRes: Int,
    @DrawableRes private val actionIconRes: Int,
    private val maxLength: Int,
    private val selectedItems: List<String>,
    private val allItems: List<AutoCompleteItem>,
    private val onSelectListener: (text: String) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_text_auto_complete

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is TextAutoCompleteBlock<*> &&
                item.hintRes == hintRes &&
                item.actionIconRes == actionIconRes &&
                item.actionTitleRes == actionTitleRes &&
                item.maxLength == maxLength &&
                item.selectedItems == selectedItems &&
                item.allItems == allItems
    }

    override fun onBind(context: C, block: View) {
        val context = block.context
        val autoComplete = block.tvAutoComplete
        val buttonField = block.mbAutoComplete

        autoComplete.setHint(hintRes)
        autoComplete.setImeActionLabel(context.getString(actionTitleRes), EditorInfo.IME_ACTION_DONE)
        autoComplete
            .withInputMaxLength(maxLength)
            .withAllItemsProvider { allItems }
            .withOnItemClickListener(onSelectListener)
            .withSelectedItemsProvider { selectedItems }
            .withOnEnterListener(showResults = false) { it?.let(onSelectListener) }
            .withOnTextChangeListener {
                val isEmpty = it.isNullOrBlank()
                buttonField.animateScale(!isEmpty)
                autoComplete.hint =
                    if (isEmpty) {
                        context.getString(hintRes)
                    } else {
                        null
                    }
            }

        buttonField.setText(actionTitleRes)
        buttonField.setIconResource(actionIconRes)
        buttonField.setOnClickListener {
            val text = autoComplete.text
            autoComplete.text = null
            onSelectListener(text.toString())
        }
    }


}