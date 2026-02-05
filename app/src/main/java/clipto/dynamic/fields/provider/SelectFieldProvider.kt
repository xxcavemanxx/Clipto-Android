package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.common.extensions.notNull
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.SelectDynamicField
import clipto.dynamic.models.ValueFormatterType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.select.options.SelectOptionsDialogRequest
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectFieldProvider @Inject constructor() : AbstractUserFieldProvider<SelectDynamicField>() {

    override fun getId(): String = SelectDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_select
    override fun getDescriptionRes(): Int = R.string.dynamic_field_select_description

    override fun newField(params: Map<String, Any?>): SelectDynamicField {
        return SelectDynamicField().apply {
            (params[DynamicField.ATTR_MULTIPLE] as? Boolean)?.let { multiple = it }
            (params[DynamicField.ATTR_FORMATTER] as? String)?.let { formatter = it }
            (params[DynamicField.ATTR_USER_INPUT] as? Boolean)?.let { userInput = it }
            (params[DynamicField.ATTR_OPTIONS] as? List<*>)?.let {
                options = it
                        .mapNotNull { option ->
                            if (option is Map<*, *>) {
                                option
                            } else {
                                null
                            }
                        }
                        .map { map ->
                            SelectDynamicField.Option(
                                    title = map[DynamicField.ATTR_OPTION_LABEL]?.toString(),
                                    value = map[DynamicField.ATTR_OPTION_VALUE]?.toString()
                            )
                        }
            }
        }
    }

    override fun fillMap(field: SelectDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_MULTIPLE] = field.multiple
        params[DynamicField.ATTR_FORMATTER] = field.formatter.takeIf { field.multiple }
        params[DynamicField.ATTR_USER_INPUT] = field.userInput
        params[DynamicField.ATTR_OPTIONS] = field.options.takeIf { it.isNotEmpty() }
    }

    override fun createFieldInput(field: SelectDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return listOf(
                SeparateScreenBlock(
                        title = field.getFieldLabel(),
                        value = field.getFieldValue(),
                        clickListener = {
                            val request = SelectValueDialogRequest(
                                    withManualInput = field.userInput,
                                    title = field.getFieldLabel(),
                                    withImmediateNotify = true,
                                    single = !field.multiple,
                                    options = field.options
                                            .map {
                                                SelectValueDialogRequest.Option(
                                                        model = it,
                                                        checked = field.values.contains(it),
                                                        title = it.getLabel(context.app)
                                                )
                                            },
                                    onSelected = {
                                        field.values = it
                                        onRefreshRequest.invoke()
                                    }
                            )
                            context.dialogState.requestSelectValueDialog(request)
                        }
                )
        )
    }

    override fun bindConfig(field: SelectDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        when (viewMode) {
            ViewMode.FILL -> {
                blocks.add(
                        HeaderFillBlock(
                                titleRes = getTitleRes(),
                                title = field.getFieldLabel(),
                                actionActive = field.hasValue(),
                                actionTitleRes = if (field.multiple) R.string.menu_clear_all else R.string.menu_clear,
                                onAction = {
                                    field.clear()
                                    viewModel.onComplete()
                                }
                        )
                )
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                val options = field.options
                if (options.isNotEmpty()) {
                    val onClicked: (option: SelectDynamicField.Option) -> Unit = {
                        if (field.multiple) {
                            val values = field.values
                            if (values.contains(it)) {
                                field.values = values.minus(it)
                            } else {
                                field.values = values.plus(it)
                            }
                            viewModel.onRefresh(withDelay = false)
                        } else {
                            field.values = listOf(it)
                            viewModel.onComplete()
                        }
                    }
                    options.forEach { option ->
                        blocks.add(SelectValueBlock(
                                model = option,
                                title = option.getLabel(viewModel.app),
                                checked = field.values.contains(option),
                                onClicked = onClicked
                        ))
                    }
                }
            }
            else -> {
                val editMode = viewMode != ViewMode.VIEW

                // multiple
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                        SwitchBlock(
                                titleRes = R.string.dynamic_field_attr_select_multiple,
                                checked = field.multiple,
                                enabled = editMode,
                                clickListener = { _, checked ->
                                    field.multiple = checked
                                    viewModel.onRefresh(withDelay = false)
                                }
                        )
                )

                // separator
                if (field.multiple) {
                    val layoutResourceRes = if (editMode) R.layout.block_dynamic_field_with_more_action else R.layout.block_text_input_layout
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(
                            TextInputLayoutBlock(
                                    hint = viewModel.string(R.string.dynamic_field_attr_common_values_formatter),
                                    maxLines = Integer.MAX_VALUE,
                                    text = field.formatter,
                                    enabled = editMode,
                                    onTextChanged = {
                                        field.formatter = it?.toString().notNull()
                                        viewModel.onRefresh(withDelay = true)
                                        null
                                    },
                                    endIconClickListener = {
                                        val options = mutableListOf<SelectValueDialogRequest.Option<String>>()
                                        ValueFormatterType.values().forEach { type ->
                                            options.add(SelectValueDialogRequest.Option(
                                                    checked = type.separator == field.formatter,
                                                    title = viewModel.string(type.titleRes),
                                                    model = type.separator
                                            ))
                                        }
                                        val request = SelectValueDialogRequest(
                                                title = viewModel.string(R.string.dynamic_field_attr_common_values_formatter),
                                                withImmediateNotify = true,
                                                single = true,
                                                options = options,
                                                onSelected = {
                                                    field.formatter = it.firstOrNull() ?: ValueFormatterType.COMMA.separator
                                                    viewModel.onRefresh(withDelay = false)
                                                }
                                        )
                                        viewModel.dynamicContext.dialogState.requestSelectValueDialog(request)
                                    },
                                    customLayoutRes = layoutResourceRes
                            )
                    )
                }

                // options
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(SeparateScreenBlock(
                    enabled = editMode,
                    title = viewModel.string(R.string.dynamic_field_attr_select_options),
                    value = field.options.size.toString(),
                    clickListener = {
                        val request = SelectOptionsDialogRequest(
                            enabled = editMode,
                            title = viewModel.string(R.string.dynamic_field_attr_select_options),
                            options = field.options
                                .map { option ->
                                    SelectOptionsDialogRequest.Option(
                                        title = option.title,
                                        value = option.value
                                    )
                                },
                            onSelected = {
                                field.options = it.map { opt -> SelectDynamicField.Option(opt.title, opt.value) }.distinct()
                                viewModel.onRefresh(withDelay = false)
                            }
                        )

                        val dialogState = viewModel.dynamicContext.dialogState
                        dialogState.requestSelectOptionsDialog(request)
                    }
                ))

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}