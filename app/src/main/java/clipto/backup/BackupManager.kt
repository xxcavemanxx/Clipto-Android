package clipto.backup

import androidx.fragment.app.FragmentActivity
import clipto.analytics.Analytics
import clipto.backup.processor.*
import clipto.common.extensions.withFile
import clipto.common.extensions.withNewFile
import clipto.common.extensions.withPermissions
import clipto.common.misc.FormatUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.dao.objectbox.ClipBoxDao
import clipto.domain.Clip
import clipto.presentation.common.dialog.DialogState
import clipto.repository.IClipRepository
import clipto.store.app.AppState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class BackupManager @Inject constructor(
    private val appState: AppState,
    private val clipBoxDao: ClipBoxDao,
    private val dialogState: DialogState,
    private val clipRepository: IClipRepository
) : RxViewModel(appState.app) {

    @Inject
    lateinit var cliptoProcessor: CliptoProcessor

    @Inject
    lateinit var clipperProcessor: ClipperProcessor

    @Inject
    lateinit var simpleNoteProcessor: SimpleNoteProcessor

    @Inject
    lateinit var cliptoOldProcessor: CliptoOldProcessor

    @Inject
    lateinit var clipStackProcessor: ClipStackProcessor

    @Inject
    lateinit var clipboardManagerProcessor: ClipboardManagerProcessor

    @Inject
    lateinit var keepProcessor: KeepProcessor

    fun backup(activity: FragmentActivity, types: List<BackupItemType>) {
        val contentResolver = activity.contentResolver
        val fileName = "${FormatUtils.buildUniqueName(BACKUP_NAME)}.json"
        activity.withNewFile("${FormatUtils.buildUniqueName(BACKUP_NAME)}.json", persistable = false) { uri ->
            appState.setLoadingState()
            onBackground {
                try {
                    val clips = clipBoxDao.getAllClips()
                    val stats = cliptoProcessor.backup(contentResolver, uri, clips, types)
                    Analytics.onBackupAll()
                    showBackupStats(string(R.string.fast_actions_export_to_file_success, fileName), stats)
                } catch (e: Exception) {
                    Analytics.onError("backup_all_error", e)
                    dialogState.showAlert(string(R.string.backup_fail_title), string(R.string.backup_fail_message, e.message))
                } finally {
                    appState.setLoadedState()
                }
            }
        }
    }

    fun backupNotes(activity: FragmentActivity, list: List<Clip>) {
        val contentResolver = activity.contentResolver
        val fileName = "${FormatUtils.buildUniqueName(BACKUP_NAME)}.json"
        activity.withNewFile(fileName, persistable = false) { uri ->
            appState.setLoadingState()
            onBackground {
                try {
                    val stats = cliptoProcessor.backup(contentResolver, uri, list, listOf(BackupItemType.NOTES))
                    Analytics.onBackupSelected()
                    showBackupStats(string(R.string.fast_actions_export_to_file_success, fileName), stats)
                } catch (e: Exception) {
                    Analytics.onError("backup_selected_error", e)
                    dialogState.showAlert(string(R.string.backup_fail_title), string(R.string.backup_fail_message, e.message))
                } finally {
                    appState.setLoadedState()
                }
            }
        }
    }

    private fun showBackupStats(title: String, stats: IBackupProcessor.BackupStats?) {
        if (stats != null) {
            val description = StringBuilder()
                .append(string(BackupItemType.SETTINGS.titleRes)).append(": ").append(stats.settings)
                .appendLine()
                .append(string(BackupItemType.NOTES.titleRes)).append(": ").append(stats.notes)
                .appendLine()
                .append(string(BackupItemType.TAGS.titleRes)).append(": ").append(stats.tags)
                .appendLine()
                .append(string(BackupItemType.FILTERS.titleRes)).append(": ").append(stats.filters)
                .appendLine()
                .append(string(BackupItemType.SNIPPET_KITS.titleRes)).append(": ").append(stats.snippetKits)
                .toString()

            dialogState.showAlert(title, description)
        }
    }

    fun restore(activity: FragmentActivity) {
        activity.withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) {
            val contentResolver = activity.contentResolver
            activity.withFile(BACKUP_MEDIA_TYPE, persistable = false) { uri ->
                appState.setLoadingState()
                onBackground {
                    try {
                        val processors = listOf(
                            cliptoProcessor,
                            clipperProcessor,
                            simpleNoteProcessor,
                            cliptoOldProcessor,
                            clipStackProcessor,
                            clipboardManagerProcessor,
                            keepProcessor
                        )
                        var stats: IBackupProcessor.BackupStats? = null
                        val restoredClips = mutableListOf<Clip>()
                        for (processor in processors) {
                            try {
                                stats = processor.restore(contentResolver, uri)
                                restoredClips.addAll(stats.clips)
                                if (stats.isNotEmpty()) {
                                    break
                                }
                            } catch (th: Throwable) {
                                //
                            }
                        }
                        if (stats != null) {
                            if (stats.isNotEmpty()) {
                                clipRepository.syncAll(restoredClips)
                            }
                            if (stats.settings) {
                                appState.requestRestart()
                            }
                            showBackupStats(string(R.string.restore_success_title), stats)
                        }
                    } catch (e: Exception) {
                        Analytics.onError("restore_error", e)
                        dialogState.showAlert(string(R.string.restore_fail_title), string(R.string.restore_fail_message, e.message))
                    } finally {
                        appState.setLoadedState()
                    }
                }
            }
        }
    }

    companion object {
        const val BACKUP_NAME = "clipto_backup"
        const val BACKUP_MEDIA_TYPE = "*/*"
    }

}