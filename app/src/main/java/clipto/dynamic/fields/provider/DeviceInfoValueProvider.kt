package clipto.dynamic.fields.provider

import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.DeviceInfoDynamicValue
import clipto.dynamic.models.DeviceInfoType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.extensions.getTextColorAccentSpan
import clipto.extensions.getTextColorPrimarySpan
import clipto.extensions.getTextColorSecondarySpan
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoValueProvider @Inject constructor()
    : AbstractFieldProvider<DeviceInfoDynamicValue>() {

    override fun getId(): String = DeviceInfoDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_value_device
    override fun getDescriptionRes(): Int = R.string.dynamic_value_device_description

    override fun newField(params: Map<String, Any?>): DeviceInfoDynamicValue {
        return DeviceInfoDynamicValue().apply {
            (params[DynamicField.ATTR_TYPE] as? String)?.let { type = it }
        }
    }

    override fun fillMap(field: DeviceInfoDynamicValue, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_TYPE] = field.type
    }

    override fun bindConfig(field: DeviceInfoDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        val editMode = viewMode != ViewMode.VIEW && viewMode != ViewMode.FILL

        if (!editMode) return

        // value
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
                TextInputLayoutBlock(
                        hint = viewModel.string(R.string.dynamic_field_attr_common_value),
                        text = field.getFieldValue(),
                        enabled = false,
                )
        )

        // type
        if (editMode) {
            blocks.add(SpaceBlock(heightInDp = 16))
            val selectedType = DeviceInfoType.getByIdOrDefault(field.type)
            val typeLabel = viewModel.string(selectedType.titleRes)
            blocks.add(SeparateScreenBlock(
                    enabled = editMode,
                    title = viewModel.string(R.string.dynamic_value_attr_device_type),
                    value = typeLabel,
                    clickListener = {
                        val ctx = it.context
                        val request = SelectValueDialogRequest(
                                single = true,
                                withImmediateNotify = true,
                                title = viewModel.string(R.string.dynamic_value_attr_device_type),
                                options = DeviceInfoType.values()
                                        .map { type ->
                                            val exampleTitle = viewModel.string(R.string.dynamic_field_attr_date_example, "")
                                            val exampleValue = type.provider.invoke()
                                            val title = SimpleSpanBuilder()
                                                    .append(viewModel.string(type.titleRes), ctx.getTextColorPrimarySpan())
                                                    .append("\n")
                                                    .append(exampleTitle, ctx.getTextColorSecondarySpan(), RelativeSizeSpan(0.8f))
                                                    .append(exampleValue, ctx.getTextColorAccentSpan(), RelativeSizeSpan(0.8f), StyleSpan(Typeface.BOLD))
                                                    .build()
                                            SelectValueDialogRequest.Option(
                                                    checked = type.id == field.type,
                                                    title = title,
                                                    model = type
                                            )
                                        },
                                onSelected = {
                                    field.type = DeviceInfoType.getByIdOrDefault(it.firstOrNull()?.id).id
                                    viewModel.onRefresh(withDelay = false)
                                }
                        )

                        val dialogState = viewModel.dynamicContext.dialogState
                        dialogState.requestSelectValueDialog(request)
                    }
            ))
        }

        blocks.add(SpaceBlock(heightInDp = 16))
    }

}