package clipto.domain

open class ClientSession {

    open var user: User = User.NULL

    fun isAuthorized() = user.isAuthorized()

    companion object {
        const val TEXT_SIZE_DEFAULT = 16
        const val TEXT_SIZE_MIN = 8
        const val TEXT_SIZE_MAX = 72

        const val TEXT_LINES_DEFAULT = 3
        const val TEXT_LINES_MIN = 1
        const val TEXT_LINES_MAX = 20

        const val UNLIMITED = -1

        const val SEPARATOR_NEW_LINE = "\n\n"
        const val SEPARATOR_SPACE = " "
        const val SEPARATOR_MD = "\n***\n"
    }
}