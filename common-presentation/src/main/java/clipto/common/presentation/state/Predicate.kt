package clipto.common.presentation.state

fun interface Predicate<S> {
    fun test(state: S): Boolean
}