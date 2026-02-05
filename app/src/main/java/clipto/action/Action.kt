package clipto.action

import clipto.common.logging.L

abstract class Action {

    protected abstract val name: String

    protected fun log(message: String, vararg args: Any?) = L.log(this, message, *args)

}