package clipto.dao.objectbox.converter

import clipto.domain.FileType
import io.objectbox.converter.PropertyConverter

class FileTypesConverter : PropertyConverter<List<FileType>, String?> {
    override fun convertToEntityProperty(from: String?): List<FileType> =
        from?.split("|")?.filter { it.isNotBlank() }?.map { FileType.byId(it.toIntOrNull()) }?.distinct() ?: emptyList()

    override fun convertToDatabaseValue(from: List<FileType>?): String? =
        from?.map { it.typeId }?.joinToString(separator = "|", prefix = "|", postfix = "|")
}