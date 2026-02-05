package clipto.repository

import clipto.domain.User
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

interface IUserRepository {

    fun init(): Completable

    fun terminate(): Completable

    fun upgrade(): Single<String>

    fun generateAppLink(): Maybe<String>

    fun login(user: User): Single<User>

    fun logout(user: User): Single<User>

    fun update(user: User): Single<User>

    fun delete(user: User): Single<User>

}