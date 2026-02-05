package clipto.domain.factory

import clipto.dao.objectbox.model.FilterBox
import clipto.domain.FileRef
import clipto.domain.Filter
import clipto.domain.Settings
import clipto.extensions.normalize

object FilterFactory {

    fun createViewFolder(folder: FileRef, template: Filter): Filter {
        val filter = FilterBox().withFilter(template)
        filter.uid = folder.getUid()
        filter.name = folder.title
        filter.color = folder.color
        filter.type = Filter.Type.FOLDER
        filter.folderId = folder.firestoreId
        filter.description = folder.description
        return filter.normalize()
    }

}