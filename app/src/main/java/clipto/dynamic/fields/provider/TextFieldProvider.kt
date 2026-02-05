package clipto.dynamic.fields.provider

import android.text.InputFilter
import androidx.fragment.app.Fragment
import clipto.common.extensions.ifNotEmpty
import clipto.common.presentation.text.InputFilterMinMax
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.TextDynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderFillBlock
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.extensions.toClip
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.RequestFocusBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.clipboard.ClipboardState
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextFieldProvider @Inject constructor(
        private val clipboardState: ClipboardState
) : AbstractUserFieldProvider<TextDynamicField>() {

    override fun getId(): String = TextDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_text
    override fun getDescriptionRes(): Int = R.string.dynamic_field_text_description

    override fun newField(params: Map<String, Any?>): TextDynamicField {
        return TextDynamicField().apply {
            (params[DynamicField.ATTR_DEFAULT_CLIPBOARD] as? Boolean)?.let { clipboard = it }
            (params[DynamicField.ATTR_MAX_LENGTH] as? Number)?.let { maxLength = it.toInt() }
            (params[DynamicField.ATTR_MULTI_LINE] as? Boolean)?.let { multiLine = it }
            (params[DynamicField.ATTR_VALUE] as? String)?.let { value = it }
            if (clipboard) {
                value = clipboardState.getPrimaryClip()?.toClip(app)?.text
            }
        }
    }

    override fun fillMap(field: TextDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_DEFAULT_CLIPBOARD] = field.clipboard
        params[DynamicField.ATTR_MAX_LENGTH] = field.maxLength
        params[DynamicField.ATTR_MULTI_LINE] = field.multiLine
        params[DynamicField.ATTR_VALUE] = field.value
    }

    override fun createFieldInput(field: TextDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return listOf(
                TextInputLayoutBlock(
                        text = field.getFieldValue(),
                        hint = field.getFieldLabel(),
                        maxLines = field.getMaxLines(),
                        filters = arrayOf(InputFilter.LengthFilter(field.getMaxLength())),
                        onTextChanged = {
                            field.value = it?.toString().ifNotEmpty()
                            null
                        },
                        counterEnabled = field.maxLength != null,
                        counterMaxLength = field.getMaxLength()
                )
        )
    }

    override fun bindConfig(field: TextDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
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

                // max length
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                    TextInputLayoutBlock(
                        text = field.maxLength?.toString(),
                        hint = viewModel.string(R.string.dynamic_field_attr_text_max_length),
                        filters = arrayOf(InputFilterMinMax(1, Integer.MAX_VALUE - 1)),
                        inputType = TextInputLayoutBlock.TextInputType.NUMBER,
                        enabled = editMode,
                        onTextChanged = {
                            field.maxLength = it.toString().toIntOrNull()
                            viewModel.onRefresh(withDelay = true)
                            null
                        }
                    )
                )

                // multi line
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                    SwitchBlock(
                        titleRes = R.string.dynamic_field_attr_text_multi_line,
                        checked = field.multiLine,
                        enabled = editMode,
                        clickListener = { _, checked ->
                            field.multiLine = checked
                            viewModel.onRefresh(withDelay = true)
                        }
                    )
                )

                // default value
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                        TextInputLayoutBlock(
                                text = field.value,
                                hint = viewModel.string(R.string.dynamic_field_attr_common_default_value),
                                enabled = editMode && !field.clipboard,
                                onTextChanged = {
                                    field.value = it?.toString()
                                    viewModel.onRefresh(withDelay = true)
                                    null
                                }
                        )
                )

                // clipboard
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                        SwitchBlock(
                                titleRes = R.string.dynamic_field_attr_text_use_clipboard,
                                checked = field.clipboard,
                                enabled = editMode,
                                clickListener = { _, checked ->
                                    field.value = null
                                    field.clipboard = checked
                                    viewModel.onRefresh(withDelay = false)
                                }
                        )
                )

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}