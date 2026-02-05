package clipto.presentation.common.dialog.hint

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wb.clipboard.R
import java.io.Serializable

data class HintDialogData(
        val id: Long = System.currentTimeMillis(),
        val title: String,
        val description: String,
        val descriptionIsMarkdown:Boolean = false,
        @DrawableRes val iconRes: Int = R.drawable.ic_hint,
        val withDefaultIconColor:Boolean = true,
        @StringRes val actionRes: Int = R.string.button_got_it
) : Serializable