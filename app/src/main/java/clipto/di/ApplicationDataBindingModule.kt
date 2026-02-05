package clipto.di

import clipto.api.Api
import clipto.api.IApi
import clipto.dao.sharedprefs.DataStoreApi
import clipto.dao.sharedprefs.DataStoreManager
import clipto.dynamic.DynamicValuesRepository
import clipto.dynamic.IDynamicValuesRepository
import clipto.repository.*
import clipto.store.clipboard.ClipboardStateManager
import clipto.store.clipboard.IClipboardStateManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationDataBindingModule {

    @Binds
    abstract fun bindApi(api: Api): IApi

    @Binds
    abstract fun bindClipboardStateManager(manager: ClipboardStateManager): IClipboardStateManager

    @Binds
    abstract fun bindDynamicValuesRepository(repository: DynamicValuesRepository): IDynamicValuesRepository

    @Binds
    abstract fun bindSecurityRepository(repository: SecurityRepository): ISecurityRepository

    @Binds
    abstract fun bindSettingsRepository(repository: SettingsRepository): ISettingsRepository

    @Binds
    abstract fun bindFilterRepository(repository: FilterRepository): IFilterRepository

    @Binds
    abstract fun bindSnippetRepository(repository: SnippetRepository): ISnippetRepository

    @Binds
    abstract fun bindClipRepository(repository: ClipRepository): IClipRepository

    @Binds
    abstract fun bindFileRepository(repository: FileRepository): IFileRepository

    @Binds
    abstract fun bindUserRepository(repository: UserRepository): IUserRepository

    @Binds
    abstract fun bindPreviewRepository(repository: PreviewRepository): IPreviewRepository

    @Binds
    abstract fun bindDataStoreApi(api: DataStoreManager): DataStoreApi

}