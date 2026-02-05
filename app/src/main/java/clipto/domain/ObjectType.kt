package clipto.domain

import com.google.gson.annotations.SerializedName

enum class ObjectType(val id: Int) {

    @SerializedName("0")
    INTERNAL(0),

    @SerializedName("3")
    INTERNAL_GENERATED(3) {
        override fun getValue(): ObjectType = INTERNAL
    },

    @SerializedName("4")
    READONLY(4) {
        override fun getValue(): ObjectType = READONLY
    },

    @SerializedName("1")
    EXTERNAL_SNIPPET(1),

    @SerializedName("2")
    EXTERNAL_SNIPPET_KIT(2);

    open fun getValue(): ObjectType = this

    companion object {
        fun byId(id: Int?): ObjectType =
            when (id) {
                INTERNAL.id -> INTERNAL
                EXTERNAL_SNIPPET.id -> EXTERNAL_SNIPPET
                EXTERNAL_SNIPPET_KIT.id -> EXTERNAL_SNIPPET_KIT
                INTERNAL_GENERATED.id -> INTERNAL_GENERATED
                READONLY.id -> READONLY
                else -> INTERNAL
            }
    }

}