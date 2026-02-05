package clipto.repository

import clipto.domain.Settings
import io.reactivex.Single

interface ISettingsRepository {

    fun get():Settings

    fun update(settings: Settings): Single<Settings>

}