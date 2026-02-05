package clipto.utils

import clipto.domain.AttributedObject
import clipto.domain.Clip
import clipto.domain.getTagIds

object DomainUtils {

    fun getTitle(objects: Collection<AttributedObject>): String? {
        return objects.firstOrNull { it.title != null }?.title
    }

    fun getTags(objects: Collection<AttributedObject>): List<String> {
        val tagsSet = linkedSetOf<String>()
        objects.forEach { tagsSet.addAll(it.getTagIds()) }
        return tagsSet.toList()
    }

    fun getCommonFolder(objects: Collection<AttributedObject>): String? {
        val folderId = objects.firstOrNull()?.folderId
        return folderId?.takeIf { !objects.any { it.folderId != folderId } }
    }

    fun getCommonTagIds(objects: Collection<AttributedObject>): List<String> {
        var commonTags: Collection<String> = objects.firstOrNull().getTagIds()
        objects.forEachIndexed { index, clip ->
            if (index > 0) {
                val clipTags = clip.getTagIds()
                commonTags = commonTags.intersect(clipTags)
            }
        }
        return commonTags.distinct()
    }

    fun getFileIds(clips: Collection<Clip>): List<String> {
        val fileIds = linkedSetOf<String>()
        clips.forEach { fileIds.addAll(it.fileIds) }
        return fileIds.toList()
    }

    fun getText(clips: Collection<Clip>, separator: String): String {
        return clips.mapNotNull { it.text }.joinToString(separator).trim()
    }

}