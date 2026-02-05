package clipto.extensions

import clipto.dynamic.DynamicValueConfig
import com.wb.clipboard.R

fun DynamicValueConfig.ActionType.getActionLabelRes(): Int {
    return when (this) {
        DynamicValueConfig.ActionType.CONFIRM -> R.string.button_confirm
        DynamicValueConfig.ActionType.SHARE -> R.string.menu_share
        DynamicValueConfig.ActionType.COPY -> R.string.menu_copy
        DynamicValueConfig.ActionType.INSERT -> R.string.button_insert
        else -> R.string.button_confirm
    }
}