package clipto.presentation.common.dialog.confirm

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import clipto.common.misc.AndroidUtils
import com.wb.clipboard.R

data class ConfirmDialogData(
        val id: Int = AndroidUtils.nextId(),
        val title: CharSequence,
        val description: CharSequence,
        val descriptionIsMarkdown:Boolean = false,
        @DrawableRes val iconRes: Int = R.drawable.ic_hint,
        @StringRes val confirmActionTextRes: Int = R.string.button_continue,
        @StringRes val cancelActionTextRes: Int = R.string.menu_cancel,
        val autoConfirm: () -> Boolean = { false },
        val onClosed: (proceeded: Boolean) -> Unit = {},
        val onCanceled: () -> Unit = {},
        val onConfirmed: () -> Unit) {
    internal var proceeded: Boolean = false
}