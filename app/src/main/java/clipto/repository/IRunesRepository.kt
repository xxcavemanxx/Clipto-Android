package clipto.repository

import clipto.domain.IRune
import io.reactivex.Single

interface IRunesRepository {

    fun getAll(): Single<List<IRune>>

    fun getById(id: String): Single<IRune>

}