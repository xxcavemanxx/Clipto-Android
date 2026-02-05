package clipto.repository

import android.util.LruCache
import clipto.extensions.log
import io.reactivex.Single
import java.util.*

abstract class CacheableRepository {

    private val cache: LruCache<Any, CacheItem> = LruCache(1000)
    private val calendar: Calendar = Calendar.getInstance()

    fun <T> Single<T>.cached(key: String, expireInSeconds: Int, cachedIf: () -> Boolean = { true }): Single<T> {
        return if (cachedIf.invoke()) {
            log("cached :: {}", key)
            cachable(key, this, expireInSeconds)
        } else {
            log("not cached :: {}", key)
            this
        }
    }

    fun <T> Single<T>.cached(key: Any, expireInSecondsDefault: Int, expireInSecondsPredicate: Int, predicate: (T) -> Boolean): Single<T> {
        return cachable(key, this, expireInSecondsDefault, expireInSecondsPredicate, predicate)
    }

    fun <T> cachable(key: Any, item: Single<T>, expireInSeconds: Int): Single<T> {
        if (expireInSeconds == 0) {
            return item
        }
        val cacheItem: CacheItem? = cache.get(key)
        if (cacheItem != null) {
            val valueFromCache: Single<T>? = checkExpiration(cacheItem, key, expireInSeconds)
            // return cached value if it is not expired
            valueFromCache?.let { return it }
        }
        return item.doOnSuccess {
            cache.put(key, CacheItem(createDate = Date(), data = it as Any))
        }
    }

    private fun <T> cachable(key: Any, item: Single<T>, expireInSeconds: Int, expireInSecondsPredicate: Int, predicate: (T) -> Boolean): Single<T> {
        if (expireInSeconds == expireInSecondsPredicate) {
            throw IllegalArgumentException("There is no point in using predicate as expiration times are equal")
        } else if (expireInSeconds == CACHE_UNLIMITED) {
            throw IllegalArgumentException("There is no point in using predicate as unlimited cache is used")
        }

        val cacheItem: CacheItem? = cache.get(key)
        if (cacheItem != null) {
            // get value from cache.
            // Check it for expiration depending on whether  predicate matched or not
            val valueFromCache: Single<T>? = if (predicate(cacheItem.data as T)) {
                // predicate matched. now we should cache with expireInSecondsPredicate time
                checkExpiration(cacheItem, key, expireInSecondsPredicate)
            } else {
                // predicate didn't match. We should cache with default expireInSeconds time
                checkExpiration(cacheItem, key, expireInSeconds)
            }
            // return cached value if it is not expired
            valueFromCache?.let { return it }
        }
        return item.doOnSuccess {
            cache.put(key, CacheItem(createDate = Date(), data = it as Any))
        }
    }

    /**
     * Returns a value from cache if it is not expired or `null` if it is expired.
     */
    private fun <T> checkExpiration(cacheItem: CacheItem, key: Any, expirationTimeSeconds: Int): Single<T>? {
        if (expirationTimeSeconds > 0) {
            val usageDate = Date()
            calendar.time = cacheItem.createDate
            calendar.add(Calendar.SECOND, expirationTimeSeconds)
            if (calendar.time.after(usageDate)) {
                cacheItem.usageDate = usageDate
                return Single.just(cacheItem.data as T)
            }
        } else {
            return Single.just(cacheItem.data as T)
        }
        return null
    }

    data class CacheItem(
            val createDate: Date,
            val data: Any,
            var usageDate: Date? = null
    )

    companion object {
        const val CACHE_1_MINUTE = 60 * 1
        const val CACHE_5_MINUTES = 60 * 5
        const val CACHE_15_MINUTES = 60 * 15
        const val CACHE_UNLIMITED = -1
    }
}