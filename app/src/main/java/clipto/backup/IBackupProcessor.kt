package clipto.backup

import android.content.ContentResolver
import android.net.Uri
import clipto.domain.Clip

interface IBackupProcessor {

    fun backup(
            contentResolver: ContentResolver,
            uri: Uri,
            list: List<Clip>,
            types: List<BackupItemType>
    ): BackupStats? = null

    fun restore(contentResolver: ContentResolver, uri: Uri): BackupStats

    data class BackupStats(
            val notes: Int = 0,
            val tags: Int = 0,
            val filters: Int = 0,
            val snippetKits: Int = 0,
            val settings: Boolean = false,
            val clips:List<Clip> = emptyList()
    ) {
        fun isNotEmpty(): Boolean = notes > 0 || tags > 0 || filters > 0 || snippetKits > 0 || settings || clips.isNotEmpty()
    }

}