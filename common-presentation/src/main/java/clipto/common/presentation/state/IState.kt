package clipto.common.presentation.state

internal interface IState<S, V> {
    fun apply(state: S, view: V)
}