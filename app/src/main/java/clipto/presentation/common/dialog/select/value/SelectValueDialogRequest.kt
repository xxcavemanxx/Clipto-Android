package clipto.presentation.common.dialog.select.value

import clipto.common.misc.AndroidUtils

data class SelectValueDialogRequest<T>(
    val id: Int = AndroidUtils.nextId(),
    val title: CharSequence,
    val single: Boolean = false,
    var options: List<Option<T>>,
    val onSelected: (options: List<T>) -> Unit,
    val withClearAll: Boolean = true,
    val withClearAllCustomTitleRes: Int = 0,
    val withClearAllAlternativeLogic: Boolean = false,
    val withClearAllCustomListener: ((options: List<T>) -> Boolean)? = null,
    val withImmediateNotify: Boolean = false,
    val withManualInput: Boolean = false,
    val onManualInput: ((request: SelectValueDialogRequest<T>) -> Unit)? = null,
) {

    var filteredByText: String? = null
    var requestRefresh: () -> Unit = {}

    data class Option<T>(
        val model: T,
        val checked: Boolean,
        val iconRes: Int? = null,
        val title: CharSequence?,
        val iconColor: Int? = null,
        val uid: String? = title?.toString()
    )
}