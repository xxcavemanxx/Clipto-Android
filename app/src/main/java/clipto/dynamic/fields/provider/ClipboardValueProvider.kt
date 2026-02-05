package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.fields.ClipboardDynamicValue
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.clipboard.ClipboardState
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardValueProvider @Inject constructor(
        private val clipboardState: ClipboardState
) : AbstractFieldProvider<ClipboardDynamicValue>() {

    override fun getId(): String = ClipboardDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_value_clipboard
    override fun getDescriptionRes(): Int = R.string.dynamic_value_clipboard_description

    override fun newField(params: Map<String, Any?>): ClipboardDynamicValue = ClipboardDynamicValue(clipboardState)

    override fun fillMap(field: ClipboardDynamicValue, params: MutableMap<String, Any?>) = Unit

    override fun bindConfig(field: ClipboardDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        val editMode = viewMode != ViewMode.VIEW && viewMode != ViewMode.FILL

        if(!editMode) return

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
    }

}