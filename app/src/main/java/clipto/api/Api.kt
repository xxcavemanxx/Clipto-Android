package clipto.api

import clipto.api.data.*
import clipto.domain.*
import clipto.repository.data.SnippetKitData
import io.reactivex.Completable
import io.reactivex.Maybe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Api @Inject constructor() : IApi {

    override fun deleteAccount(): Completable = Completable.complete()

    override fun startSession(): Maybe<StartSessionResponse> = Maybe.just(
        StartSessionResponse(
            invitedCount = 0,
            userRole = UserRole.ADMIN
        )
    )

    override fun checkSession(request: CheckSessionRequest): Maybe<CheckSessionResponse> = Maybe.just(
        CheckSessionResponse(
            syncLimit = 10000000,
            plan = LicenseType.PERSONAL,
            syncSubscriptionId = null,
            syncSubscriptionToken = null
        )
    )

    override fun getInvitationLink(): Maybe<String> = Maybe.empty()

    override fun getUrlShortLink(url: String): Maybe<String> = Maybe.just(url)

    override fun getSnippetKitCategories(): Maybe<List<SnippetKitCategory>> = Maybe.just(emptyList())

    override fun getSnippetKits(category: SnippetKitCategory?): Maybe<List<SnippetKit>> = Maybe.just(emptyList())

    override fun getSnippetDetails(snippet: Snippet): Maybe<SnippetDetails> = Maybe.empty()

    override fun publishSnippetKit(filter: Filter, language: String, country: String): Maybe<SnippetKit> = Maybe.empty()

    override fun discardSnippetKit(filter: Filter): Maybe<SnippetKit> = Maybe.empty()

    override fun createSnippetKitLink(filter: Filter): Maybe<SnippetKit> = Maybe.empty()

    override fun removeSnippetKitLink(filter: Filter): Maybe<SnippetKit> = Maybe.empty()

    override fun getSnippetKit(id: String): Maybe<SnippetKitData> = Maybe.empty()

    override fun updateSnippetKit(request: UpdateSnippetKitRequest): Maybe<SnippetKit> = Maybe.empty()

    override fun installSnippetKit(kit: SnippetKit): Maybe<Filter> = Maybe.empty()

    override fun upgradeData(): Maybe<String> = Maybe.just("")
}