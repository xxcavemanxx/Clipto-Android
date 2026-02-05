package clipto.action

import android.app.Application
import clipto.analytics.Analytics
import clipto.common.misc.IntentUtils
import clipto.repository.IUserRepository
import com.wb.clipboard.R
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareAppLinkAction @Inject constructor(
        private val app: Application,
        private val userRepository: Lazy<IUserRepository>
) : CompletableAction<ActionContext>() {

    override val name: String = "share_app_link"

    fun execute() = execute(ActionContext(showLoadingIndicator = true))

    override fun create(context: ActionContext): Completable = userRepository.get()
            .generateAppLink()
            .observeOn(appState.getViewScheduler())
            .doOnSuccess { Analytics.onShareApp() }
            .doOnSuccess { IntentUtils.share(app, it.toString()) }
            .doOnError { Analytics.onError("error_share_app_link", it) }
            .doOnError { appState.showToast(R.string.essentials_errors_unknown) }
            .ignoreElement()

}