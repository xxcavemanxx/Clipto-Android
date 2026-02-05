package clipto.dao.objectbox.converter

import clipto.domain.PublicLink
import io.objectbox.converter.PropertyConverter

class PublicLinkConverter : PropertyConverter<PublicLink, String?> {
    override fun convertToDatabaseValue(from: PublicLink?): String? = PublicLink.toJson(from)
    override fun convertToEntityProperty(from: String?): PublicLink? = PublicLink.fromJson(from)
}