package clipto.repository

import clipto.dao.objectbox.SettingsBoxDao
import clipto.dao.objectbox.model.toBox
import clipto.domain.Settings
import clipto.store.app.AppState
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsBoxDao: SettingsBoxDao,
    private val appState: AppState
) : ISettingsRepository {

    override fun get(): Settings = settingsBoxDao.get()

    override fun update(settings: Settings): Single<Settings> = Single
        .fromCallable {
            val box = settings.toBox()
            settingsBoxDao.save(box)
            box
        }
        .doOnSuccess { appState.refreshSettings() }
        .map { it }

}