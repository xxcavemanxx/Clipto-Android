package clipto.common.presentation.text

import android.text.InputFilter
import android.text.Spanned

class InputFilterExcludeSymbols(private val excludes: Set<String>) : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val builder = StringBuilder(dest)
        builder.replace(dstart, dend, source.subSequence(start, end).toString())
        val invalid = excludes.any { builder.contains(it) }
        return if (invalid) {
            ""
        } else {
            source
        }
    }
}