package clipto.domain

import com.google.gson.annotations.SerializedName

enum class NotificationStyle(val id: Int) {

    @SerializedName("-1")
    NULL(-1) {
        override fun get(): NotificationStyle = DEFAULT
    },

    @SerializedName("0")
    DEFAULT(0),

    @SerializedName("1")
    CONTROLS(1),

    @SerializedName("2")
    HISTORY(2),

    @SerializedName("3")
    ACTIONS(3)
    ;

    open fun get(): NotificationStyle = this

    companion object {
        fun byId(id: Int?): NotificationStyle = when (id) {
            DEFAULT.id -> DEFAULT
            CONTROLS.id -> CONTROLS
            HISTORY.id -> HISTORY
            ACTIONS.id -> ACTIONS
            NULL.id -> NULL
            else -> NULL
        }
    }
}