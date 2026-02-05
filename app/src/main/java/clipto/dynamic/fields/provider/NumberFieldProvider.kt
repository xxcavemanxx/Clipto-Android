package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.NumberDynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.RequestFocusBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberFieldProvider @Inject constructor() : AbstractUserFieldProvider<NumberDynamicField>() {

    override fun getId(): String = NumberDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_number
    override fun getDescriptionRes(): Int = R.string.dynamic_field_number_description

    override fun newField(params: Map<String, Any?>): NumberDynamicField {
        return NumberDynamicField().apply {
            (params[DynamicField.ATTR_MIN_VALUE] as? Number)?.let { minValue = it.toInt() }
            (params[DynamicField.ATTR_MAX_VALUE] as? Number)?.let { maxValue = it.toInt() }
            (params[DynamicField.ATTR_VALUE] as? Number)?.let { value = it.toInt() }
        }
    }

    override fun fillMap(field: NumberDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_MIN_VALUE] = field.minValue
        params[DynamicField.ATTR_MAX_VALUE] = field.maxValue
        params[DynamicField.ATTR_VALUE] = field.value
    }

    override fun createFieldInput(field: NumberDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return listOf(
                TextInputLayoutBlock(
                        text = field.getFieldValue(),
                        inputType = TextInputLayoutBlock.TextInputType.NUMBER_SIGNED,
                        hint = "${field.getFieldLabel()} ${field.getRangeLabel()}",
                        onTextChanged = {
                            val value = it?.toString()?.toIntOrNull()
                            val min = field.minValue
                            val max = field.maxValue
                            when {
                                value == null -> {
                                    field.value = null
                                    null
                                }
                                min != null && value < min -> {
                                    "$value < $min"
                                }
                                max != null && value > max -> {
                                    "$value > $max"
                                }
                                else -> {
                                    field.value = value
                                    null
                                }
                            }
                        }
                )
        )
    }

    override fun bindConfig(field: NumberDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        when (viewMode) {
            ViewMode.FILL -> {
                blocks.add(
                        HeaderFillBlock(
                                titleRes = getTitleRes(),
                                title = field.getFieldLabel(),
                                actionActive = field.hasValue(),
                                actionTitleRes = R.string.menu_clear,
                                onAction = {
                                    field.clear()
                                    viewModel.onComplete()
                                }
                        )
                )
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.addAll(createFieldInput(field, viewModel.dynamicContext) { viewModel.onRefresh() })
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(RequestFocusBlock(R.id.textInputEditText))
            }
            else -> {
                val editMode = viewMode != ViewMode.VIEW

                // min-max value
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                        NumberRangeBlock(
                                enabled = editMode,
                                minValue = field.minValue,
                                maxValue = field.maxValue,
                                onRangeChanged = { min, max ->
                                    field.minValue = min
                                    field.maxValue = max
                                    viewModel.onRefresh(withDelay = true)
                                }
                        )
                )

                // default value
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                    TextInputLayoutBlock(
                        text = field.getFieldValue(),
                        hint = viewModel.string(R.string.dynamic_field_attr_common_default_value),
                        inputType = TextInputLayoutBlock.TextInputType.NUMBER_SIGNED,
                        enabled = editMode,
                        onTextChanged = {
                            field.value = it?.toString()?.toIntOrNull()
                            viewModel.onRefresh(withDelay = true)
                            null
                        }
                    )
                )

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}