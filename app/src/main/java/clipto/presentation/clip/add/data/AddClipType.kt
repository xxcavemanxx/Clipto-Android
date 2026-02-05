package clipto.presentation.clip.add.data

import androidx.annotation.StringRes
import clipto.dao.sharedprefs.data.AddClipScreenData
import com.google.gson.annotations.SerializedName
import com.wb.clipboard.R
import java.io.Serializable

enum class AddClipType(
    val index: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) : Serializable {

    @SerializedName("EDIT")
    EDIT(
        index = 0,
        titleRes = R.string.clip_action_new_edit,
        descriptionRes = R.string.clip_action_new_edit_description
    ) {
        override fun canShowHint(hints: AddClipScreenData): Boolean = !hints.hideEdit
        override fun hideHint(hints: AddClipScreenData) = hints.copy(hideEdit = true)
    },

    @SerializedName("ACTION")
    ACTION(
        index = 1,
        titleRes = R.string.clip_action_new_action,
        descriptionRes = R.string.clip_action_new_action_description,
    ) {
        override fun canShowHint(hints: AddClipScreenData): Boolean = !hints.hideActions
        override fun hideHint(hints: AddClipScreenData) = hints.copy(hideActions = true)
    },

    @SerializedName("INSERT")
    INSERT(
        index = 2,
        titleRes = R.string.clip_action_new_insert,
        descriptionRes = R.string.clip_action_new_insert_description
    ) {
        override fun canShowHint(hints: AddClipScreenData): Boolean = !hints.hideInsert
        override fun hideHint(hints: AddClipScreenData) = hints.copy(hideInsert = true)
    };

    abstract fun canShowHint(hints: AddClipScreenData): Boolean
    abstract fun hideHint(hints: AddClipScreenData): AddClipScreenData

}