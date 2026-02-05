package clipto.dao.objectbox.converter

import clipto.domain.ClipFile
import io.objectbox.converter.PropertyConverter

@Deprecated("need to be removed")
class ClipFilesConverter : PropertyConverter<List<ClipFile.Meta>, String?> {
    override fun convertToDatabaseValue(from: List<ClipFile.Meta>?): String? = ClipFile.Meta.toJson(from)
    override fun convertToEntityProperty(from: String?): List<ClipFile.Meta> = ClipFile.Meta.fromJson(from)
}