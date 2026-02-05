package clipto.api

import clipto.api.data.CheckSessionRequest
import clipto.api.data.CheckSessionResponse
import clipto.api.data.StartSessionResponse
import clipto.api.data.UpdateSnippetKitRequest
import clipto.domain.*
import clipto.repository.data.SnippetKitData
import io.reactivex.Completable
import io.reactivex.Maybe

interface IApi {

    fun deleteAccount(): Completable

    fun startSession(): Maybe<StartSessionResponse>

    fun checkSession(request: CheckSessionRequest): Maybe<CheckSessionResponse>

    fun getInvitationLink(): Maybe<String>

    fun getUrlShortLink(url: String): Maybe<String>

    fun createNotePublicLink(clip: Clip): Maybe<Clip>

    fun removeNotePublicLink(clip: Clip): Maybe<Clip>

    fun getFilePublicLink(fileRef: FileRef): Maybe<String>

    fun getSnippetKitCategories(): Maybe<List<SnippetKitCategory>>

    fun getSnippetKits(category: SnippetKitCategory?): Maybe<List<SnippetKit>>

    fun getSnippetDetails(snippet: Snippet): Maybe<SnippetDetails>

    fun publishSnippetKit(filter: Filter, language: String, country: String): Maybe<SnippetKit>

    fun discardSnippetKit(filter: Filter): Maybe<SnippetKit>

    fun createSnippetKitLink(filter: Filter): Maybe<SnippetKit>

    fun removeSnippetKitLink(filter: Filter): Maybe<SnippetKit>

    fun getSnippetKit(id: String): Maybe<SnippetKitData>

    fun updateSnippetKit(request: UpdateSnippetKitRequest): Maybe<SnippetKit>

    fun installSnippetKit(kit: SnippetKit): Maybe<Filter>

    fun upgradeData(): Maybe<String>

}