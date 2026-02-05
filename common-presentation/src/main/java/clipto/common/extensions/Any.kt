package clipto.common.extensions

fun <T> threadLocal(instance: () -> T): ThreadLocal<T> =
    object : ThreadLocal<T>() {
        override fun initialValue(): T = instance.invoke()
    }

fun <T> ThreadLocal<T>.getNotNull() = this.get()!!

const val ComparatorNullCheck: Int = 100

fun <T> reversed(comparator: Comparator<T>, nullsLast: Boolean = false) = Comparator<T> { o1, o2 ->
    val res = comparator.compare(o2, o1)
    when {
        res == ComparatorNullCheck && nullsLast -> -ComparatorNullCheck
        res == -ComparatorNullCheck && nullsLast -> ComparatorNullCheck
        else -> res
    }
}

fun Any?.castToListOfStrings(): List<String> {
    if (this is List<*>) {
        return mapNotNull { it.toString() }
    }
    return emptyList()
}

fun Any?.castToListOfInts(): List<Int> {
    if (this is List<*>) {
        return mapNotNull { if (it is Number) it.toInt() else null }
    }
    return emptyList()
}

fun Any?.castToListOfMaps(): List<Map<String, Any?>> {
    if (this is List<*>) {
        return filterIsInstance(Map::class.java)
            .map { map -> map.mapKeys { it.key.toString() } }
    }
    return emptyList()
}

fun Any?.castToLong(): Long? {
    if (this is Number) {
        return toLong()
    }
    if (this is String) {
        return toLongOrNull()
    }
    return this?.toString()?.toLongOrNull()
}

fun Any?.castToInt(): Int? {
    if (this is Number) {
        return toInt()
    }
    if (this is String) {
        return toIntOrNull()
    }
    return this?.toString()?.toIntOrNull()
}

fun Any?.castToBoolean(): Boolean? {
    if (this is Boolean) {
        return this
    }
    if (this is String) {
        return toBoolean()
    }
    return this?.toString()?.toBoolean()
}


fun Any?.castToMap(): Map<String, Any?>? {
    if (this is Map<*, *>) {
        return this.mapKeys { it.key.toString() }
    }
    return null
}