package clipto.repository

import clipto.domain.Filter
import io.reactivex.Completable
import io.reactivex.Single

interface IFilterRepository {

    fun init(): Completable
    fun terminate(): Completable
    fun save(filter: Filter): Single<Filter>
    fun remove(filter: Filter): Single<Filter>
    fun updateNotesCount(filter: Filter): Single<Filter>

}