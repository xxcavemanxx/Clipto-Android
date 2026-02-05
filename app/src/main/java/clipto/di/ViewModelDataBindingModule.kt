package clipto.di

import clipto.presentation.runes.RunesRepository
import clipto.repository.IRunesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelDataBindingModule {

    @Binds
    abstract fun bindRunesRepository(repository: RunesRepository): IRunesRepository

}