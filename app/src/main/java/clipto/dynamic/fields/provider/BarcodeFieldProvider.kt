package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.common.extensions.notNull
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.BarcodeDynamicField
import clipto.dynamic.models.ValueFormatterType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeFieldProvider @Inject constructor() : AbstractUserFieldProvider<BarcodeDynamicField>() {

    override fun getId(): String = BarcodeDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_barcode_scanning
    override fun getDescriptionRes(): Int = R.string.dynamic_field_barcode_scanning_description

    override fun newField(params: Map<String, Any?>): BarcodeDynamicField {
        return BarcodeDynamicField().apply {
            (params[DynamicField.ATTR_MULTIPLE] as? Boolean)?.let { multiple = it }
            (params[DynamicField.ATTR_FORMATTER] as? String)?.let { formatter = it }
        }
    }

    override fun fillMap(field: BarcodeDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_MULTIPLE] = field.multiple
        params[DynamicField.ATTR_FORMATTER] = field.formatter.takeIf { field.multiple }
    }

    override fun createFieldInput(field: BarcodeDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return createFieldInput(canStartScanning = false, field, context, onRefreshRequest)
    }

    override fun bindConfig(field: BarcodeDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
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
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.addAll(createFieldInput(canStartScanning = true, field, viewModel.dynamicContext) { viewModel.onRefresh() })
                blocks.add(SpaceBlock(heightInDp = 16))
            }
            else -> {
                val editMode = viewMode != ViewMode.VIEW

                // multiple
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                        SwitchBlock(
                                titleRes = R.string.dynamic_field_attr_barcode_scanning_multiple,
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
                                    hint = viewModel.string(R.string.dynamic_field_attr_barcode_values_formatter),
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
                                                title = viewModel.string(R.string.dynamic_field_attr_barcode_values_formatter),
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

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

    private fun createFieldInput(
            canStartScanning: Boolean,
            field: BarcodeDynamicField,
            context: DynamicContext,
            onRefreshRequest: () -> Unit
    ): List<BlockItem<Fragment>> {
        val fields = mutableListOf<BlockItem<Fragment>>()
        val fieldValues = field.values
        fieldValues.forEachIndexed { index, value ->
            fields.add(
                    ScanBarcodeBlock(
                            startScanning = false,
                            dialogState = context.dialogState,
                            title = getFieldLabel(field, index),
                            value = value,
                            canRemove = true,
                            onValueChanged = { before, after ->
                                val values = field.values.toMutableList()
                                if (after.isNullOrBlank()) {
                                    values.remove(before)
                                } else {
                                    val idx = index.takeIf { values.getOrNull(it) == before } ?: values.indexOf(before)
                                    if (idx != -1) {
                                        values[idx] = after
                                    }
                                }
                                field.values = values
                                onRefreshRequest.invoke()
                            }
                    )
            )
            if (field.multiple) {
                fields.add(SpaceBlock(heightInDp = 12))
            }
        }
        if (field.multiple || fieldValues.isEmpty()) {
            fields.add(
                    ScanBarcodeBlock(
                            dialogState = context.dialogState,
                            startScanning = canStartScanning && fieldValues.isEmpty(),
                            title = getFieldLabel(field, fieldValues.size),
                            canRemove = false,
                            onValueChanged = { _, text ->
                                val values = field.values
                                if (!text.isNullOrBlank()) {
                                    field.values = values.plus(text)
                                    onRefreshRequest.invoke()
                                }
                            }
                    )
            )
        }
        return fields
    }

    private fun getFieldLabel(field: BarcodeDynamicField, index: Int = 0): String {
        return if (field.multiple) {
            StringBuilder()
                    .append(field.getFieldLabel())
                    .append(" (")
                    .append(index + 1)
                    .append(")")
                    .toString()
        } else {
            field.getFieldLabel()
        }
    }

}