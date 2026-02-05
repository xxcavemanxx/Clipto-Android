package clipto.action

open class ActionContext(
        val showLoadingIndicator:Boolean = false,
        val disposeRunning:Boolean = true,
        val withTimeout:Boolean = true
) {

    companion object {
        val EMPTY = ActionContext()
        val DETATCHED = ActionContext(withTimeout = false)
    }
}