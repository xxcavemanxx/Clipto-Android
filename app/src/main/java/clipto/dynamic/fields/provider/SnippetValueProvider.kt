package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.fields.SnippetDynamicValue
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetValueProvider @Inject constructor(
        private val context: DynamicValueContext
) : AbstractFieldProvider<SnippetDynamicValue>() {

    override fun getId(): String = SnippetDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_value_snippet
    override fun getDescriptionRes(): Int = R.string.dynamic_value_snippet_description

    override fun newField(params: Map<String, Any?>): SnippetDynamicValue = SnippetDynamicValue(
            context = context,
            level = (params[DynamicField.ATTR_LEVEL] as? Int) ?: 0,
            ref = params[DynamicField.ATTR_SNIPPET_REF].toString()
    ).apply {
        value = params[DynamicField.ATTR_VALUE]?.toString()
        label = params[DynamicField.ATTR_LABEL]?.toString()
    }

    override fun fillMap(field: SnippetDynamicValue, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_SNIPPET_REF] = field.snippetId
    }

    override fun bindConfig(field: SnippetDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        val editMode = viewMode != ViewMode.VIEW && viewMode != ViewMode.FILL

        if(!editMode) return

        // value
        if (field.hasValue()) {
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(
                    TextInputLayoutBlock(
                            hint = viewModel.string(R.string.dynamic_field_attr_common_value),
                            text = field.getFieldValue(),
                            enabled = false,
                            maxLines = 1
                    )
            )
        }
        blocks.add(SpaceBlock(heightInDp = 16))
    }

}