package clipto.presentation.common.fragment.attributed.blocks

import android.annotation.SuppressLint
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import clipto.AppContext
import clipto.common.extensions.*
import clipto.domain.*
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attributed_object_title.view.*

@SuppressLint("ClickableViewAccessibility")
class TitleBlock<O : AttributedObject, S : AttributedObjectScreenState<O>>(
    private val showAdditionalAttributes: Boolean,
    private val screenState: S,
    private val hintRes: Int,
    private val hasExtraAttrs: Boolean = !screenState.value.description.isNullOrBlank() || !screenState.value.abbreviation.isNullOrBlank(),
    private val onChanged: (title: CharSequence?) -> Unit = {},
    private val onNextFocus: (focus: FocusMode) -> Unit = {},
    private val onShowAttrs: (show: Boolean) -> Unit = {},
    private val onEdit: () -> Unit = {},
    private val isNew: Boolean = false
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_attributed_object_title

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is TitleBlock<*, *> &&
                screenState.viewMode == item.screenState.viewMode &&
                screenState.value.title == item.screenState.value.title &&
                showAdditionalAttributes == item.showAdditionalAttributes &&
                hasExtraAttrs == item.hasExtraAttrs &&
                hintRes == item.hintRes &&
                !isNew

    override fun onInit(fragment: Fragment, block: View) {
        block.ivShowMore.setDebounceClickListener {
            getTitleBlock(block)?.let { ref -> ref.onShowAttrs(!ref.showAdditionalAttributes) }
        }
        initListeners(block)
    }

    override fun onBind(fragment: Fragment, block: View) {
        block.tag = this

        val editText = block.etClipTitle
        editText.setHint(hintRes)

        when (screenState.viewMode) {
            ViewMode.EDIT -> {
                editText.isReadOnly = false
                editText.editableSingleLine(true)
                editText.setText(screenState.value.title)
                when (screenState.focusMode) {
                    FocusMode.TITLE -> editText.showKeyboard { editText.restoreLastTouchEvent() }
                    FocusMode.PREVIEW -> editText.restoreLastTouchEvent()
                    else -> editText.clearFocus()
                }

            }
            ViewMode.VIEW -> {
                editText.clearFocus()
                editText.isReadOnly = true
                editText.consumeLastTouchEvent()
                editText.editableSingleLine(false)
                editText.setText(screenState.value.title)
            }
            ViewMode.PREVIEW -> {
                editText.isReadOnly = true
                editText.editableSingleLine(false)
                editText.setText(screenState.value.title)
            }
        }

        if (showAdditionalAttributes) {
            block.ivShowMore.setImageResource(R.drawable.ic_expand_less)
            block.highlightView.gone()
        } else {
            block.ivShowMore.setImageResource(R.drawable.ic_expand_more)
            block.highlightView.setVisibleOrGone(hasExtraAttrs)
        }
    }

    private fun initListeners(block: View) {
        val editText = block.etClipTitle
        val context = editText.context
        val appConfig = AppContext.get().appConfig
        editText.filters = arrayOf(InputFilter.LengthFilter(appConfig.maxLengthTitle()))
        editText.setOnClickListener(DoubleClickListenerWrapper(
            context,
            { getScreenState(block).acceptDoubleClick() },
            {
                getScreenState(block)
                    ?.whenCanBeEdited(editText)
                    ?.let { getTitleBlock(block)?.onEdit?.invoke() }
            }
        ))
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                getTitleBlock(block)?.onNextFocus?.invoke(FocusMode.TAGS)
                true
            } else {
                false
            }
        }
        editText.doAfterTextChanged {
            if (getScreenState(block).isEditMode() && it === editText.text) {
                getTitleBlock(block)?.onChanged?.invoke(it)
            }
        }
    }

    private fun getTitleBlock(block: View): TitleBlock<*, *>? {
        val tag = block.tag
        if (tag is TitleBlock<*, *>) {
            return tag
        }
        return null
    }

    private fun getScreenState(block: View): AttributedObjectScreenState<*>? {
        return getTitleBlock(block)?.screenState
    }

}