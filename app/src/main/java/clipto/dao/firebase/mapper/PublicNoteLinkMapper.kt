package clipto.dao.firebase.mapper

import clipto.dao.firebase.FirebaseDaoHelper
import clipto.domain.PublicLink

object PublicNoteLinkMapper {

    fun toMap(from: PublicLink?): Map<String, Any?> {
        if (from == null) {
            return emptyMap()
        }
        return mapOf(
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_AT_DATE to from.postponeAtDate,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_IN_MILLIS to from.postponeInMillis,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_AT_DATE to from.expiresAtDate,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_IN_MILLIS to from.expiresInMillis,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_LINK to from.link,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_PASSWORD_CLUE to from.passwordClue,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_OPENED_TIMES to from.openedTimes,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_UNAVAILABLE to from.unavailable,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_ONETIME to from.oneTimeOpening,
            FirebaseDaoHelper.ATTR_PUBLIC_NOTE_LOCKED to from.locked
        )
    }

    fun fromMap(from: Map<*, Any?>?): PublicLink? {
        if (from.isNullOrEmpty()) {
            return null
        }
        return PublicLink(
            postponeAtDate = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_AT_DATE]?.let { DateMapper.toDate(it) },
            postponeInMillis = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_IN_MILLIS]?.let { it as Number }?.toLong(),
            expiresAtDate = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_AT_DATE]?.let { DateMapper.toDate(it) },
            expiresInMillis = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_IN_MILLIS]?.let { it as Number }?.toLong(),
            link = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_LINK]?.let { it as String },
            passwordClue = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_PASSWORD_CLUE]?.let { it as String },
            openedTimes = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_OPENED_TIMES]?.let { it as Number }?.toInt(),
            unavailable = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_UNAVAILABLE]?.let { it as Boolean } ?: false,
            oneTimeOpening = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_ONETIME]?.let { it as Boolean } ?: false,
            locked = from[FirebaseDaoHelper.ATTR_PUBLIC_NOTE_LOCKED]?.let { it as Boolean } ?: false
        )
    }

}