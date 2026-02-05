package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.fields.LegacyValueDynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderEditBlock
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.blocks.HeaderViewBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class LegacyFieldProvider(
        val context: DynamicValueContext,
        val field: LegacyValueDynamicField
) : IFieldProvider<LegacyValueDynamicField> {

    override fun getId(): String = field.id
    override fun createPlaceholder(field: LegacyValueDynamicField): String = field.getFieldPlaceholder()

    override fun getTitleRes(): Int = field.dynamicType.titleRes
    override fun getDescriptionRes(): Int = throw UnsupportedOperationException("not supported")
    override fun createField(params: Map<String, Any?>): LegacyValueDynamicField = throw UnsupportedOperationException("not supported")
    override fun createField(placeholder: String, params: Map<String, Any?>): LegacyValueDynamicField = throw UnsupportedOperationException("not supported")

    override fun createFieldConfig(field: LegacyValueDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel): List<BlockItem<Fragment>> {
        val blocks = mutableListOf<BlockItem<Fragment>>()

        when (viewMode) {
            ViewMode.EDIT -> {
                blocks.add(HeaderEditBlock(titleRes = getTitleRes(), viewModel = viewModel))
            }
            ViewMode.FILL -> {
                // header
                blocks.add(
                        HeaderFillBlock(
                                titleRes = getTitleRes(),
                                title = field.getFieldLabel(),
                                actionTitleRes = R.string.button_refresh,
                                actionActive = field.hasValue(),
                                onAction = {
                                    field.clear()
                                    viewModel.onRefresh()
                                }
                        )
                )
            }
            else -> {
                blocks.add(HeaderViewBlock(titleRes = getTitleRes(), viewModel = viewModel))
            }
        }
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))

        if (viewMode != ViewMode.FILL) {
            // placeholder
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(
                    TextInputLayoutBlock(
                            hint = viewModel.string(R.string.dynamic_field_attr_common_placeholder),
                            text = createPlaceholder(field),
                            enabled = false
                    )
            )
        }

        // value
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
                TextInputLayoutBlock(
                        hint = viewModel.string(R.string.dynamic_field_attr_common_value),
                        text = field.getFieldValue(),
                        enabled = false,
                )
        )

        blocks.add(SpaceBlock(heightInDp = 16))

        return blocks
    }

}