package clipto.action

import clipto.repository.ISettingsRepository
import clipto.store.main.MainState
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveSettingsAction @Inject constructor(
        private val mainState: MainState,
        private val settingsRepository: Lazy<ISettingsRepository>
) : CompletableAction<ActionContext>() {

    override val name: String = "save_settings"

    fun execute(callback: () -> Unit = {}) = execute(ActionContext.EMPTY, callback)

    override fun create(context: ActionContext): Completable = Completable
            .fromCallable { mainState.listConfig.updateValue { it!!.copy(appState.getSettings()) } }
            .andThen(settingsRepository.get().update(appState.getSettings()).ignoreElement())

}