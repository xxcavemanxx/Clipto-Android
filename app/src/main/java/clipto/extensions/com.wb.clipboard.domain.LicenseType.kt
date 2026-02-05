package clipto.extensions

import com.wb.clipboard.R
import clipto.domain.LicenseType

fun LicenseType.getTitleRes() = when (this) {
    LicenseType.SUBSCRIPTION -> R.string.account_plan_subscription
    LicenseType.CONTRIBUTOR -> R.string.account_plan_contributor
    LicenseType.PERSONAL -> R.string.account_plan_personal
    LicenseType.NONE -> R.string.account_plan_free
}