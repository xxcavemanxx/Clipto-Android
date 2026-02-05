package clipto.dynamic.fields.provider

import androidx.fragment.app.Fragment
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.ReferenceDynamicValue
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.clip.ClipState
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceFieldProvider @Inject constructor(
    val clipState: ClipState,
) : AbstractFieldProvider<ReferenceDynamicValue>() {

    override fun getId(): String = ReferenceDynamicValue.ID
    override fun getTitleRes(): Int = R.string.dynamic_field_reference
    override fun getDescriptionRes(): Int = R.string.dynamic_field_reference_description

    override fun newField(params: Map<String, Any?>): ReferenceDynamicValue {
        return ReferenceDynamicValue().apply {
            (params[DynamicField.ATTR_REFERENCE_REF] as? String)?.let { refName = it }
            (params[DynamicField.ATTR_REFERENCE_INTRINSIC] as? Boolean)?.let { intrinsic = it }
        }
    }

    override fun fillMap(field: ReferenceDynamicValue, params: MutableMap<String, Any?>) {
        params[DynamicField.ATTR_REFERENCE_REF] = field.refName
        params[DynamicField.ATTR_REFERENCE_INTRINSIC] = field.intrinsic
    }

    override fun bindConfig(field: ReferenceDynamicValue, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        when (viewMode) {
            ViewMode.FILL -> {
                val fieldRef = field.ref
                val providerRef = fieldRef?.let { viewModel.factory.getFieldProvider(it) }
                if (providerRef is AbstractFieldProvider) {
                    providerRef.bindConfig(fieldRef, viewMode, viewModel, blocks)
                }
            }
            else -> {
                val editMode = viewMode != ViewMode.VIEW

                // refs
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(SeparateScreenBlock(
                    enabled = editMode,
                    title = viewModel.string(R.string.dynamic_field_attr_ref_value),
                    value = field.refName,
                    clickListener = {
                        val request = SelectValueDialogRequest(
                            single = true,
                            withImmediateNotify = true,
                            title = viewModel.string(R.string.dynamic_field_attr_ref_value),
                            options = (clipState.formFields.getValue() ?: emptyList())
                                .map { it.field }
                                .filter { it.isUserInput() && !it.isReference() }
                                .mapNotNull { it.label }
                                .map { label ->
                                    SelectValueDialogRequest.Option(
                                        checked = label == field.refName,
                                        title = label,
                                        model = label
                                    )
                                },
                            onSelected = {
                                field.refName = it.firstOrNull()
                                viewModel.onRefresh(withDelay = false)
                            }
                        )

                        val dialogState = viewModel.dynamicContext.dialogState
                        dialogState.requestSelectValueDialog(request)
                    }
                ))

                // intrinsic
                if (!field.refName.isNullOrBlank()) {
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(
                        SwitchBlock(
                            titleRes = R.string.dynamic_field_attr_ref_intrinsic,
                            checked = field.intrinsic,
                            enabled = editMode,
                            clickListener = { _, checked ->
                                field.intrinsic = checked
                                viewModel.onRefresh(withDelay = false)
                            }
                        )
                    )
                }

                blocks.add(SpaceBlock(heightInDp = 16))
            }
        }
    }

}