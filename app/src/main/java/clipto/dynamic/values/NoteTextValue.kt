package clipto.dynamic.values

import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType

class NoteTextValue(val id: String, val level: Int) : AbstractDynamicValue(id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val snippetId = DynamicValueType.SNIPPET.getValue(id)
        val note = context.clipBoxDao.getClipBySnippetId(snippetId)
        val text = note?.text ?: getKey()
        return if (level <= context.appConfig.deepReplaceLevel()) {
            val actionType = DynamicValueConfig.ActionType.RECURSIVE
            val repository = context.dynamicValuesRepository.get()
            repository.process(text = text, DynamicValueConfig(level = level + 1, actionType = actionType)).blockingGet()
        } else {
            text
        }
    }

}