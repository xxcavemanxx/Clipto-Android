package clipto.presentation.common.widget

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.toNullIfEmpty
import com.wb.clipboard.R

class AutoCompleteTextView : AppCompatAutoCompleteTextView {

    private var currentItems: List<String> = emptyList()
    private var selectedItemsProvider: () -> List<String> = { emptyList() }
    private var allItemsProvider: () -> List<AutoCompleteItem> = { emptyList() }
    private val adapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, mutableListOf())

    init {
        setDropDownBackgroundResource(R.drawable.bg_popup_menu)
        adapter.setNotifyOnChange(false)
        setAdapter(adapter)
        setOnClickListener { showResults() }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun enoughToFilter(): Boolean = true

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            performFiltering(text, 0)
            if (windowToken != null) {
                runCatching { super.showDropDown() }
            }
        } else {
            if (windowToken != null) {
                runCatching { super.dismissDropDown() }
            }
        }
    }

    override fun performFiltering(text: CharSequence?, keyCode: Int) {
        updateItems()
        super.performFiltering(text, keyCode)
    }

    fun withSelectedItemsProvider(provider: () -> List<String>): AutoCompleteTextView {
        selectedItemsProvider = provider
        return this
    }

    fun withAllItemsProvider(provider: () -> List<AutoCompleteItem>): AutoCompleteTextView {
        allItemsProvider = provider
        return this
    }

    fun withInputMaxLength(length: Int): AutoCompleteTextView {
        filters = arrayOf(InputFilter.LengthFilter(length))
        return this
    }

    fun withOnEnterListener(showResults: Boolean = true, callback: (text: String?) -> Unit): AutoCompleteTextView {
        setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val value = text.toNullIfEmpty()
                text = null
                callback.invoke(value)
                if (!text.isNullOrBlank() && showResults) {
                    showResults()
                }
                true
            } else {
                false
            }
        }
        return this
    }

    fun withOnItemClickListener(callback: (item: String) -> Unit): AutoCompleteTextView {
        setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            text = null
            callback.invoke(selected)
        }
        return this
    }

    fun withOnTextChangeListener(callback: (text: Editable?) -> Unit): AutoCompleteTextView {
        doAfterTextChanged(callback)
        return this
    }

    fun showResults() {
        if (!hasFocus()) {
            requestFocus()
        } else {
            performFiltering(text, 0)
            super.showDropDown()
        }
    }

    private fun updateItems() {
        val filtered = getAvailable()
        if (filtered != currentItems) {
            currentItems = filtered
            adapter.clear()
            adapter.addAll(filtered)
        }
    }

    override fun dismissDropDown() {
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing && event?.action == KeyEvent.ACTION_UP) {
            super.dismissDropDown()
            return true
        }
        return super.onKeyPreIme(keyCode, event)
    }

    private fun getAvailable(): List<String> {
        val existing: List<String> = selectedItemsProvider.invoke()
        return allItemsProvider.invoke()
            .filter { !existing.contains(it.first) }
            .mapNotNull { it.second }
            .distinct()
    }

}

typealias AutoCompleteItem = Pair<String?, String?>