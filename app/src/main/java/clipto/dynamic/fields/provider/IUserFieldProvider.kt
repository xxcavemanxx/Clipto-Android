package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.presentation.common.recyclerview.BlockItem

interface IUserFieldProvider<F : DynamicField> : IFieldProvider<F> {

    fun createFieldInput(field: F, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>>

}