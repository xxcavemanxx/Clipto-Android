package clipto.domain

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import clipto.common.misc.GsonUtils
import java.util.*

data class PublicLink(
    @SerializedName("pm") val postponeInMillis: Long? = null,
    @SerializedName("pd") val postponeAtDate: Date? = null,
    @SerializedName("em") val expiresInMillis: Long? = null,
    @SerializedName("ed") val expiresAtDate: Date? = null,
    @SerializedName("l") val link: String? = null,
    @SerializedName("ua") val unavailable: Boolean = false,
    @SerializedName("pc") val passwordClue: String? = null,
    @SerializedName("ot") val openedTimes: Int? = null,
    @SerializedName("oto") val oneTimeOpening: Boolean = false,
    @SerializedName("ld") val locked: Boolean = false,
    @Expose val password: String? = null
) {

    fun isPostponed() = postponeAtDate != null || postponeInMillis != null

    fun canBeExpired() = expiresAtDate != null || expiresInMillis != null

    fun isOneTime() = oneTimeOpening

    fun isLocked() = locked

    companion object {
        fun fromJson(json: String?): PublicLink? {
            if (json == null) {
                return null
            }
            return try {
                GsonUtils.get().fromJson(json, PublicLink::class.java)
            } catch (th: Throwable) {
                null
            }
        }

        fun toJson(from: PublicLink?): String? {
            if (from == null) {
                return null
            }
            return try {
                GsonUtils.get().toJson(from)
            } catch (th: Throwable) {
                th.printStackTrace()
                null
            }
        }
    }
}