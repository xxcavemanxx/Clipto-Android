package clipto.presentation.common.fragment.attributed.blocks

import android.annotation.SuppressLint
import android.text.InputFilter
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import clipto.common.extensions.*
import clipto.domain.*
import clipto.extensions.TextTypeExt
import clipto.extensions.storeActiveFieldState
import clipto.extensions.withConfig
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import clipto.store.main.MainState
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attributed_object_description.view.*

@SuppressLint("ClickableViewAccessibility")
class DescriptionBlock<O : AttributedObject, S : AttributedObjectScreenState<O>>(
    private val dialogState: DialogState,
    private val mainState: MainState,
    private val screenState: S,
    private val description: String? = screenState.value.description,
    private val onChanged: (description: CharSequence?) -> Unit = {},
    private val onEdit: () -> Unit = {}
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_attributed_object_description

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is DescriptionBlock<*, *>
                && screenState.viewMode == item.screenState.viewMode
                && screenState.value.description == item.screenState.value.description

    override fun onInit(fragment: Fragment, block: View) {
        block.ivHintDescription.setDebounceClickListener {
            fragment.storeActiveFieldState()
            dialogState.showHint(
                HintDialogData(
                    title = dialogState.string(R.string.common_label_description),
                    description = dialogState.string(R.string.hint_description, dialogState.appConfig.maxLengthDescription())
                )
            )
        }
        initListeners(block)
    }

    override fun onBind(fragment: Fragment, block: View) {
        val editText = block.etClipDescription
        val hint = block.ivHintDescription
        block.tag = this

        editText.withConfig(mainState.getListConfig().textFont, mainState.getListConfig().textSize - 2)

        when (screenState.viewMode) {
            ViewMode.EDIT -> {
                editText.isReadOnly = false
                editText.editableMultiLine(true)
                TextTypeExt.TEXT_PLAIN.apply(editText, description)
                when (screenState.focusMode) {
                    FocusMode.DESCRIPTION -> editText.showKeyboard { editText.restoreLastTouchEvent() }
                    FocusMode.PREVIEW -> editText.restoreLastTouchEvent()
                    else -> editText.clearFocus()
                }
                hint.visible()
            }
            ViewMode.VIEW -> {
                editText.clearFocus()
                editText.isReadOnly = true
                editText.consumeLastTouchEvent()
                editText.editableMultiLine(false)
                TextTypeExt.MARKDOWN.apply(editText, description.notNull())
                hint.gone()
            }
            ViewMode.PREVIEW -> {
                editText.isReadOnly = true
                editText.editableMultiLine(false)
                TextTypeExt.MARKDOWN.apply(editText, description.notNull())
                hint.gone()
            }
        }
    }

    private fun initListeners(block: View) {
        val editText = block.etClipDescription
        val appConfig = dialogState.appConfig
        val context = editText.context
        editText.filters = arrayOf(InputFilter.LengthFilter(appConfig.maxLengthDescription()))
        editText.setOnClickListener(DoubleClickListenerWrapper(
            context,
            { getScreenState(block).acceptDoubleClick() },
            {
                getScreenState(block)
                    ?.whenCanBeEdited(editText)
                    ?.let { getDescriptionBlock(block)?.onEdit?.invoke() }
            }
        ))
        editText.doAfterTextChanged {
            if (getScreenState(block).isEditMode() && it === editText.text) {
                getDescriptionBlock(block)?.onChanged?.invoke(it)
            }
        }
    }

    private fun getDescriptionBlock(block: View): DescriptionBlock<*, *>? {
        val tag = block.tag
        if (tag is DescriptionBlock<*, *>) {
            return tag
        }
        return null
    }

    private fun getScreenState(block: View): AttributedObjectScreenState<*>? {
        return getDescriptionBlock(block)?.screenState
    }

}