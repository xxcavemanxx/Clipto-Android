package clipto.dao.objectbox.converter

import clipto.domain.FileType
import io.objectbox.converter.PropertyConverter

class FileTypeConverter : PropertyConverter<FileType, Int> {
    override fun convertToEntityProperty(from: Int?): FileType = FileType.byId(from)
    override fun convertToDatabaseValue(from: FileType): Int = from.typeId
}