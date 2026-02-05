package clipto.dynamic.fields

import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue
import clipto.extensions.toClip
import clipto.store.clipboard.ClipboardState

class ClipboardDynamicValue(private val state: ClipboardState) : DynamicValue(ID) {

    private var value: String? = null

    override fun getFieldValueUnsafe(): String? {
        if (value == null) {
            value = state.getPrimaryClip()?.toClip(state.app)?.text
        }
        return value
    }

    override fun apply(from: DynamicField) = Unit
    override fun hasValue(): Boolean = true
    override fun clear() = Unit

    companion object {
        const val ID = "clipboard"
    }

}