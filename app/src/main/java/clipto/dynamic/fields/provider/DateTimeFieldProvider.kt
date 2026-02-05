package clipto.dynamic.fields.provider

import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.FormatUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.TimePeriod
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.DateTimeDynamicField
import clipto.dynamic.models.DateFormatType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.extensions.getTextColorAccentSpan
import clipto.extensions.getTextColorPrimarySpan
import clipto.extensions.getTextColorSecondarySpan
import clipto.extensions.getTitleRes
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.RequestFocusBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateTimeFieldProvider @Inject constructor() : AbstractUserFieldProvider<DateTimeDynamicField>() {

    override fun getId(): String = DateTimeDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_date
    override fun getDescriptionRes(): Int = R.string.dynamic_field_date_description

    override fun newField(params: Map<String, Any?>): DateTimeDynamicField {
        return DateTimeDynamicField().apply {
            (params[DynamicField.ATTR_DATE_FORMAT] as? String)?.let { format = it }
            (params[DynamicField.ATTR_VALUE] as? String)?.let { value = it }
            date = TimePeriod.byCode(value)?.toInterval()?.from
        }
    }

    override fun fillMap(field: DateTimeDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_DATE_FORMAT] = field.format
        params[DynamicField.ATTR_VALUE] = field.value
    }

    override fun createFieldInput(field: DateTimeDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return listOf(
            SingleDateBlock(
                title = field.getFieldLabel(),
                format = field.format,
                currentDate = { field.date },
                onDateChanged = {
                    field.date = it
                    onRefreshRequest.invoke()
                }
            )
        )
    }

    override fun bindConfig(field: DateTimeDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        when (viewMode) {
            ViewMode.FILL -> {
                blocks.add(
                    HeaderFillBlock(
                        titleRes = getTitleRes(),
                        title = field.getFieldLabel(),
                        actionTitleRes = R.string.menu_clear,
                        actionActive = field.hasValue(),
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

                // format
                val date = TimePeriod.byCode(field.value)?.toInterval()?.from ?: Date()
                blocks.add(SpaceBlock(heightInDp = 16))
                val helperText = viewModel.string(R.string.dynamic_field_attr_date_example, FormatUtils.formatDate(date, field.format))
                val layoutResourceRes = if (editMode) R.layout.block_dynamic_field_with_more_action else R.layout.block_text_input_layout
                blocks.add(
                    TextInputLayoutBlock(
                        hint = viewModel.string(R.string.dynamic_field_attr_date_format),
                        helperText = helperText,
                        text = field.format,
                        enabled = editMode,
                        onTextChanged = {
                            field.format = it?.toString().toNullIfEmpty()
                            viewModel.onRefresh(withDelay = true)
                            null
                        },
                        endIconClickListener = {
                            val ctx = it.context
                            val options = mutableListOf<SelectValueDialogRequest.Option<String>>()
                            DateFormatType.values().forEach { format ->
                                val pattern = format.getFormatPattern()
                                if (pattern != null) {
                                    val exampleTitle = viewModel.string(R.string.dynamic_field_attr_date_example, "")
                                    val exampleValue = format.formatDate(date).notNull()
                                    val title = SimpleSpanBuilder()
                                        .append(viewModel.string(format.titleRes), ctx.getTextColorPrimarySpan())
                                        .append("\n")
                                        .append(exampleTitle, ctx.getTextColorSecondarySpan(), RelativeSizeSpan(0.8f))
                                        .append(exampleValue, ctx.getTextColorAccentSpan(), RelativeSizeSpan(0.8f), StyleSpan(Typeface.BOLD))
                                        .build()
                                    options.add(
                                        SelectValueDialogRequest.Option(
                                            checked = field.format == pattern,
                                            model = pattern,
                                            title = title
                                        )
                                    )
                                }
                            }
                            val request = SelectValueDialogRequest(
                                title = viewModel.string(R.string.dynamic_field_attr_date_format),
                                withImmediateNotify = true,
                                single = true,
                                options = options,
                                onSelected = {
                                    field.format = it.firstOrNull()
                                    if (field.label.isNullOrEmpty() || DateFormatType.getByPattern(field.label) != null) {
                                        field.label = field.format
                                    }
                                    viewModel.onRefresh(withDelay = false)
                                }
                            )
                            viewModel.dynamicContext.dialogState.requestSelectValueDialog(request)
                        },
                        customLayoutRes = layoutResourceRes
                    )
                )

                // default value
                blocks.add(SpaceBlock(heightInDp = 16))
                val defValueResourceRes = if (editMode) R.layout.block_dynamic_field_with_more_action else R.layout.block_text_input_layout
                blocks.add(
                    TextInputLayoutBlock(
                        text = TimePeriod.byCode(field.value)?.getTitleRes()?.let { viewModel.string(it) } ?: field.value,
                        hint = viewModel.string(R.string.dynamic_field_attr_common_default_value),
                        inputType = TextInputLayoutBlock.TextInputType.NULL,
                        enabled = editMode,
                        endIconClickListener = {
                            val ctx = it.context
                            val options = mutableListOf<SelectValueDialogRequest.Option<String>>()
                            TimePeriod.datePeriods.forEach { period ->
                                val code = period.code!!
                                val exampleTitle = viewModel.string(R.string.dynamic_field_attr_date_example, "")
                                val exampleValue = FormatUtils.formatDate(period.toInterval()?.from, field.format)
                                val title = SimpleSpanBuilder()
                                    .append(viewModel.string(period.getTitleRes()), ctx.getTextColorPrimarySpan())
                                    .append("\n")
                                    .append(exampleTitle, ctx.getTextColorSecondarySpan(), RelativeSizeSpan(0.8f))
                                    .append(exampleValue, ctx.getTextColorAccentSpan(), RelativeSizeSpan(0.8f), StyleSpan(Typeface.BOLD))
                                    .build()
                                options.add(
                                    SelectValueDialogRequest.Option(
                                        checked = field.value == code,
                                        model = code,
                                        title = title
                                    )
                                )
                            }
                            val request = SelectValueDialogRequest(
                                title = viewModel.string(R.string.dynamic_field_attr_common_default_value),
                                withImmediateNotify = true,
                                single = true,
                                options = options,
                                onSelected = {
                                    field.value = it.firstOrNull()
                                    viewModel.onRefresh(withDelay = false)
                                }
                            )
                            viewModel.dynamicContext.dialogState.requestSelectValueDialog(request)
                        },
                        customLayoutRes = defValueResourceRes
                    )
                )

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}