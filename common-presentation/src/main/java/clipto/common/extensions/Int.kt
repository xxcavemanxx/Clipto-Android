package clipto.common.extensions

import clipto.common.misc.Units

fun Int.doToPx() = Units.DP.toPx(toFloat()).toInt()