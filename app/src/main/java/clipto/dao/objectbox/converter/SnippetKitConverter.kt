package clipto.dao.objectbox.converter

import clipto.common.misc.GsonUtils
import clipto.domain.SnippetKit
import io.objectbox.converter.PropertyConverter

class SnippetKitConverter : PropertyConverter<SnippetKit, String> {
    override fun convertToEntityProperty(databaseValue: String?): SnippetKit? = GsonUtils.toObjectSilent(databaseValue, SnippetKit::class.java)
    override fun convertToDatabaseValue(entityProperty: SnippetKit?): String? = GsonUtils.toStringSilent(entityProperty?.copy(snippets = emptyList()))
}