package clipto.dynamic

import clipto.common.extensions.disposeSilently
import clipto.common.extensions.notNull
import clipto.domain.Clip
import clipto.domain.ObjectType
import clipto.dynamic.fields.ReferenceDynamicValue
import clipto.dynamic.fields.SnippetDynamicValue
import clipto.dynamic.presentation.text.DynamicTextState
import clipto.store.clip.ClipState
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicValuesRepository @Inject constructor(
    private val clipState: ClipState,
    private val state: DynamicTextState,
    private val factory: DynamicValuesFactory,
    private val helper: Lazy<DynamicTextHelper>
) : IDynamicValuesRepository {

    private var lastDisposable: Disposable? = null

    override fun getFieldsCount(clip: Clip): Single<Int> {
        val text = clip.text.notNull()
        val config = DynamicValueConfig(clip = clip, actionType = DynamicValueConfig.ActionType.PREVIEW)
        return getFormFields(text, config).map { it.size }
    }

    override fun getFormFields(text: CharSequence, config: DynamicValueConfig): Single<List<FormField>> = Single
        .fromCallable {
            if (config.actionType == DynamicValueConfig.ActionType.RECURSIVE) {
                return@fromCallable emptyList()
            }

            var idx = 0
            val fields = mutableListOf<FormField>()
            val initialFields = config.initialFields

            var prevPos: Pair<Int, String>? = null
            var nextPos = text.findAnyOf(DynamicField.PLACEHOLDERS)
            while (nextPos != null) {
                when {
                    prevPos?.second == DynamicField.PLACEHOLDER_OPEN && nextPos.second == DynamicField.PLACEHOLDER_CLOSE -> {
                        val startIndex = prevPos.first
                        val endIndex = nextPos.first + 2
                        val placeholder = text.substring(startIndex, endIndex)
                        val field = factory.getDynamicField(placeholder, config)
                        if (field != null) {
                            fields.add(
                                FormField(
                                    field = field,
                                    startIndex = startIndex,
                                    endIndex = endIndex,
                                    index = idx++
                                )
                            )
                            prevPos = nextPos
                        }
                    }
                    else -> {
                        prevPos = nextPos
                    }
                }
                nextPos = text.findAnyOf(DynamicField.PLACEHOLDERS, nextPos.first + 2)
            }

            fields.forEach { formField ->
                if (formField.isReference()) {
                    val field = formField.field as ReferenceDynamicValue
                    if (!field.refName.isNullOrEmpty()) {
                        val fieldRef = fields.find { !it.isReference() && it.field.label == field.refName }?.field
                        if (!config.isEditMode() && field.intrinsic && fieldRef != null) {
                            val newField = fieldRef.clone() as DynamicField
                            newField.placeholder = field.placeholder
                            newField.required = field.required
                            newField.prefix = field.prefix
                            newField.suffix = field.suffix
                            newField.label = field.label ?: fieldRef.label
                            val newFormField = formField.copy(field = newField)
                            fields[formField.index] = newFormField
                        } else {
                            field.ref = fieldRef
                        }
                    }
                }
            }

            val fieldsAfter = fields.filter { it.isUserInput() }
            val fieldsBefore = initialFields.filter { it.isUserInput() }
            if (fieldsBefore.isNotEmpty() && fieldsBefore.size == fieldsAfter.size) {
                fieldsAfter.forEachIndexed { index, field ->
                    val prevField = fieldsBefore[index]
                    field.apply(prevField)
                }
            }

            val snippetsAfter = fields.filter { it.field.isSnippet() }.map { it.field as SnippetDynamicValue }
            if (!config.isEditMode() && snippetsAfter.isNotEmpty()) {
                snippetsAfter.forEach { it.value = it.getFieldValue() }
            }

            clipState.formFields.setValue(fields)

            fields
        }

    override fun process(text: CharSequence, config: DynamicValueConfig): Single<CharSequence> {
        if (config.clip?.objectType == ObjectType.INTERNAL_GENERATED) {
            return Single.just(text)
        }
        return getFormFields(text, config)
            .flatMap { fields ->
                if (config.actionType.skipInput || fields.isEmpty() || !fields.any { it.isUserInput() || it.isSnippet() }) {
                    Single.just(helper.get().toString(text, fields, config))
                } else {
                    lastDisposable.disposeSilently()

                    val dynamicValues = fields.filter { !it.isUserInput() }
                    if (dynamicValues.isEmpty()) {
                        state.requestManualInput(text, config, fields).toSingle()
                            .map { helper.get().toString(it.text, it.fields, config) }
                            .doOnSubscribe {
                                lastDisposable = it.takeIf { config.level == 0 }
                            }
                    } else {
                        val newText = helper.get().toString(text, dynamicValues, config)
                        getFormFields(newText, config)
                            .flatMap { newFields ->
                                state.requestManualInput(newText, config, newFields).toSingle()
                                    .map { helper.get().toString(it.text, it.fields, config) }
                                    .doOnSubscribe {
                                        lastDisposable = it.takeIf { config.level == 0 }
                                    }
                            }
                    }
                }
            }
    }

}