package clipto.dynamic

import clipto.domain.Clip
import clipto.domain.TextType

data class DynamicValueConfig(
    val level: Int = 0,
    val clip: Clip? = null,
    val title: String? = clip?.title,
    val fastActionRequest:Boolean = false,
    val textType: TextType? = clip?.textType,
    val initialFields: List<FormField> = emptyList(),
    val actionType: ActionType = ActionType.CONFIRM) {

    fun isEditMode(): Boolean = actionType == ActionType.EDIT
    fun isPreviewMode(): Boolean = actionType == ActionType.PREVIEW

    enum class ActionType(val skipInput: Boolean = false) {
        CONFIRM,
        COPY,
        SHARE,
        INSERT,
        EDIT,
        PREVIEW(
                skipInput = true
        ),
        RECURSIVE(
                skipInput = true
        )
    }
}