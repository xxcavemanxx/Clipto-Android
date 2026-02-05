package clipto.dao.sharedprefs

import io.reactivex.Completable
import io.reactivex.Maybe

interface DataStoreApi {

    fun <T> save(key: String, value: T, valueType: Class<T>): Completable

    fun <T> read(key: String, returnType: Class<T>): Maybe<T>

    fun remove(key: String): Completable

    fun clear(): Completable

}