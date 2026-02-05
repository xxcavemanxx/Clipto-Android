package clipto.dynamic.fields.provider

import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.FormatUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.DateTimeDynamicValue
import clipto.dynamic.models.DateFormatType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.extensions.getTextColorAccentSpan
import clipto.extensions.getTextColorPrimarySpan
import clipto.extensions.getTextColorSecondarySpan
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateTimeValueProvider @Inject constructor()
    : AbstractFieldProvider<DateTimeDynamicValue>() {

    override fun getId(): String = DateTimeDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_value_date
    override fun getDescriptionRes(): Int = R.string.dynamic_value_date_description

    override fun newField(params: Map<String, Any?>): DateTimeDynamicValue {
        return DateTimeDynamicValue().apply {
            (params[DynamicField.ATTR_DATE_FORMAT] as? String)?.let { format = it }
        }
    }

    override fun fillMap(field: DateTimeDynamicValue, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_DATE_FORMAT] = field.format
    }

    override fun bindConfig(field: DateTimeDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        val editMode = viewMode != ViewMode.VIEW && viewMode != ViewMode.FILL

        if (!editMode) return

        // format
        val date = Date()
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
                                    options.add(SelectValueDialogRequest.Option(
                                            checked = field.format == pattern,
                                            model = pattern,
                                            title = title
                                    ))
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

        blocks.add(SpaceBlock(heightInDp = 16))
    }

}