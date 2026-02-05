package clipto.domain

import com.wb.clipboard.R
import java.io.Serializable

enum class FileType(
    val typeId: Int,
    val typeName: String,
    val roundIconRes: Int
) : Serializable {

    FILE(
        0,
        "file",
        R.drawable.file_type_file_rounded
    ),
    PHOTO(
        1,
        "photo",
        R.drawable.file_type_photo_rounded
    ),
    SEND(
        2,
        "send",
        R.drawable.file_type_file_rounded
    ),
    RECORD(
        3,
        "record",
        R.drawable.file_type_record_rounded
    ),
    BARCODE(
        4,
        "barcode",
        R.drawable.file_type_barcode_rounded
    ),
    FOLDER(
        5,
        "folder",
        R.drawable.file_type_folder_rounded
    )

    ;

    companion object {

        fun byId(id: Int?): FileType = when (id) {
            1 -> PHOTO
            2 -> SEND
            3 -> RECORD
            4 -> BARCODE
            5 -> FOLDER
            else -> FILE
        }
    }

}