package clipto.action.intent.provider

import android.content.Context
import android.content.Intent
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.analytics.Analytics
import clipto.common.extensions.disposeSilently
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.ObjectType
import clipto.extensions.getId
import clipto.extensions.toClip
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.contextactions.ContextActionsActivity
import clipto.repository.IClipRepository
import clipto.store.clipboard.IClipboardStateManager
import com.wb.clipboard.R
import dagger.Lazy
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowClipProvider @Inject constructor(
    private val clipboardStateManager: IClipboardStateManager,
    private val clipRepository: Lazy<IClipRepository>
) : IntentActionProvider<ShowClipProvider.Action>(
    actionClass = Action::class.java,
    actionId = "show_clip_provider"
) {

    private var disposable: Disposable? = null

    override fun createNewIntent(action: Action): Intent {
        val intent = Intent(app, ContextActionsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        context as ContextActionsActivity
        val viewModel = context.viewModel
        val clip = clipboardState.refreshClipboard(true)?.toClip(app, withMetadata = true)
        val text = clip?.text
        when {
            text.isNullOrBlank() -> {
                viewModel.showToast(context.getString(R.string.notification_title_empty))
                context.onComplete()
            }
            clipboardState.canEmulateCopyAction() -> {
                disposable.disposeSilently()
                disposable = clipRepository.get().getByText(clip.text, clip.getId())
                    .subscribeOn(appState.getBackgroundScheduler())
                    .observeOn(appState.getViewScheduler())
                    .subscribe(
                        {
                            AddClipFragment.show(
                                context, AddClipRequest(
                                    id = it.getId(),
                                    text = it.text
                                )
                            )
                        },
                        {
                            val newClip = ClipBox().apply {
                                this.objectType = ObjectType.INTERNAL_GENERATED
                                this.tracked = action.tracked
                                this.text = text
                            }
                            Analytics.screenClipPaste()
                            clipboardStateManager.onCopy(
                                clip = newClip,
                                clearSelection = false,
                                saveCopied = true,
                                withToast = false,
                                callback = callback
                            )
                        })
            }
            else -> {
                Analytics.screenClipPaste()
                disposable.disposeSilently()
                disposable = clipRepository.get().getByText(clip.text, clip.getId())
                    .subscribeOn(appState.getBackgroundScheduler())
                    .observeOn(appState.getViewScheduler())
                    .subscribe(
                        {
                            AddClipFragment.show(
                                context, AddClipRequest(
                                    id = it.getId(),
                                    text = it.text
                                )
                            )
                        },
                        {
                            AddClipFragment.show(
                                context, AddClipRequest(
                                    tracked = action.tracked,
                                    text = text
                                )
                            )
                        }
                    )
            }
        }
    }

    class Action(val tracked: Boolean = true) : IntentAction

}