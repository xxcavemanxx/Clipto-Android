package clipto.repository

import clipto.domain.*
import clipto.repository.data.SnippetKitData
import clipto.repository.data.SnippetKitUpdateData
import clipto.store.app.TextLanguage
import io.reactivex.Single

interface ISnippetRepository {

    fun update(kit: SnippetKit, data: SnippetKitUpdateData): Single<SnippetKit>

    fun getKit(id: String, force: Boolean = false): Single<SnippetKitData>

    fun getKits(category: SnippetKitCategory?): Single<List<SnippetKit>>

    fun getCategories(): Single<List<SnippetKitCategory>>

    fun installKit(kit: SnippetKit): Single<Filter>

    fun discardKit(filter: Filter): Single<SnippetKit>

    fun publishKit(filter: Filter): Single<SnippetKit>

    fun createLink(filter: Filter): Single<SnippetKit>

    fun removeLink(filter: Filter): Single<SnippetKit>

    fun getLanguages(kit: SnippetKit): Single<List<TextLanguage>>

    fun getSnippetDetails(snippet: Snippet): Single<SnippetDetails>

}