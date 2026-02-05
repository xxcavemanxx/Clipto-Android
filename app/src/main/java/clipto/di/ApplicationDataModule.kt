package clipto.di

import android.app.Application
import android.webkit.URLUtil
import clipto.common.logging.ILogger
import clipto.common.logging.L
import clipto.common.misc.GsonUtils
import clipto.config.AppConfig
import clipto.config.IAppConfig
import clipto.dao.objectbox.LinkPreviewBoxDao
import clipto.dao.objectbox.model.MyObjectBox
import clipto.dao.objectbox.model.toBox
import clipto.dao.objectbox.model.toPreview
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewCache
import clipto.utils.UriUtils
import com.google.gson.Gson
import com.wb.clipboard.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.BoxStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationDataModule {

    init {
        if (BuildConfig.DEBUG) L.init(ILogger.LOGCAT)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonUtils.get()

    @Provides
    @Singleton
    fun provideAppConfig(app: Application): IAppConfig = AppConfig(app)

    @Provides
    @Singleton
    fun provideBoxStore(app: Application): BoxStore = MyObjectBox.builder().androidContext(app).build()

    @Provides
    @Singleton
    fun provideLinkPreviewCache(
        app: Application,
        linkPreviewBoxDao: LinkPreviewBoxDao
    ): LinkPreviewCache =
        object : LinkPreviewCache {
            override fun get(url: String): LinkPreview? {
                val preview = linkPreviewBoxDao.get(url)?.toPreview()
                if (preview != null && preview.sitename.isNullOrBlank() && URLUtil.isNetworkUrl(url)) {
                    preview.sitename = UriUtils.getUriHost(url)
                }
                return preview
            }

            override fun put(metadata: LinkPreview) {
                linkPreviewBoxDao.save(metadata.toBox())
            }

            override fun remove(url: String) {
                linkPreviewBoxDao.remove(url)
            }
        }

}