package clipto.store.clipboard.data

import clipto.domain.Clip
import clipto.extensions.getId

data class ClipboardNotification(val clip: Clip?) {
    val text: String? = clip?.text
    val id: Long = clip.getId()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipboardNotification

        if (text != other.text) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text?.hashCode() ?: 0
        result = 31 * result + id.hashCode()
        return result
    }
}