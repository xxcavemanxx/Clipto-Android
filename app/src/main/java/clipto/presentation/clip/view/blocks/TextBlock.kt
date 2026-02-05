package clipto.presentation.clip.view.blocks

import android.annotation.SuppressLint
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditTextExt
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import clipto.common.extensions.*
import clipto.config.IAppConfig
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.dynamic.FormField
import clipto.extensions.*
import clipto.presentation.common.fragment.attributed.AttributedObjectFragment
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import clipto.store.clip.ClipScreenState
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R
import kotlin.math.absoluteValue

@SuppressLint("ClickableViewAccessibility")
class TextBlock<F : Fragment>(
    private val appConfig: IAppConfig,
    private val showHidePreview: Boolean,
    private val textHelper: DynamicTextHelper,
    private val screenState: ClipScreenState,
    private val getMinHeight: (fragment: F) -> Int,
    private val getClip: () -> Clip? = { screenState.value },
    private val getState: () -> ClipScreenState? = { screenState },
    private val onEdit: () -> Unit = {},
    private val onTextChanged: (text: CharSequence?) -> Unit = {},
    private val onUpdate: (screenState: ClipScreenState?) -> Unit = {},
    private val onListConfigChanged: (callback: (listConfig: ListConfig) -> Unit) -> Unit = {},
    private val onDynamicFieldClicked: (formField: FormField, editable: Editable?) -> Unit = { _, _ -> },
    private val textType: TextType = screenState.value.textType,
    private val text: String? = screenState.value.text,
    private val newText: String? = screenState.value.newText,
    private val viewMode: ViewMode = screenState.viewMode
) : BlockItem<F>() {

    override val layoutRes: Int = R.layout.block_clip_details_text

    override fun areContentsTheSame(item: BlockItem<F>): Boolean =
        item is TextBlock<*>
                && showHidePreview == item.showHidePreview
                && textType == item.textType
                && viewMode == item.viewMode
                && text == item.text
                && newText == item.newText
                && !screenState.value.isNew()

    private val markdownEditorAcceptor: () -> Boolean = {
        screenState.isEditMode() && (screenState.value.isNew() || screenState.value.textType == TextType.MARKDOWN)
    }

    override fun onInit(fragment: F, block: View) {
        val editText = block as EditTextExt
        editText.minHeight = getMinHeight(fragment)

        initPreview(fragment)
        initListeners(fragment, editText)

        onListConfigChanged { config ->
            editText.withConfig(config.textFont, config.textSize)
            val descriptionView: EditTextExt? = (fragment as? AttributedObjectFragment<*, *, *>)?.getDescriptionView()
            descriptionView?.withConfig(config.textFont, config.textSize - 2)
        }
    }

    override fun onBind(fragment: F, block: View) {
        val editText = block as EditTextExt

        when (screenState.viewMode) {
            ViewMode.EDIT -> {
                editText.editableMultiLine(true)
                val text = screenState.value.getText()
                textHelper.bind(editText, editable = true) { onDynamicFieldClicked(it, editText.text) }
                TextTypeExt.MARKDOWN.applyEditor(editText, markdownEditorAcceptor)
                TextTypeExt.TEXT_PLAIN.apply(editText, text)
                when (screenState.focusMode) {
                    FocusMode.TEXT -> editText.showKeyboard()
                    FocusMode.PREVIEW -> editText.restoreLastTouchEvent()
                    FocusMode.TEXT_AUTO_SCROLL -> editText.showKeyboard { editText.restoreLastTouchEvent() }
                    else -> editText.clearFocus()
                }
                editText.isReadOnly = false
            }
            ViewMode.VIEW -> {
                editText.isReadOnly = true
                editText.clearFocus()
                editText.consumeLastTouchEvent()
                editText.editableMultiLine(false)
                val textTypeExt = screenState.value.textType.toExt()
                textHelper.bind(editText, editable = false) { onDynamicFieldClicked(it, editText.text) }
                textTypeExt.apply(editText, screenState.value.text.notNull())
            }
            ViewMode.PREVIEW -> {
                editText.isReadOnly = true
                editText.editableMultiLine(false)
                val textTypeExt = screenState.value.textType.toExt()
                textHelper.bind(editText, editable = false) { onDynamicFieldClicked(it, editText.text) }
                textTypeExt.apply(editText, screenState.value.text.notNull())
            }
        }

        bindUndoRedo(fragment, editText)
        bindPreview(fragment)
    }

    private fun initPreview(fragment: F) {
        if (fragment is AttributedObjectFragment<*, *, *>) {
            val scrollMultiplier = appConfig.notePreviewModeAutoScrollMultiplier()
            var lastTextY = 0
            val recyclerView = fragment.getBlocksView()
            fragment.getPreviewView().setOnTouchListener { _, event ->
                getState()?.let { state ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            lastTextY = event.rawY.toInt()
                            getClip()?.let { newClip ->
                                onUpdate(
                                    state.copy(
                                        value = newClip,
                                        viewMode = ViewMode.PREVIEW,
                                        focusMode = FocusMode.PREVIEW
                                    )
                                )
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            onUpdate(state.copy(viewMode = ViewMode.EDIT, focusMode = FocusMode.PREVIEW))
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (getState().isPreviewMode()) {
                                val scrollBy = (event.rawY.toInt() - lastTextY) * scrollMultiplier
                                lastTextY = event.rawY.toInt()
                                recyclerView.scrollBy(0, -scrollBy)
                            } else Unit
                        }
                        else -> Unit
                    }
                }
                false
            }
        }
    }

    private fun bindPreview(fragment: F) {
        if (fragment is AttributedObjectFragment<*, *, *>) {
            val previewView = fragment.getPreviewView()
            val clip = screenState.value
            when (screenState.viewMode) {
                ViewMode.EDIT -> {
                    val type = clip.textType.toExt()
                    previewView.setIconResource(type.iconRes)
                    previewView.animateScale(true)
                }
                ViewMode.VIEW -> {
                    previewView.animateScale(false)
                }
                else -> Unit
            }
        }
    }

    private fun bindUndoRedo(fragment: F, editText: EditTextExt) {
        if (fragment is AttributedObjectFragment<*, *, *>) {
            if (screenState.isViewMode()) {
                editText.weakTextWatcherOne.watcher = null
                fragment.getUndoView().animateVisibleInvisible(false)
                fragment.getRedoView().animateVisibleInvisible(false)
                return
            }
            if (screenState.isEditMode()) {
                if (editText.weakTextWatcherOne.watcher is TextUndoRedo) {
                    return
                }

                val undoView = fragment.getUndoView()
                val redoView = fragment.getRedoView()

                val undoRedo = TextUndoRedo(editText, getState) {
                    redoView.animateVisibleInvisible(it.canRedo())
                    undoView.animateVisibleInvisible(it.canUndo())
                }

                undoView.setOnClickListener {
                    undoRedo.takeIf { ref -> ref.canUndo() }?.exeUndo()
                }

                redoView.setOnClickListener {
                    undoRedo.takeIf { ref -> ref.canRedo() }?.exeRedo()
                }
            }
        }
    }

    private fun initListeners(fragment: F, editText: EditTextExt) {
        val context = editText.context

        editText.setOnClickListener(DoubleClickListenerWrapper(
            context,
            { getState().acceptDoubleClick() },
            {
                if (!TextTypeExt.isLinkClickAllowed()) {
                    return@DoubleClickListenerWrapper
                }
                getState()?.whenCanBeEdited(editText)?.let {
                    onEdit()
                }
            }
        ))

        var prevY = 0
        val distance = appConfig.notePreventAccidentClicksDistance()
        editText.setOnTouchListener { _, event ->
            if (!getState().isEditMode() && event != null) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        prevY = event.rawY.toInt()
                    }
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_CANCEL -> {
                        val distanceY = (prevY - event.rawY.toInt()).absoluteValue
                        if (distanceY >= distance) {
                            TextTypeExt.setClicked()
                        }
                    }
                }
            }
            false
        }

        val previewView: MaterialButton? = (fragment as? AttributedObjectFragment<*, *, *>)?.getPreviewView()
        editText.doAfterTextChanged {
            if (getState().isEditMode() && it === editText.text) {
                onTextChanged(it)
                val textLength = it?.length ?: 0
                previewView?.isEnabled = textLength > 0
                previewView?.text = textLength.toString()
            }
        }
    }

    class TextUndoRedo(
        private val editText: EditTextExt,
        private val getActualScreenState: () -> ClipScreenState?,
        private val interceptor: (ref: TextUndoRedo) -> Unit = {}
    ) : TextWatcher {

        private var offset: Record? = null
        private var isUndoOrRedo = false

        init {
            editText.weakTextWatcherOne.watcher = this
            Record(0, 0, null)
            interceptor.invoke(this)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (getActualScreenState().isViewMode()) {
                editText.removeTextChangedListener(this)
                return
            }
            if (isUndoOrRedo) {
                return
            }
            if (editText.isReadOnly) {
                return
            }
            if (getActualScreenState().isPreviewMode()) {
                return
            }
            if (start == 0 && count == 0 && after == 0 && s.isEmpty()) {
                return
            }
            Record(start, start + after, s.subSequence(start, start + count).toString())
            cleanNext()
            noticeTextChang()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable) = Unit

        fun exeUndo() {
            exeUndoOrRedo(true)
        }

        fun exeRedo() {
            exeUndoOrRedo(false)
        }

        fun canUndo(): Boolean {
            return offset?.prior != null
        }

        fun canRedo(): Boolean {
            return offset?.next != null
        }

        private fun noticeTextChang() {
            interceptor.invoke(this)
        }

        private fun cleanNext() {
            while (offset?.next != null) {
                val record = offset?.next
                offset?.next = record?.next
                record?.prior = null
                record?.next = null
            }
        }

        private fun exeUndoOrRedo(Or: Boolean) {
            if (!Or) {
                offset = offset?.next
            }
            offset?.let { offsetRef ->
                isUndoOrRedo = true
                runCatching {
                    val editable = editText.editableText
                    val temp = editable.subSequence(offsetRef.start, offsetRef.end)
                    editable.replace(offsetRef.start, offsetRef.end, offsetRef.text)
                    offsetRef.end = offsetRef.start + (offsetRef.text?.length ?: 0)
                    Selection.setSelection(editable, offsetRef.end)
                    offsetRef.text = temp
                }
                isUndoOrRedo = false
            }
            if (Or) {
                offset = offset?.prior
            }
            noticeTextChang()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TextUndoRedo

            if (editText != other.editText) return false

            return true
        }

        override fun hashCode(): Int {
            return System.identityHashCode(editText)
        }

        private inner class Record constructor(var start: Int, var end: Int, var text: CharSequence?) {

            var prior: Record? = null
            var next: Record? = null

            init {
                offset?.let {
                    it.next = this
                    prior = it
                }
                offset = this
            }
        }
    }

}