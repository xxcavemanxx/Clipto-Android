package clipto.presentation.usecases

import android.Manifest
import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.getNavController
import clipto.common.extensions.withFile
import clipto.common.extensions.withPermissions
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.IdUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.Clip
import clipto.domain.FocusMode
import clipto.extensions.from
import clipto.presentation.clip.details.ClipDetailsFragment
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.usecases.data.ShowNoteDetailsRequest
import clipto.store.app.AppState
import clipto.store.clip.ClipState
import clipto.utils.DomainUtils
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject

@ActivityRetainedScoped
class NoteUseCases @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val clipState: ClipState,
    private val appConfig: IAppConfig,
    private val dialogState: DialogState,
    private val clipDetailsState: ClipDetailsState
) : RxViewModel(app) {

    fun onNewNote() {
        clipState.setNewState()
        appState.requestNavigateTo(R.id.action_clip)
    }

    fun onNewSnippet() {
        val clip = clipState.getDefaultNewClip()
        clip.snippetId = IdUtils.autoId()
        clipState.setNewState(clip)
        appState.requestNavigateTo(R.id.action_clip)
    }

    fun onNewNoteFromFile(act: FragmentActivity) {
        act.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
            val contentResolver = act.contentResolver
            act.withFile(persistable = false) { uri ->
                appState.setLoadingState()
                onBackground {
                    runCatching {
                        contentResolver.openFileDescriptor(uri, "r")?.use {
                            if (it.statSize <= appConfig.noteMaxSizeInKb() * 1000) {
                                FileInputStream(it.fileDescriptor).use { stream ->
                                    val text = String(stream.readBytes())
                                    clipState.setEditState(Clip.from(text), FocusMode.NONE)
                                    appState.requestNavigateTo(R.id.action_clip)
                                }
                            } else {
                                dialogState.showError(
                                    string(R.string.main_action_notes_import_title),
                                    string(R.string.main_action_notes_import_error, appConfig.noteMaxSizeInKb())
                                )
                            }
                        }
                    }
                    appState.setLoadedState()
                }
            }
        }
    }

    fun onNewNote(fragment: Fragment) {
        clipState.setNewState()
        val navController = fragment.getNavController()
        if (navController.currentDestination?.id != R.id.fragment_clip) {
            appState.requestNavigateTo(R.id.action_clip)
        }
    }

    fun onViewNote(clip: Clip) {
        clipState.setViewState(clip)
        appState.requestNavigateTo(R.id.action_clip)
    }

    fun onMergeNotes(clips: Collection<Clip>) {
        val clip = ClipBox()
        clip.text = DomainUtils.getText(clips, appState.getSettings().textSeparator)
        clip.fileIds = DomainUtils.getFileIds(clips)
        clip.title = DomainUtils.getTitle(clips)
        clip.tagIds = DomainUtils.getTags(clips)
        clip.sourceClips = clips.toList()
        clip.createDate = Date()

        val subTitle = quantityString(R.plurals.main_toolbar_notes, clips.size, clips.size)
        val title = "${string(R.string.menu_merge)} ($subTitle)"

        clipState.setEditState(clip, FocusMode.NONE, title)
        appState.requestNavigateTo(R.id.action_clip)
    }

    fun onEditNote(fragment: Fragment, clip: Clip) {
        clipState.setEditState(clip, FocusMode.NONE)
        val navController = fragment.getNavController()
        if (navController.currentDestination?.id != R.id.fragment_clip) {
            appState.requestNavigateTo(R.id.action_clip)
        }
    }

    fun onShowNoteDetails(fragment: Fragment, request: ShowNoteDetailsRequest) {
        val activity = fragment.activity
        activity?.withSafeFragmentManager()?.let {
            clipDetailsState.unbind(activity)

            clipDetailsState.attachment.getLiveData().observe(activity) { attachment ->
                attachment?.let(request.onAttachment)
            }

            clipDetailsState.clipDetails.getLiveData().observe(activity) { details ->
                details?.let(request.onChanged)
            }

            clipDetailsState.dynamicValue.getLiveData().observe(activity) { value ->
                value?.let(request.onValue)
            }

            clipDetailsState.fastAction.getLiveData().observe(activity) { fastAction ->
                fastAction?.let(request.onFastAction)
            }

            clipDetailsState.openedClip.setValue(request.clip, force = true)

            ClipDetailsFragment().show(it, ClipDetailsFragment.TAG)
        }
    }

}