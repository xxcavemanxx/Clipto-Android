package clipto.dao.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import clipto.common.misc.GsonUtils
import clipto.dao.TxHelper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Clip
import clipto.domain.Filter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import javax.inject.Inject

class DriveSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val driveHelper: DriveServiceHelper,
    private val clipDao: ClipBoxDao,
    private val filterDao: FilterBoxDao,
    private val txHelper: TxHelper,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    companion object {
        private const val METADATA_FILE = "metadata.json"
    }

    override suspend fun doWork(): Result = try {
        sync()
        Result.success()
    } catch (e: Exception) {
        e.printStackTrace()
        Result.retry()
    }

    private fun sync() {
        val driveFiles = driveHelper.listFiles()
        
        // 1. Download or initialize metadata.json
        val remoteMetaId = driveFiles.find { it.name == METADATA_FILE }?.id
        val remoteMetadata: SyncMetadata = if (remoteMetaId != null) {
            val content = driveHelper.downloadFile(remoteMetaId)
            gson.fromJson(content, SyncMetadata::class.java)
        } else {
            SyncMetadata()
        }

        // 2. Fetch local items
        val localClips = clipDao.getAllClips()
        val localFilters = filterDao.getFilters().getSortedNamedFilters()

        val updatedRemoteItems = mutableMapOf<String, Long>()
        updatedRemoteItems.putAll(remoteMetadata.items)

        // 3. Process clips deletions and updates
        txHelper.inTx("DriveSync-Clips") {
            localClips.forEach { localClip ->
                if (localClip.firestoreId == null && localClip.snippetId == null) {
                    localClip.snippetId = clipto.common.misc.IdUtils.autoId()
                    clipDao.save(localClip)
                }
                val uid = localClip.firestoreId ?: localClip.snippetId ?: return@forEach
                val localTime = localClip.modifyDate?.time ?: localClip.createDate?.time ?: 0L
                val remoteTime = remoteMetadata.items[uid] ?: 0L

                if (localClip.isDeleted()) {
                    // Deleted locally: remove from Drive
                    if (remoteMetadata.items.containsKey(uid)) {
                        driveHelper.deleteFileByName("clip_$uid.json")
                        updatedRemoteItems.remove(uid)
                        remoteMetadata.tombstones.add(uid)
                    }
                } else if (remoteMetadata.tombstones.contains(uid)) {
                    // Deleted on remote: delete locally
                    clipDao.deleteAll(listOf(localClip))
                } else if (localTime > remoteTime) {
                    // Local is newer: upload JSON to Drive
                    val clipJson = gson.toJson(localClip, Clip::class.java)
                    driveHelper.uploadFile("clip_$uid.json", clipJson, "application/json")
                    updatedRemoteItems[uid] = localTime
                } else if (remoteTime > localTime) {
                    // Remote is newer: download and save locally
                    val fileId = driveFiles.find { it.name == "clip_$uid.json" }?.id
                    if (fileId != null) {
                        val remoteJson = driveHelper.downloadFile(fileId)
                        val remoteClip = gson.fromJson(remoteJson, ClipBox::class.java)
                        localClip.apply(remoteClip)
                        clipDao.save(localClip)
                    }
                }
            }

            // Sync remote new clips to local
            remoteMetadata.items.forEach { (uid, remoteTime) ->
                if (uid.startsWith("clip") || driveFiles.any { it.name == "clip_$uid.json" }) {
                    val local = localClips.find { it.firestoreId == uid || it.snippetId == uid }
                    if (local == null && !remoteMetadata.tombstones.contains(uid)) {
                        val fileId = driveFiles.find { it.name == "clip_$uid.json" }?.id
                        if (fileId != null) {
                            val remoteJson = driveHelper.downloadFile(fileId)
                            val remoteClip = gson.fromJson(remoteJson, ClipBox::class.java)
                            clipDao.save(remoteClip)
                        }
                    }
                }
            }
        }

        // 4. Process filters deletions and updates
        txHelper.inTx("DriveSync-Filters") {
            localFilters.forEach { localFilter ->
                val uid = localFilter.uid ?: return@forEach
                val localTime = localFilter.updateDate?.time ?: localFilter.createDate?.time ?: 0L
                val remoteTime = remoteMetadata.items[uid] ?: 0L

                if (remoteMetadata.tombstones.contains(uid)) {
                    // Deleted on remote: delete locally
                    filterDao.remove(localFilter.toBox())
                } else if (localTime > remoteTime) {
                    // Local is newer: upload JSON to Drive
                    val filterJson = gson.toJson(localFilter, Filter::class.java)
                    driveHelper.uploadFile("filter_$uid.json", filterJson, "application/json")
                    updatedRemoteItems[uid] = localTime
                } else if (remoteTime > localTime) {
                    // Remote is newer: download and save locally
                    val fileId = driveFiles.find { it.name == "filter_$uid.json" }?.id
                    if (fileId != null) {
                        val remoteJson = driveHelper.downloadFile(fileId)
                        val remoteFilter = gson.fromJson(remoteJson, FilterBox::class.java)
                        localFilter.apply(remoteFilter)
                        filterDao.save(localFilter.toBox())
                    }
                }
            }

            // Sync remote new filters to local
            remoteMetadata.items.forEach { (uid, remoteTime) ->
                if (uid.startsWith("filter") || driveFiles.any { it.name == "filter_$uid.json" }) {
                    val local = localFilters.find { it.uid == uid }
                    if (local == null && !remoteMetadata.tombstones.contains(uid)) {
                        val fileId = driveFiles.find { it.name == "filter_$uid.json" }?.id
                        if (fileId != null) {
                            val remoteJson = driveHelper.downloadFile(fileId)
                            val remoteFilter = gson.fromJson(remoteJson, FilterBox::class.java)
                            filterDao.save(remoteFilter)
                        }
                    }
                }
            }
        }

        // 5. Upload updated metadata.json
        remoteMetadata.items = updatedRemoteItems
        val updatedMetaJson = gson.toJson(remoteMetadata)
        driveHelper.uploadFile(METADATA_FILE, updatedMetaJson, "application/json")
    }

    private data class SyncMetadata(
        var items: Map<String, Long> = emptyMap(),
        val tombstones: MutableSet<String> = mutableSetOf()
    )
}
