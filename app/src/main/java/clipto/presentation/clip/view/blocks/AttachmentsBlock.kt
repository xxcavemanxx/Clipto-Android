package clipto.presentation.clip.view.blocks

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import clipto.AppContext
import clipto.common.extensions.setDebounceClickListener
import clipto.domain.FileRef
import clipto.domain.isEditMode
import clipto.domain.isEditable
import clipto.extensions.isNew
import clipto.extensions.log
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import clipto.store.clip.ClipScreenState
import com.google.android.material.chip.Chip
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_attachments.view.*

class AttachmentsBlock(
    private val backgroundColor: Int? = null,
    private val screenState: ClipScreenState,
    private val onEdit: () -> Unit = {},
    private val onShowAttachment: (fileRef: FileRef) -> Unit = {},
    private val onRemoveAttachment: (fileRef: FileRef) -> Unit = {},
    private val onGetFiles: (changesCallback: (files: List<FileRef>) -> Unit) -> Unit,
    private val onGetFilesChanges: (changesCallback: (fileRef: FileRef) -> Unit) -> Unit = {},
    private val fileIds: List<String> = screenState.value.fileIds
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_attachments

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is AttachmentsBlock
                && screenState.value == item.screenState.value
                && screenState.viewMode == item.screenState.viewMode
                && fileIds == item.fileIds
                && backgroundColor == item.backgroundColor
                && !screenState.value.isNew()

    override fun onInit(context: Fragment, block: View) {
        block.cgFiles.setOnClickListener(
            DoubleClickListenerWrapper(
                block.context,
                {
                    val settings = AppContext.get().getSettings()
                    val state = getScreenState(block)
                    settings.doubleClickToEdit
                            && state.isEditable()
                            && !state.isEditMode()
                },
                {
                    getAttachmentsBlock(block)?.onEdit?.invoke()
                }
            )
        )
    }

    override fun onBind(context: Fragment, block: View) {
        block.tag = this

        onGetFiles {
            initAttachments(block, it)
        }

        onGetFilesChanges {
            updateAttachment(block, it)
        }
    }

    private fun initAttachments(block: View, attachments: List<FileRef>) {
        val editable = screenState.isEditMode()
        val filesGroup = block.cgFiles
        val context = block.context
        attachments.forEachIndexed { index, attachment ->
            val chip =
                if (filesGroup.childCount - 1 >= index) {
                    filesGroup.getChildAt(index) as Chip
                } else {
                    val newChip = FrameLayout.inflate(context, R.layout.chip_attachment, null) as Chip
                    backgroundColor?.let { newChip.chipBackgroundColor = ColorStateList.valueOf(it) }
                    filesGroup.addView(newChip)
                    newChip
                }
            StyleHelper.bind(chip, attachment)
            updateAttachment(block, chip, editable)
        }
        val viewsToRemove = filesGroup.childCount - attachments.size
        if (viewsToRemove > 0) {
            filesGroup.removeViewsInLayout(attachments.size, viewsToRemove)
        }
    }

    private fun updateAttachment(block: View, attachment: FileRef) {
        val filesGroup = block.cgFiles
        val context = block.context
        val same = filesGroup.children.find { (it.tag as? FileRef) == attachment } as Chip?
        log("UPDATE ATTACHMENT :: {} - same={}", attachment.getUid(), same)
        if (same != null) {
            StyleHelper.bind(same, attachment)
        } else {
            val editable = screenState.isEditMode()
            val chip = FrameLayout.inflate(context, R.layout.chip_attachment, null) as Chip
            backgroundColor?.let { chip.chipBackgroundColor = ColorStateList.valueOf(it) }
            StyleHelper.bind(chip, attachment)
            updateAttachment(block, chip, editable)
            filesGroup.addView(chip)
        }
    }

    private fun updateAttachment(block: View, chip: Chip, editable: Boolean) {
        val filesGroup = block.cgFiles

        chip.isCloseIconVisible = editable

        if (editable) {
            chip.setOnCloseIconClickListener {
                val att = chip.tag as FileRef
                filesGroup.removeView(chip)
                getAttachmentsBlock(block)?.onRemoveAttachment?.invoke(att)
            }
        }

        chip.setDebounceClickListener {
            val att = chip.tag as FileRef
            getAttachmentsBlock(block)?.onShowAttachment?.invoke(att)
        }

        chip.setOnLongClickListener {
            val att = chip.tag as FileRef
            getAttachmentsBlock(block)?.onShowAttachment?.invoke(att)
            true
        }
    }

    private fun getAttachmentsBlock(block: View): AttachmentsBlock? {
        val tag = block.tag
        if (tag is AttachmentsBlock) {
            return tag
        }
        return null
    }

    private fun getScreenState(block: View): ClipScreenState? {
        return getAttachmentsBlock(block)?.screenState
    }

}