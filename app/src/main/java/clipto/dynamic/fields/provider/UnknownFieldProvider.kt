package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.fields.UnknownDynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderViewBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class UnknownFieldProvider(val context: DynamicValueContext) : IFieldProvider<UnknownDynamicField> {

    override fun getId(): String = UnknownDynamicField.ID
    override fun createPlaceholder(field: UnknownDynamicField): String = field.getFieldPlaceholder()
    override fun getTitleRes(): Int = R.string.dynamic_field_unknown
    override fun getDescriptionRes(): Int = R.string.dynamic_field_unknown_description
    override fun createField(params: Map<String, Any?>): UnknownDynamicField = throw UnsupportedOperationException("not supported")
    override fun createField(placeholder: String, params: Map<String, Any?>): UnknownDynamicField {
        return UnknownDynamicField(params[DynamicField.ATTR_ID]?.toString() ?: placeholder).apply {
            this.defaultLabel = context.app.getString(getTitleRes())
            (params[DynamicField.ATTR_LABEL] as? String)?.let { label = it }
            this.placeholder = placeholder
        }
    }

    override fun createFieldConfig(field: UnknownDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel): List<BlockItem<Fragment>> {
        val blocks = mutableListOf<BlockItem<Fragment>>()
        // header
        blocks.add(HeaderViewBlock(titleRes = getTitleRes(), viewModel = viewModel))
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
        // placeholder
        withPlaceholderBlock(field, viewModel, blocks)
        // label
        withLabelBlock(field, viewModel, blocks)
        // error
        blocks.add(
            WarningBlock(
                titleRes = getDescriptionRes(),
                actionIcon = 0
            )
        )
        blocks.add(SpaceBlock(heightInDp = 16))
        return blocks
    }

    private fun withPlaceholderBlock(field: UnknownDynamicField, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            TextInputLayoutBlock(
                text = createPlaceholder(field),
                hint = viewModel.string(R.string.dynamic_field_attr_common_placeholder),
                maxLines = 1,
                enabled = false
            )
        )
    }

    private fun withLabelBlock(field: UnknownDynamicField, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            TextInputLayoutBlock(
                text = field.label,
                hint = viewModel.string(R.string.dynamic_field_attr_common_label),
                enabled = false
            )
        )
    }

}