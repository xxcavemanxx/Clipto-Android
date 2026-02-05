package clipto.domain

interface IRune {

    fun getId(): String

    fun getIcon(): Int

    fun getColor(): String

    fun getTitle(): String

    fun isActive(): Boolean

    fun getDescription(): String

    fun isAvailable(): Boolean = true

    fun hasWarning(): Boolean = false

    fun isExpanded(): Boolean = false

    fun setExpanded(expanded: Boolean)

    companion object {
        const val RUNE_INSTANT_SYNC = "instant_sync"
        const val RUNE_PINCODE = "pincode"
        const val RUNE_THEME = "theme"
        const val RUNE_CLIPBOARD = "clipboard"
        const val RUNE_UNIVERSAL_CLIPBOARD = "universal_clipboard"
        const val RUNE_TEXPANDER = "texpander"
        const val RUNE_NOTIFICATION = "notification"
        const val RUNE_LINK_PREVIEW = "link_preview"
        const val RUNE_AUTO_SAVE = "auto_save"
        const val RUNE_REMEMBER_LAST_FILTER = "remember_last_filter"
        const val RUNE_SWIPE_ACTIONS = "swipe_actions"
        const val RUNE_FOCUS_ON_TITLE = "focus_on_title"
        const val RUNE_HIDE_ON_COPY = "hide_on_copy"
        const val RUNE_DOUBLE_CLICK_ACTIONS = "double_click_actions"
        const val RUNE_LOYALTY = "loyalty"
        const val RUNE_BACKUP_RESTORE = "backup_restore"
    }
}