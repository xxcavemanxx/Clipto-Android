package clipto.repository

import io.reactivex.Completable
import io.reactivex.Single

interface ISecurityRepository {

    fun isLocked(): Single<Boolean>
    fun unlock(): Completable
    fun lock(): Completable

    fun isPassCodeSet(): Single<Boolean>
    fun savePassCode(passCode: String): Completable
    fun checkPassCode(passCode: String): Single<Boolean>

    fun isFingerprintEnabled(): Single<Boolean>
    fun setFingerprintEnabled(enabled: Boolean): Completable

}