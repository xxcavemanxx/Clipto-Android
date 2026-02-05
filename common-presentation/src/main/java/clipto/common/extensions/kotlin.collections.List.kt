package clipto.common.extensions

import kotlin.math.max

fun <E> List<E>.subListExt(start: Int, end: Int, incrementMax: Int = 0): List<E> {
    val min = max(0, minOf(start, end))
    val max = max(0, maxOf(start, end))
    return subList(min, max + incrementMax)
}

