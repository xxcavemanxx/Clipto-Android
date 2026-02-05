package clipto.dynamic

import android.content.res.ColorStateList
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.EditTextExt
import android.widget.TextView
import android.widget.TextViewExt
import androidx.core.graphics.ColorUtils
import clipto.common.extensions.*
import clipto.common.presentation.text.MyClickableSpan
import clipto.common.presentation.text.MyCustomSpan
import clipto.common.presentation.text.VerticalImageSpan
import clipto.config.IAppConfig
import clipto.domain.TextType
import clipto.dynamic.fields.ReferenceDynamicValue
import clipto.extensions.*
import clipto.store.app.AppState
import com.google.android.material.chip.ChipDrawable
import com.wb.clipboard.R
import dagger.Lazy
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class DynamicTextHelper @Inject constructor(
    private val appState: AppState,
    private val appConfig: IAppConfig,
    private val repository: Lazy<IDynamicValuesRepository>
) {

    private var disposableMap = mutableMapOf<Int, Disposable>()

    fun toString(text: CharSequence, fields: List<FormField>, config: DynamicValueConfig): CharSequence {
        if (fields.isEmpty()) return text
        val sb = StringBuilder(text.length * 2)
        var lastIndex = 0
        fields.forEach { ff ->
            sb.append(text.substring(lastIndex, ff.startIndex))
            val value =
                when {
                    config.textType == TextType.QRCODE -> ff.getFieldValue().notNull()
                    config.isPreviewMode() && ff.isUserInput() -> ff.getPlaceholder()
                    config.isEditMode() && ff.isSnippet() -> ff.getFieldLabel()
                    else -> ff.getFieldValue().notNull()
                }
            sb.append(value)
            lastIndex = ff.endIndex
        }
        sb.append(text.substring(lastIndex, text.length))
        return sb
    }

    fun bind(editText: EditTextExt, editable: Boolean, onClicked: (field: FormField) -> Unit = {}) {
        unbind(editText)
        editText.weakTextWatcherTwo.watcher = DynamicTextWatcher(editText, editable, emptyList(), onClicked)
    }

    private fun unbind(editText: EditTextExt) {
        editText.weakTextWatcherTwo.watcher = null
        disposableMap.remove(System.identityHashCode(editText)).disposeSilently()
    }

    fun bind(textView: TextViewExt, fields: List<FormField> = emptyList(), editable: Boolean = false, onClicked: (field: FormField) -> Unit = {}): DynamicTextWatcher {
        unbind(textView)
        val watcher = DynamicTextWatcher(textView, editable, fields, onClicked)
        textView.weakTextWatcherTwo.watcher = watcher
        return watcher
    }

    private fun unbind(textView: TextViewExt) {
        textView.weakTextWatcherTwo.watcher = null
        disposableMap.remove(System.identityHashCode(textView)).disposeSilently()
    }

    inner class DynamicTextWatcher(
        private val view: TextView,
        private val editable: Boolean,
        private val initialFields: List<FormField> = emptyList(),
        private val onClicked: (field: FormField) -> Unit
    ) : TextWatcher {

        private var renderCycle: Int = 0
        private var hasCustomSpans = false
        private var hasInvalidSpans = false
        private val viewId = System.identityHashCode(view)

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!s.isNullOrEmpty()) {
                if (count == s.length && before == 0) {
                    hasCustomSpans = DynamicField.isDynamic(s)
                    hasInvalidSpans = false
                } else if (count > 0) {
                    val startIndex = max(0, start - 1)
                    val endIndex = min(s.length, start + count + 1)
                    val diff = s.substring(startIndex, endIndex)
                    hasCustomSpans = DynamicField.canBeDynamic(diff)
                    hasInvalidSpans = false
                    if (!hasCustomSpans && diff.contains('\n') && DynamicField.canBeDynamic(s)) {
                        hasInvalidSpans = true
                        hasCustomSpans = true
                    }
                } else if (before > 0) {
                    val startIndex = max(0, start - before)
                    val endIndex = min(s.length, start + before + 1)
                    val diff = s.substring(startIndex, endIndex)
                    hasInvalidSpans = DynamicField.canBeInvalid(diff)
                    hasCustomSpans = before == 1 && DynamicField.canBeDynamic(s)
                } else {
                    hasCustomSpans = false
                    hasInvalidSpans = false
                }
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if (s.isNullOrEmpty()) {
                return
            }

            if (editable && hasInvalidSpans) {
                s.getSpans(0, s.length, MyCustomSpan::class.java).forEach { span ->
                    val start = s.getSpanStart(span)
                    val end = s.getSpanEnd(span)
                    val placeholder = s.substring(start, end)
                    if (!DynamicField.isStronglyDynamic(placeholder)) {
                        s.removeSpan(span)
                    }
                }
            }

            if (!hasCustomSpans) {
                return
            }

            val renderCycleSnapshot = ++renderCycle
            disposableMap.remove(viewId).disposeSilently()
            val actionType = if (editable) DynamicValueConfig.ActionType.EDIT else DynamicValueConfig.ActionType.PREVIEW
            val disposable = repository.get().getFormFields(s, DynamicValueConfig(actionType = actionType, initialFields = initialFields))
                .let {
                    if (editable && renderCycleSnapshot > 1) {
                        it.delaySubscription(appConfig.getDynamicValueRenderingDelay(), TimeUnit.MILLISECONDS)
                    } else {
                        it
                    }
                }
                .subscribeOn(appState.getBackgroundScheduler())
                .observeOn(appState.getViewScheduler())
                .subscribe({
                    disposableMap.remove(viewId)
                    if (renderCycle == renderCycleSnapshot) {
                        renderFields(s, it)
                    }
                }, { disposableMap.remove(viewId) })
            disposableMap[viewId] = disposable
        }

        private fun renderFields(s: Editable, fields: List<FormField>) {
            runCatching {
                var cursorPositionChanged = false
                var cursorPosition: Int? = null
                if (view is EditTextExt) {
                    cursorPosition = view.getLastTouchEvent()?.position
                }
                var requestLayout = false
                var updated = false
                fields.forEach { field ->
                    val spans = field.getSpans(view, editable, onClicked)
                    spans.forEach { span ->
                        val updateFlags = updateSpan(s, span, field)
                        requestLayout = requestLayout || updateFlags.requestLayout
                        updated = updated || updateFlags.updated
                    }
                    val position = cursorPosition ?: 0
                    if (!cursorPositionChanged && position > field.startIndex && position < field.endIndex) {
                        cursorPosition = field.endIndex
                        cursorPositionChanged = true
                    }
                }
                cursorPosition = cursorPosition?.takeIf { cursorPositionChanged }
                when {
                    updated && requestLayout -> {
                        view.restoreFocus(position = cursorPosition)
                    }
                    updated -> {
                        view.invalidate()
                        if (cursorPositionChanged) {
                            view.restorePosition(cursorPosition)
                        }
                    }
                }
            }
        }

        private fun updateSpan(s: Editable, span: MyCustomSpan, field: FormField): UpdateFlags {
            val startIndex = field.startIndex
            val endIndex = field.endIndex
            var requestLayout = false
            var canUpdate = false
            when (span) {
                is VerticalImageSpan -> {
                    val existingSpan = s.getSpans(startIndex, endIndex, VerticalImageSpan::class.java).firstOrNull()
                    canUpdate =
                        if (existingSpan != null) {
                            val currentChip = existingSpan.drawable as ChipDrawable
                            val newChip = span.drawable as ChipDrawable
                            val update = currentChip.isCloseIconVisible != newChip.isCloseIconVisible
                                    || currentChip.text != newChip.text
                                    || currentChip.chipStrokeColor != newChip.chipStrokeColor
                            if (update) {
                                s.removeSpan(existingSpan)
                                requestLayout = true
                            }
                            update
                        } else {
                            true
                        }
                }
                is EditFieldSpan -> {
                    val existingSpan = s.getSpans(startIndex, endIndex, EditFieldSpan::class.java).firstOrNull()
                    canUpdate =
                        if (existingSpan != null) {
                            val existingField = existingSpan.field
                            val update = startIndex != existingField.startIndex || endIndex != existingField.endIndex
                            if (update) {
                                s.removeSpan(existingSpan)
                            }
                            update
                        } else {
                            true
                        }
                }
                else -> Unit
            }
            if (canUpdate) {
                s.setSpan(span, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            log("REF::: render field :: add span :: {} - {} -> {}", startIndex, endIndex, span)
            return UpdateFlags(
                requestLayout = requestLayout,
                updated = canUpdate
            )
        }
    }

    private data class UpdateFlags(
        val updated: Boolean,
        val requestLayout: Boolean
    )

}

private fun FormField.getSpans(view: TextView, editable: Boolean, onClicked: (field: FormField) -> Unit = {}): Array<MyCustomSpan> {
    val context = view.context
    val chip = ChipDrawable.createFromResource(context, R.xml.chip)
    chip.chipMinHeight = max(chip.chipMinHeight, view.textSize)
    val hasValue = field.hasValue()
    if (editable) {
        chip.text = getFieldLabel()
        if (field.isLegacy()) {
            chip.setCloseIconResource(R.drawable.ic_expand_more)
        } else {
            chip.setCloseIconResource(R.drawable.action_edit)
        }
        chip.isCloseIconVisible = true
    } else {
        chip.text = getFieldValue() ?: getFieldLabel()
    }
    chip.setBounds(0, 0, chip.intrinsicWidth, chip.intrinsicHeight)
    when {
        field.isUnknown() -> {
            val strokeColor = context.getColorNegative()
            val bgColor = ColorUtils.setAlphaComponent(strokeColor, 36)
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        }
        !field.isUserInput() -> {
            val strokeColor = context.getTextColorSecondary()
            val bgColor = ColorUtils.setAlphaComponent(strokeColor, 36)
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        }
        field.isReference() -> {
            field as ReferenceDynamicValue
            val strokeColor =
                when {
                    field.refName.isNullOrEmpty() -> context.getColorNegative()
                    field.intrinsic -> context.getTextColorAccent()
                    else -> context.getTextColorSecondary()
                }
            val bgColor = ColorUtils.setAlphaComponent(strokeColor, 36)
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        }
        field.isUserInput() && view is EditText && !view.isEditable() -> {
            val strokeColor = context.getTextColorSecondary()
            val bgColor = ColorUtils.setAlphaComponent(strokeColor, 36)
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        }
        field.isUserInput() && hasValue -> {
            val strokeColor = context.getColorPositive()
            val bgColor = ColorUtils.setAlphaComponent(strokeColor, 36)
            chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        }
    }
    return arrayOf(VerticalImageSpan(chip), EditFieldSpan(this, onClicked))
}

class EditFieldSpan(
    val field: FormField,
    val onClicked: (field: FormField) -> Unit
) : ClickableSpan(), MyCustomSpan, MyClickableSpan {
    override fun onClick(widget: View) {
        widget.hideKeyboard()
        widget.hapticKeyRelease()
        onClicked.invoke(field)
    }
}