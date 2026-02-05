package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.common.recyclerview.BlockItem

interface IFieldProvider<F : DynamicField> {

    fun getId(): String

    fun getTitleRes(): Int

    fun getDescriptionRes(): Int

    fun createPlaceholder(field: F): String

    fun createField(params: Map<String, Any?> = emptyMap()): F

    fun createField(placeholder: String, params: Map<String, Any?>): F

    fun createFieldConfig(field: F, viewMode: ViewMode, viewModel: DynamicFieldViewModel): List<BlockItem<Fragment>>

}