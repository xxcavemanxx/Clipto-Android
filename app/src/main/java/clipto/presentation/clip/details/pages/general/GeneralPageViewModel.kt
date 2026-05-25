package clipto.presentation.clip.details.pages.general

import android.app.Application
import android.text.Spannable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import clipto.AppContext
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.config.IAppConfig
import clipto.domain.Clip
import clipto.domain.FastAction
import clipto.domain.TextType
import clipto.extensions.TextTypeExt
import clipto.presentation.blocks.*
import clipto.presentation.blocks.layout.ChipsRowBlock
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.SeparatorHorizontalBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.clip.details.pages.general.blocks.*
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.repository.IClipRepository
import clipto.store.app.AppState
import clipto.store.clip.ClipState
import clipto.store.internet.InternetState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.essentials.StringUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GeneralPageViewModel @Inject constructor(
    app: Application,
    private val appConfig: IAppConfig,
    private val appState: AppState,
    private val clipState: ClipState,
    private val state: ClipDetailsState,
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val clipRepository: IClipRepository
) : RxViewModel(app) {

    val blocksLive: LiveData<List<BlockItem<GeneralPageFragment>>> by lazy {
        Transformations.map(state.clipDetails.getLiveData()) { createBlocks() }
    }

    val fastActionsUpdateLive = appState.requestFastActionsUpdate.getLiveData()

    fun getSettings() = appState.getSettings()
    fun isEditMode(): Boolean = clipState.screenState.getValue()?.isEditMode() == true
    fun getVisibleClipActions(spannable: Spannable) = appState.getVisibleClipActions(spannable)

    private fun createBlocks(): List<BlockItem<GeneralPageFragment>> {
        val blocks = mutableListOf<BlockItem<GeneralPageFragment>>()
        val textType = state.textType.requireValue()
        val clip = state.openedClip.requireValue()

        blocks.add(ToolbarBlock(this, clip))
        blocks.add(SeparatorVerticalBlock())
        blocks.add(AttrsBlock(clip))
        blocks.add(SeparatorVerticalBlock())
        blocks.add(FastActionsBlock(this, clip))
        blocks.add(SeparatorVerticalBlock())
        withTextTypes(textType, blocks)
        withExport(clip, blocks)

        return blocks
    }

    private fun withAttrs(clip: Clip, blocks: MutableList<BlockItem<GeneralPageFragment>>) {
        val attrs = mutableListOf<BlockItem<GeneralPageFragment>>()
        // created
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_created),
                value = StyleHelper.getCreateDateValue(clip)
            )
        )
        attrs.add(SeparatorHorizontalBlock())

        // edited or deleted
        if (clip.isDeleted()) {
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.clip_attr_deleted),
                    value = StyleHelper.getDeleteDateValue(clip)
                )
            )
        } else {
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.clip_attr_edited),
                    value = StyleHelper.getModifyDateValue(clip)
                )
            )
        }
        attrs.add(SeparatorHorizontalBlock())

        // updated
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_updated),
                value = StyleHelper.getUpdateDateValue(clip)
            )
        )
        attrs.add(SeparatorHorizontalBlock())

        // usage count
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_usageCount),
                value = StyleHelper.getUsageCountValue(clip)
            )
        )
        attrs.add(SeparatorHorizontalBlock())

        // characters
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_charsCount),
                value = StyleHelper.getCharactersValue(clip)
            )
        )
        attrs.add(SeparatorHorizontalBlock())

        // size
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_size),
                value = StyleHelper.getSizeValue(clip, app)
            )
        )
        blocks.add(RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0))
    }

    private fun withExport(clip: Clip, blocks: MutableList<BlockItem<GeneralPageFragment>>) {
        blocks.add(SpaceBlock(16))
        blocks.add(TitleBlock(R.string.clip_details_label_export))
        blocks.add(ExportBlock(this, clip))
    }

    private fun withTextTypes(textType: TextType, blocks: MutableList<BlockItem<GeneralPageFragment>>) {
        val items = TextTypeExt.types
            .map { type ->
                ChipBlock<TextTypeExt, GeneralPageFragment>(
                    model = type,
                    title = string(type.titleRes),
                    iconRes = type.iconRes,
                    checked = type.type == textType,
                    checkable = false,
                    cornerRadius = 8f,
                    minHeight = 36f,
                    onClicked = {
                        onTextTypeChanged(type.type)
                    }
                )
            }
        val indexOfChecked = items.indexOfFirst { it.checked }

        blocks.add(SpaceBlock(4))
        blocks.add(TitleBlock(R.string.text_config_type))
        blocks.add(ChipsRowBlock(items, scrollToPosition = indexOfChecked, nestedScrollingEnabled = false))
    }



    fun onTextTypeChanged(type: TextType) {
        state.textType.setValue(type)
    }

    fun onToggleFav() {
        val fav = state.fav.requireValue()
        state.fav.setValue(!fav)
    }

    fun onShare() {
        val clip = state.openedClip.requireValue()
        dialogState.requestFastAction(FastAction.SHARE, clip)
        onDismiss()
    }

    fun onDismiss() {
        state.dismiss.setValue(true, force = true)
    }

    fun onDelete() {
        val clip = state.openedClip.requireValue()
        val permanently = clip.isDeleted()
        dialogState.showConfirm(
            ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.confirm_delete_note_title),
                description = string(R.string.confirm_delete_note_description),
                confirmActionTextRes = R.string.menu_delete,
                onConfirmed = {
                    clipRepository.deleteAll(listOf(clip), permanently = permanently)
                        .subscribeBy("onDelete", appState) {
                            if (permanently) {
                                onDismiss()
                                clipState.close()
                            } else {
                                it.firstOrNull()?.let { deleted -> clipState.setViewState(deleted) }
                                onDismiss()
                            }
                        }
                },
                cancelActionTextRes = R.string.menu_cancel
            )
        )
    }

    fun onFastAction(action: FastAction) {
        state.fastAction.setValue(action, force = true)
    }



}