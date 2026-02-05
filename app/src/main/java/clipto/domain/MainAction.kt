package clipto.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.wb.clipboard.R

enum class MainAction(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val rememberLastAction: Boolean = true
) {

    @SerializedName("FOLDER_MOVE_NOTE")
    FOLDER_MOVE_NOTE(
        titleRes = R.string.main_action_folders_move_note_title,
        descriptionRes = R.string.main_action_folders_move_note_description,
        iconRes = R.drawable.main_action_folder_move_file,
        rememberLastAction = false
    ),

    @SerializedName("FOLDER_MOVE_FILE")
    FOLDER_MOVE_FILE(
        titleRes = R.string.main_action_folders_move_file_title,
        descriptionRes = R.string.main_action_folders_move_file_description,
        iconRes = R.drawable.main_action_folder_move_file,
        rememberLastAction = false
    ),

    @SerializedName("NOTE_NEW")
    NOTE_NEW(
        titleRes = R.string.main_action_notes_new_title,
        descriptionRes = R.string.main_action_notes_new_description,
        iconRes = R.drawable.main_action_notes_new
    ),

    @SerializedName("NOTE_BARCODE")
    NOTE_BARCODE(
        titleRes = R.string.main_action_notes_barcode_title,
        descriptionRes = R.string.main_action_notes_barcode_description,
        iconRes = R.drawable.main_action_notes_barcode
    ),

    @SerializedName("NOTE_CLIPBOARD")
    NOTE_CLIPBOARD(
        titleRes = R.string.main_action_notes_clipboard_title,
        descriptionRes = R.string.main_action_notes_clipboard_description,
        iconRes = R.drawable.main_action_notes_clipboard
    ),

    @SerializedName("NOTE_FILE")
    NOTE_FILE(
        titleRes = R.string.main_action_notes_import_title,
        descriptionRes = R.string.main_action_notes_import_description,
        iconRes = R.drawable.main_action_notes_import
    ),

    @SerializedName("FILE_SELECT")
    FILE_SELECT(
        titleRes = R.string.main_action_files_file_title,
        descriptionRes = R.string.main_action_files_file_description,
        iconRes = R.drawable.file_type_file
    ),

    @SerializedName("FILE_PHOTO")
    FILE_PHOTO(
        titleRes = R.string.main_action_files_photo_title,
        descriptionRes = R.string.main_action_files_photo_description,
        iconRes = R.drawable.file_type_photo
    ),

    @SerializedName("FILE_VIDEO")
    FILE_VIDEO(
        titleRes = R.string.main_action_files_record_title,
        descriptionRes = R.string.main_action_files_record_description,
        iconRes = R.drawable.file_type_record
    ),

    @SerializedName("TAG_NEW")
    TAG_NEW(
        titleRes = R.string.main_action_organize_tag_title,
        descriptionRes = R.string.main_action_organize_tag_description,
        iconRes = R.drawable.filter_tag_outline
    ),

    @SerializedName("FOLDER_NEW")
    FOLDER_NEW(
        titleRes = R.string.main_action_organize_folder_title,
        descriptionRes = R.string.main_action_organize_folder_description,
        iconRes = R.drawable.filter_group_folders
    ),

    @SerializedName("FILTER_NEW")
    FILTER_NEW(
        titleRes = R.string.main_action_organize_filter_title,
        descriptionRes = R.string.main_action_organize_filter_description,
        iconRes = R.drawable.action_filter_alt
    ),

    @SerializedName("SNIPPET_NEW")
    SNIPPET_NEW(
        titleRes = R.string.main_action_snippets_snippet_title,
        descriptionRes = R.string.main_action_snippets_snippet_description,
        iconRes = R.drawable.filter_snippet
    ),

    @SerializedName("SNIPPET_KIT_NEW")
    SNIPPET_KIT_NEW(
        titleRes = R.string.main_action_snippets_set_title,
        descriptionRes = R.string.main_action_snippets_set_description,
        iconRes = R.drawable.filter_snippet_set
    )

    ;

}