package clipto.dynamic.fields.provider

import android.app.Application
import android.text.InputFilter
import androidx.fragment.app.Fragment
import clipto.common.extensions.decode
import clipto.common.extensions.encode
import clipto.common.misc.GsonUtils
import clipto.config.IAppConfig
import clipto.dynamic.DynamicField
import clipto.dynamic.fields.SelectDynamicField
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.blocks.HeaderEditBlock
import clipto.dynamic.presentation.field.blocks.HeaderNewBlock
import clipto.dynamic.presentation.field.blocks.HeaderViewBlock
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.blocks.TwoInputBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import javax.inject.Inject

abstract class AbstractFieldProvider<F : DynamicField> : IFieldProvider<F> {

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var appConfig: IAppConfig

    private val labelMaxLengthFilters: Array<InputFilter> by lazy {
        arrayOf(
            InputFilter.LengthFilter(appConfig.getDynamicValueLabelMaxLength())
        )
    }

    final override fun createField(params: Map<String, Any?>): F {
        val map = params.toMutableMap()
        encodeStrings(map) { it.decode() }
        val field = newField(map)
        field.defaultLabel = app.getString(getTitleRes())
        (map[DynamicField.ATTR_LABEL] as? String)?.let { field.label = it }
        (map[DynamicField.ATTR_PREFIX] as? String)?.let { field.prefix = it }
        (map[DynamicField.ATTR_SUFFIX] as? String)?.let { field.suffix = it }
        (map[DynamicField.ATTR_REQUIRED] as? Boolean)?.let { field.required = it }
        return field
    }

    final override fun createField(placeholder: String, params: Map<String, Any?>): F {
        val field = createField(params)
        field.placeholder = placeholder
        return field
    }

    override fun createPlaceholder(field: F): String {
        val params = mutableMapOf<String, Any?>(
            DynamicField.ATTR_ID to field.id,
            DynamicField.ATTR_LABEL to field.label,
            DynamicField.ATTR_PREFIX to field.prefix,
            DynamicField.ATTR_SUFFIX to field.suffix,
            DynamicField.ATTR_REQUIRED to field.required
        )
        fillMap(field, params)
        encodeStrings(params) { it.encode() }
        val json = GsonUtils.get().toJson(
            params.filterValues {
                when (it) {
                    is Boolean -> it
                    is String -> it.isNotEmpty()
                    else -> it != null
                }
            }
        )
        return "${DynamicField.BRACE_OPEN}${json}${DynamicField.BRACE_CLOSE}"
    }

    private fun encodeStrings(map: MutableMap<String, Any?>, mapper: (value: String) -> String) {
        map.forEach { entry ->
            when (val value = entry.value) {
                is String -> {
                    map[entry.key] = mapper.invoke(value)
                }
                is List<*> -> {
                    map[entry.key] = value.map {
                        when (it) {
                            is String -> mapper.invoke(it)
                            is Map<*, *> -> {
                                val newMap = it
                                    .mapKeys { ent -> ent.key.toString() }
                                    .toMutableMap()
                                encodeStrings(newMap, mapper)
                                newMap
                            }
                            is SelectDynamicField.Option -> it.copy(
                                title = it.title?.let(mapper),
                                value = it.value?.let(mapper)
                            )
                            else -> it
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    override fun createFieldConfig(field: F, viewMode: ViewMode, viewModel: DynamicFieldViewModel): List<BlockItem<Fragment>> {
        val blocks = mutableListOf<BlockItem<Fragment>>()
        val enabled = viewMode != ViewMode.VIEW

        when (viewMode) {
            ViewMode.INSERT -> {
                // header
                blocks.add(HeaderNewBlock(titleRes = getTitleRes(), viewModel = viewModel, ResultCode.INSERT))
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))

                // placeholder
                withPlaceholderBlock(field, viewModel, blocks)

                // label
                withLabelBlock(field, viewModel, blocks, enabled)

                // prefix & suffix
                withPrefixAndSuffixBlock(field, viewModel, blocks, enabled)
            }

            ViewMode.COPY -> {
                // header
                blocks.add(HeaderNewBlock(titleRes = getTitleRes(), viewModel = viewModel, ResultCode.COPY))
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))

                // placeholder
                withPlaceholderBlock(field, viewModel, blocks)

                // label
                withLabelBlock(field, viewModel, blocks, enabled)

                // prefix & suffix
                withPrefixAndSuffixBlock(field, viewModel, blocks, enabled)
            }

            ViewMode.VIEW -> {
                // header
                blocks.add(HeaderViewBlock(titleRes = getTitleRes(), viewModel = viewModel))
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))

                // placeholder
                withPlaceholderBlock(field, viewModel, blocks)

                // label
                withLabelBlock(field, viewModel, blocks, enabled)

                // prefix & suffix
                withPrefixAndSuffixBlock(field, viewModel, blocks, enabled)
            }

            ViewMode.EDIT -> {
                // header
                blocks.add(HeaderEditBlock(titleRes = getTitleRes(), viewModel = viewModel))
                blocks.add(SeparatorVerticalBlock(marginHoriz = 0))

                // placeholder
                withPlaceholderBlock(field, viewModel, blocks)

                // label
                withLabelBlock(field, viewModel, blocks, enabled)

                // prefix & suffix
                withPrefixAndSuffixBlock(field, viewModel, blocks, enabled)
            }

            ViewMode.FILL -> {

            }
        }

        // attrs
        bindConfig(field, viewMode, viewModel, blocks)

        return blocks
    }

    protected open fun hasPrefixAndSuffix(): Boolean = true

    protected abstract fun newField(params: Map<String, Any?>): F

    protected abstract fun fillMap(field: F, params: MutableMap<String, Any?>)

    abstract fun bindConfig(field: F, viewMode: ViewMode, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>)

    private fun withPlaceholderBlock(field: F, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            TextInputLayoutBlock(
                text = createPlaceholder(field),
                hint = viewModel.string(R.string.dynamic_field_attr_common_placeholder),
                maxLines = 1,
                enabled = false
            )
        )
    }

    private fun withLabelBlock(field: F, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>, enabled: Boolean) {
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            TextInputLayoutBlock(
                text = field.label,
                filters = labelMaxLengthFilters,
                hint = viewModel.string(R.string.dynamic_field_attr_common_label),
                counterMaxLength = appConfig.getDynamicValueLabelMaxLength(),
                counterEnabled = true,
                enabled = enabled,
                onTextChanged = {
                    field.label = it?.toString()
                    viewModel.onRefresh(withDelay = true)
                    null
                }
            )
        )
    }

    private fun withPrefixAndSuffixBlock(field: F, viewModel: DynamicFieldViewModel, blocks: MutableList<BlockItem<Fragment>>, enabled: Boolean) {
        if (!hasPrefixAndSuffix()) return
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            TwoInputBlock(
                enabled = enabled,
                firstText = field.prefix,
                secondText = field.suffix,
                onFirstTextChanged = {
                    field.prefix = it?.toString()
                    viewModel.onRefresh(withDelay = true)
                    null
                },
                onSecondTextChanged = {
                    field.suffix = it?.toString()
                    viewModel.onRefresh(withDelay = true)
                    null
                }
            )
        )
    }

}