package clipto.domain

import com.google.gson.annotations.SerializedName

enum class SwipeAction(val id: Int) {

    @SerializedName("1")
    STAR(1),

    @SerializedName("4")
    TAG(4),

    @SerializedName("3")
    COPY(3),

    @SerializedName("0")
    DELETE(0),

    @SerializedName("2")
    NONE(2)
    ;

    companion object {
        fun byId(id: Int?): SwipeAction = when (id) {
            DELETE.id -> DELETE
            STAR.id -> STAR
            COPY.id -> COPY
            TAG.id -> TAG
            NONE.id -> NONE
            else -> DELETE
        }
    }
}