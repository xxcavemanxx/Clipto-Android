package clipto.dynamic.fields

import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.DynamicValueContext

class SnippetDynamicValue(
    private val context: DynamicValueContext,
    private val level: Int,
    ref: String
) : DynamicValue(ID) {

    var snippetId: String = ref

    var value: String? = null

    override fun getFieldValueUnsafe(): String? {
        if (value == null) {
            runCatching {
                val note = context.clipBoxDao.getClipBySnippetId(snippetId)
                val text = note?.text
                value =
                    if (text != null && level <= context.appConfig.deepReplaceLevel()) {
                        val actionType = DynamicValueConfig.ActionType.RECURSIVE
                        val repository = context.dynamicValuesRepository.get()
                        repository.process(text = text, DynamicValueConfig(level = level + 1, actionType = actionType))
                            .blockingGet()
                            .toString()
                    } else {
                        text
                    }
            }
        }
        return value
    }

    override fun apply(from: DynamicField) {
        if (from is SnippetDynamicValue) {
            value = from.value
        }
    }

    override fun hasValue(): Boolean = value != null

    override fun clear() {
        value = null
    }

    companion object {
        const val ID = "snippet"
    }

}