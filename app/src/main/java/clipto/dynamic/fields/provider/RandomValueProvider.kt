package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.RandomDynamicValue
import clipto.dynamic.models.RandomType
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ChipBlock
import clipto.presentation.blocks.layout.ChipsRowBlock
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.select.options.SelectOptionsDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RandomValueProvider @Inject constructor()
    : AbstractFieldProvider<RandomDynamicValue>() {

    override fun getId(): String = RandomDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_value_random
    override fun getDescriptionRes(): Int = R.string.dynamic_value_random_description

    override fun newField(params: Map<String, Any?>): RandomDynamicValue {
        return RandomDynamicValue().apply {
            params[DynamicField.ATTR_TYPE]?.let { type = RandomType.byIdOrDefault(it.toString()).type }
            (params[DynamicField.ATTR_OPTIONS] as? List<*>)?.let { opts ->
                options = opts.mapNotNull { it?.toString() }
            }
        }
    }

    override fun fillMap(field: RandomDynamicValue, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_TYPE] = field.type
        params[DynamicField.ATTR_OPTIONS] = field.options.takeIf { it.isNotEmpty() }
    }

    override fun bindConfig(field: RandomDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        val editMode = viewMode != ViewMode.VIEW && viewMode != ViewMode.FILL

        if (!editMode) return

        val types = RandomType.values().map { randomType ->
            ChipBlock<RandomType, Fragment>(
                    model = randomType,
                    checkable = false,
                    checked = randomType.type == field.type,
                    title = viewModel.string(randomType.titleRes),
                    onClicked = {
                        field.type = randomType.type
                        viewModel.onRefresh(withDelay = false)
                    }
            )
        }

        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(ChipsRowBlock(types))

        if (field.type == RandomType.CUSTOM.type) {
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(SeparateScreenBlock(
                    enabled = editMode,
                    title = viewModel.string(R.string.dynamic_field_attr_select_options),
                    value = field.options.size.toString(),
                    clickListener = {
                        val request = SelectOptionsDialogRequest(
                                withTitle = false,
                                enabled = editMode,
                                title = viewModel.string(R.string.dynamic_field_attr_select_options),
                                options = field.options
                                        .map { option ->
                                            SelectOptionsDialogRequest.Option(
                                                    value = option
                                            )
                                        },
                                onSelected = {
                                    field.options = it.mapNotNull { opt -> opt.value }.distinct()
                                    viewModel.onRefresh(withDelay = false)
                                }
                        )

                        val dialogState = viewModel.dynamicContext.dialogState
                        dialogState.requestSelectOptionsDialog(request)
                    }
            ))
        }

        blocks.add(SpaceBlock(heightInDp = 16))
    }

}