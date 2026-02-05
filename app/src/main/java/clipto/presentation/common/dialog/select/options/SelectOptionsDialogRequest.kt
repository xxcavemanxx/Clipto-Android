package clipto.presentation.common.dialog.select.options

import clipto.common.misc.AndroidUtils

data class SelectOptionsDialogRequest(
        val id: Int = AndroidUtils.nextId(),
        val title: CharSequence,
        var options: List<Option>,
        val enabled: Boolean = true,
        val withTitle:Boolean = true,
        val onSelected: (options: List<Option>) -> Unit
) {
    data class Option(
            var value: String? = null,
            var title: String? = null,
            var editMode: Boolean = false
    )
}