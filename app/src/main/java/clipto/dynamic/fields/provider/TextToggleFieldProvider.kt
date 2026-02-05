package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.TextToggleDynamicField
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
class TextToggleFieldProvider @Inject constructor() : AbstractUserFieldProvider<TextToggleDynamicField>() {

    override fun getId(): String = TextToggleDynamicField.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_text_toggle
    override fun getDescriptionRes(): Int = R.string.dynamic_field_text_toggle_description
    override fun hasPrefixAndSuffix(): Boolean = false

    override fun newField(params: Map<String, Any?>): TextToggleDynamicField {
        return TextToggleDynamicField().apply {
            (params[DynamicField.ATTR_TOGGLE_CHECKED] as? Boolean)?.let { checked = it }
            (params[DynamicField.ATTR_TOGGLE_TEXT] as? String)?.let { text = it }
        }
    }

    override fun fillMap(field: TextToggleDynamicField, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_TOGGLE_CHECKED] = field.checked
        params[DynamicField.ATTR_TOGGLE_TEXT] = field.text
    }

    override fun createFieldInput(field: TextToggleDynamicField, context: DynamicContext, onRefreshRequest: () -> Unit): List<BlockItem<Fragment>> {
        return listOf(
            SwitchBlock(
                title = field.getFieldLabel(),
                description = field.text,
                checked = field.checked,
                maxLines = 2,
                clickListener = { _, isChecked ->
                    field.checked = isChecked
                    onRefreshRequest.invoke()
                }
            )
        )
    }

    override fun bindConfig(field: TextToggleDynamicField, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
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

                // text
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                    TextInputLayoutBlock(
                        text = field.text,
                        hint = viewModel.string(R.string.dynamic_field_attr_toggle_text),
                        maxLines = Integer.MAX_VALUE,
                        enabled = editMode,
                        onTextChanged = {
                            field.text = it?.toString()
                            viewModel.onRefresh(withDelay = true)
                            null
                        }
                    )
                )

                // checked
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(
                    SwitchBlock(
                        titleRes = R.string.dynamic_field_attr_common_default_value,
                        checked = field.checked,
                        enabled = editMode,
                        clickListener = { _, checked ->
                            field.checked = checked
                            viewModel.onRefresh(withDelay = false)
                        }
                    )
                )

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}