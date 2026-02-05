package clipto.action.intent.provider

import android.content.Context
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.common.extensions.disposeSilently
import clipto.domain.Clip
import clipto.extensions.from
import clipto.repository.IClipRepository
import clipto.store.clipboard.IClipboardStateManager
import dagger.Lazy
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CopyProvider @Inject constructor(
    private val clipRepository: Lazy<IClipRepository>,
    private val clipboardStateManager: IClipboardStateManager
) : IntentActionProvider<CopyProvider.Action>(
    actionClass = Action::class.java,
    actionId = "copy_provider"
) {

    private var disposable: Disposable? = null

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        disposable.disposeSilently()
        disposable = clipRepository.get().getByText(action.text, action.id)
            .subscribeOn(clipboardState.getBackgroundScheduler())
            .subscribe { clip, _ ->
                val newClip = clip ?: Clip.from(action.text, tracked = true)
                clipboardStateManager.onCopy(
                    clip = newClip,
                    clearSelection = false,
                    saveCopied = true,
                    callback = callback
                )
            }
        closeSystemDialogs()
    }

    data class Action(val text: String, val id: Long) : IntentAction {
        override fun getSize(): Int = text.length
    }

}