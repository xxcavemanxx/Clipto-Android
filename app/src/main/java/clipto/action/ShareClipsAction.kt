package clipto.action

import android.app.Application
import clipto.common.extensions.notNull
import clipto.common.misc.IntentUtils
import clipto.domain.Clip
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.IDynamicValuesRepository
import clipto.store.main.MainState
import clipto.utils.DomainUtils
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareClipsAction @Inject constructor(
    private val app: Application,
    private val mainState: MainState,
    private val dynamicValuesRepository: Lazy<IDynamicValuesRepository>
) : CompletableAction<ShareClipsAction.Context>() {

    override val name: String = "share_clips"

    fun execute(clips: Collection<Clip>, clearSelection: Boolean = true, callback: () -> Unit = {}) = execute(Context(clips, clearSelection), callback)

    override fun create(context: Context): Completable = Single
        .fromCallable {
            val clips = context.clips
            if (clips.size > 1) {
                val sb = StringBuilder()
                val size = clips.size
                clips
                    .mapNotNull { it.text }
                    .forEachIndexed { index, text ->
                        sb.append(text)
                        if (index < size) {
                            sb.append('\n')
                            sb.append('\n')
                        }
                    }
                Clip().apply {
                    text = sb.toString().trim()
                    fileIds = DomainUtils.getFileIds(clips)
                }
            } else {
                clips.first()
            }
        }
        // apply dynamic values
        .flatMap { clip ->
            val text = clip.text.notNull()
            if (DynamicField.isDynamic(text)) {
                val actionType = DynamicValueConfig.ActionType.SHARE
                dynamicValuesRepository.get().process(text, DynamicValueConfig(clip = clip, actionType = actionType))
                    .map { it.toString() }
                    .map { newText ->
                        Clip().apply {
                            this.textType = clip.textType
                            this.snippet = clip.snippet
                            this.tagIds = clip.tagIds
                            this.title = clip.title
                            this.fav = clip.fav
                            this.text = newText
                        }
                    }
            } else {
                Single.just(clip)
            }
        }
        .map { clip -> Pair(clip.text, clip.title) }
        .doOnSuccess { IntentUtils.share(app, it.first, it.second) }
        .doOnSubscribe { if (context.clearSelection) mainState.clearSelection() }
        .ignoreElement()

    data class Context(
        val clips: Collection<Clip>,
        val clearSelection: Boolean = true
    ) : ActionContext(showLoadingIndicator = true, withTimeout = false)

}