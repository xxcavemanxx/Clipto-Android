package clipto.repository

import clipto.api.IApi
import clipto.api.data.UpdateSnippetKitRequest
import clipto.common.extensions.toNullIfEmpty
import clipto.dao.objectbox.ClipBoxDao
import clipto.domain.*
import clipto.repository.data.SnippetKitData
import clipto.repository.data.SnippetKitUpdateData
import clipto.store.app.AppState
import clipto.store.app.TextLanguage
import clipto.store.user.UserState
import dagger.Lazy
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetRepository @Inject constructor(
    private val api: Lazy<IApi>,
    private val appState: AppState,
    private val userState: UserState,
    private val clipBoxDao: ClipBoxDao
) : CacheableRepository(), ISnippetRepository {

    override fun installKit(kit: SnippetKit): Single<Filter> = api.get()
        .installSnippetKit(kit)
        .toSingle()

    override fun update(kit: SnippetKit, data: SnippetKitUpdateData): Single<SnippetKit> = api.get()
        .updateSnippetKit(
            UpdateSnippetKitRequest(
                kit = kit,
                message = data.message.toNullIfEmpty(trim = true),
                country = data.country,
                language = data.language,
                categoryId = data.categoryId,
                status = data.status
            )
        )
        .toSingle()

    override fun getKits(category: SnippetKitCategory?): Single<List<SnippetKit>> = api.get().getSnippetKits(category).toSingle()
        .cached("getKits(${category?.id}_${userState.getUserId()})", CACHE_1_MINUTE) { !userState.isAdmin() }

    override fun getKit(id: String, force: Boolean): Single<SnippetKitData> = api.get().getSnippetKit(id).toSingle()
        .cached("getKit(${id}_${userState.getUserId()})", CACHE_1_MINUTE) { !force && !userState.isAdmin() }

    override fun getCategories(): Single<List<SnippetKitCategory>> = api.get().getSnippetKitCategories().toSingle()
        .cached("getSnippetKitCategories(${userState.getUserId()})", CACHE_5_MINUTES)

    override fun getSnippetDetails(snippet: Snippet): Single<SnippetDetails> = api.get().getSnippetDetails(snippet).toSingle()
        .cached("getSnippetDetails(${snippet.id})", CACHE_15_MINUTES)

    override fun discardKit(filter: Filter): Single<SnippetKit> = api.get().discardSnippetKit(filter).toSingle()

    override fun createLink(filter: Filter): Single<SnippetKit> = api.get().createSnippetKitLink(filter).toSingle()

    override fun removeLink(filter: Filter): Single<SnippetKit> = api.get().removeSnippetKitLink(filter).toSingle()

    override fun publishKit(filter: Filter): Single<SnippetKit> = Single
        .fromCallable {
            val text = StringBuilder()
            val snapshot = Filter.Snapshot().copy(filter)
            clipBoxDao.getFiltered(snapshot).find()
                .forEach { clip ->
                    clip.text?.let { text.append(it).appendLine() }
                    clip.title?.let { text.append(it).appendLine() }
                    clip.description?.let { text.append(it).appendLine() }
                }
            filter.name?.let { text.append(it).appendLine() }
            filter.description?.let { text.append(it).appendLine() }
            text.toString()
        }
        .flatMap { text -> appState.getTextLanguage(text) }
        .map { lang -> Locale(lang, appState.getSelectedLocale().country) }
        .flatMapMaybe { api.get().publishSnippetKit(filter, it.language, it.country) }
        .toSingle()

    override fun getLanguages(kit: SnippetKit): Single<List<TextLanguage>> = Single
        .fromCallable {
            val text = StringBuilder()
            kit.getSortedSnippets().forEach { snippet ->
                snippet.text.let { text.append(it).appendLine() }
                snippet.title?.let { text.append(it).appendLine() }
                snippet.description?.let { text.append(it).appendLine() }
            }
            kit.name.let { text.append(it).appendLine() }
            kit.description?.let { text.append(it).appendLine() }
            text.toString()
        }
        .flatMap { appState.getTextPossibleLanguages(it) }

}