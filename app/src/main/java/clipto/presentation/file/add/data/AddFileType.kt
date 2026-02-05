package clipto.presentation.file.add.data

import androidx.annotation.StringRes
import clipto.dao.sharedprefs.data.AddFileScreenData
import clipto.domain.FileRef
import com.google.gson.annotations.SerializedName
import com.wb.clipboard.R

enum class AddFileType(
    val index: Int
) {

    @SerializedName("FILE")
    FILE(index = 0) {
        override fun canShowHint(hints: AddFileScreenData): Boolean = !hints.hideFile
        override fun hideHint(hints: AddFileScreenData) = hints.copy(hideFile = true)
        override fun getTitleRes(files: List<FileRef>): Int = if (files.size == 1) R.string.file_action_new_file else R.string.file_action_new_files
        override fun getDescriptionRes(files: List<FileRef>): Int = if (files.size == 1) R.string.file_action_new_file_description else R.string.file_action_new_files_description
    },

    @SerializedName("NOTE")
    NOTE(index = 1) {
        override fun canShowHint(hints: AddFileScreenData): Boolean = !hints.hideNote
        override fun hideHint(hints: AddFileScreenData) = hints.copy(hideNote = true)
        override fun getTitleRes(files: List<FileRef>): Int = R.string.file_action_new_note
        override fun getDescriptionRes(files: List<FileRef>): Int = if (files.size == 1) R.string.file_action_new_note_description else R.string.file_action_new_notes_description
    },

    @SerializedName("ATTACHMENT")
    ATTACHMENT(index = 2) {
        override fun canShowHint(hints: AddFileScreenData): Boolean = !hints.hideAttachment
        override fun hideHint(hints: AddFileScreenData) = hints.copy(hideAttachment = true)
        override fun getTitleRes(files: List<FileRef>): Int = if (files.size == 1) R.string.file_action_new_attachment else R.string.file_action_new_attachments
        override fun getDescriptionRes(files: List<FileRef>): Int = if (files.size == 1) R.string.file_action_new_attachment_description else R.string.file_action_new_attachments_description
    };

    abstract fun getTitleRes(files: List<FileRef>): Int
    abstract fun getDescriptionRes(files: List<FileRef>): Int

    abstract fun canShowHint(hints: AddFileScreenData): Boolean
    abstract fun hideHint(hints: AddFileScreenData): AddFileScreenData

}