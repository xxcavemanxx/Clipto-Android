package clipto.common.extensions

import clipto.common.misc.GsonUtils
import java.util.*

fun Date?.toFormattedString(): String? = GsonUtils.formatDate(this)
fun String?.toDate(): Date? = GsonUtils.parseDate(this)