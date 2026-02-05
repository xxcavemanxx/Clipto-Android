package clipto.dynamic

import android.app.Application
import clipto.config.IAppConfig
import clipto.dao.objectbox.ClipBoxDao
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicValueContext @Inject constructor(
        val dynamicValuesRepository: Lazy<IDynamicValuesRepository>,
        val clipBoxDao: ClipBoxDao,
        val appConfig: IAppConfig,
        val app:Application
)