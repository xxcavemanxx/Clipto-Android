package clipto.store

data class StoreObjectSnapshot<T>(val value: T?) {
    fun isNull(): Boolean = value == null
    fun isNotNull(): Boolean = !isNull()
    fun requireValue(): T = value!!
}