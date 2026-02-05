package clipto.store.main

enum class ScreenState(val id: String) {

    STATE_MAIN("main") {
        override fun hasMoreSettings() = true
        override fun hasSettings(): Boolean = true
    },

    STATE_MAIN_CONTEXT("context") {
        override fun isContextScreen(): Boolean = true
    },

    STATE_MAIN_CONTEXT_READONLY("context_readonly") {
        override fun isContextScreen(): Boolean = true
    },

    STATE_MAIN_CONTEXT_DELETED("context_deleted") {
        override fun isContextScreen(): Boolean = true
    }

    ;

    open fun hasSettings() = false
    open fun hasMoreSettings() = false
    open fun isContextScreen() = false

}