package clipto.presentation.common.fragment.attributed.blocks

import android.annotation.SuppressLint
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import clipto.common.extensions.*
import clipto.domain.*
import clipto.extensions.TextTypeExt
import clipto.extensions.storeActiveFieldState
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attributed_object_abbreviation.view.*

@SuppressLint("ClickableViewAccessibility")
class AbbreviationBlock<O : AttributedObject, S : AttributedObjectScreenState<O>>(
    private val dialogState: DialogState,
    private val screenState: S,
    private val abbreviation: String? = screenState.value.abbreviation,
    private val onChanged: (abbreviation: CharSequence?) -> Unit = {},
    private val onNextFocus: (focus: FocusMode) -> Unit = {},
    private val onEdit: () -> Unit = {}
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_attributed_object_abbreviation

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is AbbreviationBlock<*, *>
                && screenState.viewMode == item.screenState.viewMode
                && abbreviation == item.abbreviation

    override fun onInit(fragment: Fragment, block: View) {
        val appConfig = dialogState.appConfig
        block.ivHintAbbreviation.setDebounceClickListener {
            fragment.storeActiveFieldState()
            dialogState.showHint(
                HintDialogData(
                    title = dialogState.string(R.string.common_label_abbreviation),
                    description = dialogState.string(
                        R.string.hint_abbreviation,
                        appConfig.maxLengthAbbreviation(),
                        appConfig.getAbbreviationLeadingSymbol()
                    ),
                    descriptionIsMarkdown = true
                )
            )
        }
        initListeners(block)
    }

    override fun onBind(fragment: Fragment, block: View) {
        val editText = block.etClipAbbreviation
        val hint = block.ivHintAbbreviation
        block.tag = this

        when (screenState.viewMode) {
            ViewMode.EDIT -> {
                editText.isReadOnly = false
                editText.editableSingleLine(true)
                TextTypeExt.TEXT_PLAIN.apply(editText, abbreviation)
                when (screenState.focusMode) {
                    FocusMode.ABBREVIATION -> editText.showKeyboard { editText.restoreLastTouchEvent() }
                    FocusMode.PREVIEW -> editText.restoreLastTouchEvent()
                    else -> editText.clearFocus()
                }
                hint.visible()
            }
            ViewMode.VIEW -> {
                editText.clearFocus()
                editText.isReadOnly = true
                editText.consumeLastTouchEvent()
                editText.editableSingleLine(false)
                TextTypeExt.TEXT_PLAIN.apply(editText, abbreviation)
                hint.gone()
            }
            ViewMode.PREVIEW -> {
                editText.isReadOnly = true
                editText.editableSingleLine(false)
                TextTypeExt.TEXT_PLAIN.apply(editText, abbreviation)
                hint.gone()
            }
        }
    }

    private fun initListeners(block: View) {
        val editText = block.etClipAbbreviation
        val context = editText.context
        val appConfig = dialogState.appConfig
        editText.filters = arrayOf(InputFilter.LengthFilter(appConfig.maxLengthAbbreviation()))
        editText.setOnClickListener(DoubleClickListenerWrapper(
            context,
            { getScreenState(block).acceptDoubleClick() },
            {
                getScreenState(block)
                    ?.whenCanBeEdited(editText)
                    ?.let { getAbbreviationBlock(block)?.onEdit?.invoke() }
            }
        ))
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                getAbbreviationBlock(block)?.onNextFocus?.invoke(FocusMode.DESCRIPTION)
                true
            } else {
                false
            }
        }
        editText.doAfterTextChanged {
            if (getScreenState(block).isEditMode() && it === editText.text) {
                getAbbreviationBlock(block)?.onChanged?.invoke(it)
            }
        }
    }

    private fun getAbbreviationBlock(block: View): AbbreviationBlock<*, *>? {
        val tag = block.tag
        if (tag is AbbreviationBlock<*, *>) {
            return tag
        }
        return null
    }

    private fun getScreenState(block: View): AttributedObjectScreenState<*>? {
        return getAbbreviationBlock(block)?.screenState
    }

}