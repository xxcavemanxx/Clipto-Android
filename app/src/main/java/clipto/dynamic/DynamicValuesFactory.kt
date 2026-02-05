package clipto.dynamic

import android.app.Application
import clipto.analytics.Analytics
import clipto.common.misc.GsonUtils
import clipto.dynamic.fields.LegacyValueDynamicField
import clipto.dynamic.fields.provider.*
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("UNCHECKED_CAST")
@Singleton
class DynamicValuesFactory @Inject constructor(
    val app: Application,
    val context: DynamicValueContext,
    textFieldProvider: TextFieldProvider,
    numberFieldProvider: NumberFieldProvider,
    selectFieldProvider: SelectFieldProvider,
    dateTimeFieldProvider: DateTimeFieldProvider,
    dateTimeValueProvider: DateTimeValueProvider,
    deviceInfoValueProvider: DeviceInfoValueProvider,
    clipboardValueProvider: ClipboardValueProvider,
    snippetValueProvider: SnippetValueProvider,
    randomValueProvider: RandomValueProvider,
    barcodeFieldProvider: BarcodeFieldProvider,
    textToggleFieldProvider: TextToggleFieldProvider,
    referenceFieldProvider: ReferenceFieldProvider
) {

    private val fields = listOf(
        textFieldProvider,
        numberFieldProvider,
        selectFieldProvider,
        dateTimeFieldProvider,
        barcodeFieldProvider,
        textToggleFieldProvider,
        referenceFieldProvider
    )
    private val values = listOf(
        dateTimeValueProvider,
        deviceInfoValueProvider,
        clipboardValueProvider,
        randomValueProvider
    )

    private val providers = fields.plus(values).plus(snippetValueProvider)

    fun getFieldProvider(field: DynamicField): IFieldProvider<DynamicField> {
        if (field is LegacyValueDynamicField) {
            return LegacyFieldProvider(context, field) as IFieldProvider<DynamicField>
        }
        if (field.isUnknown()) {
            return UnknownFieldProvider(context) as IFieldProvider<DynamicField>
        }
        return providers.first { it.getId() == field.id } as IFieldProvider<DynamicField>
    }

    fun getDynamicFieldProviders(): List<IFieldProvider<DynamicField>> = fields as List<IFieldProvider<DynamicField>>

    fun getDynamicValueProviders(): List<IFieldProvider<DynamicField>> = values as List<IFieldProvider<DynamicField>>

    fun getDynamicField(placeholder: String, config: DynamicValueConfig): DynamicField? {
        if (placeholder.contains('\n')) return null
        val json = placeholder.substring(1, placeholder.length - 1)

        // dynamic fields
        var params = runCatching { GsonUtils.get().fromJson<Map<String, Any>>(json, TYPE_MAP.type) }.getOrNull() ?: emptyMap()
        val id = DynamicField.getId(params)
        val dynamicFieldProvider = providers.find { it.getId() == id }
        if (dynamicFieldProvider != null) {
            params = params.plus(DynamicField.ATTR_LEVEL to config.level)
            return runCatching { dynamicFieldProvider.createField(placeholder, params) }
                .onFailure { Analytics.onError("getDynamicField", it) }
                .getOrNull()
        }
        // unknown field
        if (!id.isNullOrEmpty() && params.isNotEmpty()) {
            return runCatching { UnknownFieldProvider(context).createField(placeholder, params) }
                .onFailure { Analytics.onError("getDynamicField#unknown", it) }
                .getOrNull()
        }

        // legacy values
        val legacyId = DynamicField.getId(placeholder)
        val legacyType = getLegacyValueType(legacyId)
        if (legacyType != null) {
            val legacyValue = legacyType.getDynamicValue(legacyId, config.level)
            return LegacyValueDynamicField(context, legacyType, legacyValue, legacyId).apply {
                this.value = if (config.isEditMode()) null else legacyValue.getValue(context).toString()
                this.defaultLabel = app.getString(legacyType.titleRes)
                this.placeholder = placeholder
            }
        }

        return null
    }

    private fun getLegacyValueType(id: String): DynamicValueType? {
        val commonValues = DynamicValueType.commonValues
        return when {
            commonValues.containsKey(id) -> commonValues.getValue(id)
            DynamicValueType.SNIPPET.isValid(id) -> DynamicValueType.SNIPPET
            else -> null
        }
    }

    companion object {
        private val TYPE_MAP = object : TypeToken<Map<String, Any>>() {}
    }

}