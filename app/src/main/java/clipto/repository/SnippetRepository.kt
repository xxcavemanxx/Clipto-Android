package clipto.repository

import clipto.domain.*
import clipto.repository.data.SnippetKitData
import clipto.repository.data.SnippetKitUpdateData
import clipto.store.app.TextLanguage
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetRepository @Inject constructor() : ISnippetRepository {

    override fun installKit(kit: SnippetKit): Single<Filter> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun update(kit: SnippetKit, data: SnippetKitUpdateData): Single<SnippetKit> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun getKits(category: SnippetKitCategory?): Single<List<SnippetKit>> = 
        Single.just(emptyList())

    override fun getKit(id: String, force: Boolean): Single<SnippetKitData> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun getCategories(): Single<List<SnippetKitCategory>> = 
        Single.just(emptyList())

    override fun getSnippetDetails(snippet: Snippet): Single<SnippetDetails> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun discardKit(filter: Filter): Single<SnippetKit> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun createLink(filter: Filter): Single<SnippetKit> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun removeLink(filter: Filter): Single<SnippetKit> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun publishKit(filter: Filter): Single<SnippetKit> = 
        Single.error(UnsupportedOperationException("Snippet Kits feature is disabled"))

    override fun getLanguages(kit: SnippetKit): Single<List<TextLanguage>> = 
        Single.just(emptyList())
}