package clipto.domain

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class TextType(val typeId: Int) : Serializable {

    @SerializedName("0")
    TEXT_PLAIN(0),

    @SerializedName("1")
    LINK(1) {
        override fun isClickable(): Boolean = true
    },

    @SerializedName("2")
    MARKDOWN(2) {
        override fun isPreviewable(): Boolean = true
    },

    @SerializedName("3")
    HTML(3) {
        override fun isPreviewable(): Boolean = true
    },

    @SerializedName("4")
    LINE_CLICKABLE(4) {
        override fun isClickable(): Boolean = true
    },

    @SerializedName("5")
    WORD_CLICKABLE(5) {
        override fun isClickable(): Boolean = true
    },

    @SerializedName("6")
    QRCODE(6),
    ;

    open fun isClickable(): Boolean = false
    open fun isPreviewable(): Boolean = false

    companion object {

        private val items = arrayOf(
            TEXT_PLAIN,
            LINK,
            MARKDOWN,
            HTML,
            LINE_CLICKABLE,
            WORD_CLICKABLE,
            QRCODE
        )

        fun byId(id: Int?): TextType = items.getOrElse(id ?: 0) { TEXT_PLAIN }
    }

}