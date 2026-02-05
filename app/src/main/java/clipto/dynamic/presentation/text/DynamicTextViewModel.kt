package clipto.dynamic.presentation.text

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.domain.TextType
import clipto.dynamic.*
import clipto.dynamic.fields.TextToggleDynamicField
import clipto.dynamic.fields.provider.IUserFieldProvider
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.dynamic.presentation.text.blocks.PreviewBlock
import clipto.dynamic.presentation.text.blocks.TextBlock
import clipto.dynamic.presentation.text.model.ViewMode
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.ThreeButtonsToggleBlock
import clipto.presentation.blocks.TwoButtonsToggleBlock
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.contextactions.ContextActionsState
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DynamicTextViewModel @Inject constructor(
    app: Application,
    val appConfig: IAppConfig,
    val dialogState: DialogState,
    private val mainState: MainState,
    private val factory: DynamicValuesFactory,
    private val dynamicTextState: DynamicTextState,
    private val dynamicFieldState: DynamicFieldState,
    private val contextActionsState: ContextActionsState,
    private val dynamicFieldContext: DynamicContext,
    val dynamicTextHelper: DynamicTextHelper
) : RxViewModel(app) {

    val configLive = MutableLiveData<Config>()

    val blocksLive: LiveData<List<BlockItem<Fragment>>> = Transformations.map(configLive) {
        val request = it.request
        val text = request.text
        val fields = request.fields
        val textType = request.config.textType ?: TextType.TEXT_PLAIN
        val viewMode = normalize(it.viewMode, textType)

        val blocks = mutableListOf<BlockItem<Fragment>>()
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(createViewModeBlock(viewMode, textType))
        when (viewMode) {
            ViewMode.TEXT -> {
                blocks.add(TextBlock(this, fields, textType, text))
            }
            ViewMode.FORM -> {
                fields.forEach { field -> add(field, blocks) }
                blocks.add(SpaceBlock(heightInDp = 12))
            }
            ViewMode.PREVIEW -> {
                val config = DynamicValueConfig(textType = textType)
                val newText = dynamicTextHelper.toString(text, fields, config)
                blocks.add(PreviewBlock(this, textType, newText))
            }
        }
        blocks
    }

    fun getListConfig() = mainState.getListConfig()

    private fun createViewModeBlock(viewMode: ViewMode, textType: TextType): BlockItem<Fragment> {
        return when (textType) {
            TextType.TEXT_PLAIN,
            TextType.MARKDOWN,
            TextType.LINK,
            TextType.HTML -> {
                val position =
                    when (viewMode) {
                        ViewMode.TEXT -> 0
                        ViewMode.FORM -> 1
                        ViewMode.PREVIEW -> 2
                    }
                ThreeButtonsToggleBlock(
                    firstButtonTextRes = R.string.dynamic_text_view_mode_text,
                    secondButtonTextRes = R.string.dynamic_text_view_mode_form,
                    thirdButtonTextRes = R.string.button_preview,
                    onFirstButtonClick = { onTextMode() },
                    onSecondButtonClick = { onFormMode() },
                    onThirdButtonClick = { onPreviewMode() },
                    selectedButtonIndex = position
                )
            }
            else -> {
                val position = if (viewMode == ViewMode.FORM) 0 else 1
                TwoButtonsToggleBlock(
                    firstButtonTextRes = R.string.dynamic_text_view_mode_form,
                    secondButtonTextRes = R.string.button_preview,
                    onFirstButtonClick = { onFormMode() },
                    onSecondButtonClick = { onPreviewMode() },
                    selectedButtonIndex = position
                )
            }
        }
    }

    private fun normalize(viewMode: ViewMode, textType: TextType): ViewMode {
        return when (textType) {
            TextType.TEXT_PLAIN,
            TextType.MARKDOWN,
            TextType.LINK,
            TextType.HTML -> viewMode
            else -> {
                if (viewMode == ViewMode.TEXT) {
                    ViewMode.FORM
                } else {
                    viewMode
                }
            }
        }
    }

    override fun doCreate() {
        dynamicTextState.dynamicTextRequest.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .subscribeBy("dynamicTextRequest") {
                val config = configLive.value?.copy(request = it) ?: Config(request = it)
                configLive.postValue(config)
            }
    }

    fun onFieldClicked(formField: FormField) {
        if (formField.isTextToggle()) {
            val field = formField.field as TextToggleDynamicField
            field.checked = !field.checked
            onTextMode()
        } else {
            dynamicFieldState.requestFillField(formField.field)
                .subscribeBy {
                    onTextMode()
                }
        }
    }

    fun onShowHint() {
        dialogState.showHint(
            HintDialogData(
                descriptionIsMarkdown = true,
                iconRes = R.drawable.hint_dynamic_text,
                title = string(R.string.dynamic_text_hint_title),
                description = string(R.string.dynamic_text_hint_description)
            )
        )
    }

    fun onClosed() {
        configLive.value?.request?.let { request ->
            val response = dynamicTextState.dynamicTextResponse.getValue()
            if (response != request) {
                dynamicTextState.dynamicTextResponse.setValue(request.copy(canceled = true))
                contextActionsState.requestFinish()
            } else if (!request.config.fastActionRequest) {
                contextActionsState.requestFinish()
            } else {
                // nothing
            }
        }
    }

    fun onApply() {
        configLive.value?.request?.let { request ->
            dynamicTextState.dynamicTextResponse.setValue(request)
            dismiss()
        }
    }

    private fun onTextMode() {
        configLive.value?.let { config ->
            configLive.postValue(config.copy(viewMode = ViewMode.TEXT))
        }
    }

    private fun onFormMode() {
        configLive.value?.let { config ->
            configLive.postValue(config.copy(viewMode = ViewMode.FORM))
        }
    }

    private fun onPreviewMode() {
        configLive.value?.let { config ->
            configLive.postValue(config.copy(viewMode = ViewMode.PREVIEW))
        }
    }

    private fun add(ff: FormField, blocks: MutableList<BlockItem<Fragment>>) {
        val fieldProvider = factory.getFieldProvider(ff.field)
        if (fieldProvider is IUserFieldProvider) {
            val fieldInput = fieldProvider.createFieldInput(ff.field, dynamicFieldContext) { onFormMode() }
            blocks.add(SpaceBlock(heightInDp = 12))
            blocks.addAll(fieldInput)
        }
    }

    data class Config(
        val request: DynamicTextRequestResponse,
        val viewMode: ViewMode = ViewMode.TEXT
    )

}