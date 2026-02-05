package android.widget

import android.text.Editable
import android.text.TextWatcher

class WeakTextWatcher(var watcher: TextWatcher? = null) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        watcher?.beforeTextChanged(s, start, count, after)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        watcher?.onTextChanged(s, start, before, count)
    }

    override fun afterTextChanged(s: Editable?) {
        watcher?.afterTextChanged(s)
    }

}