package clipto.dao.sharedprefs

import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Maybe

abstract class SharedPrefsDataStore(private val gson: Lazy<Gson>) : DataStoreApi {

    private val sharedPrefs by lazy { createSharedPreferences() }

    protected abstract fun createSharedPreferences(): SharedPreferences

    protected inline fun <reified T> read(key: String): Maybe<T> = read(key, T::class.java)

    protected inline fun <reified T> save(key: String, value: T): Completable =
            save(key, value, T::class.java)

    override fun <T> save(key: String, value: T, valueType: Class<T>): Completable {
        return Completable.fromCallable {
            val editor = sharedPrefs.edit()
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Double -> editor.putLong(key, java.lang.Double.doubleToRawLongBits(value))
                else -> {
                    val json = gson.get().toJson(value, valueType)
                    editor.putString(key, json)
                }
            }
            editor.apply()
        }
    }

    override fun <T> read(key: String, returnType: Class<T>): Maybe<T> {
        return Maybe.defer {
            if (!sharedPrefs.contains(key)) {
                return@defer Maybe.empty<T>()
            }
            // this default value is actually does nothing as we already returned Maybe.empty above
            // in case preference with this key is missing.
            // We need it just in order to read the value from sharedprefs (even though it will never occur)
            val defaultValue = getDumbReturnValue(returnType)
            val value: Any?
            value = when (returnType) {
                String::class.java -> sharedPrefs.getString(key, defaultValue as? String)
                java.lang.Boolean::class.java,
                Boolean::class.java -> sharedPrefs.getBoolean(key, defaultValue as Boolean)
                java.lang.Integer::class.java,
                Int::class.java -> sharedPrefs.getInt(key, defaultValue as Int)
                java.lang.Long::class.java,
                Long::class.java -> sharedPrefs.getLong(key, defaultValue as Long)
                java.lang.Float::class.java,
                Float::class.java -> sharedPrefs.getFloat(key, defaultValue as Float)
                java.lang.Double::class.java,
                Double::class.java -> java.lang.Double.longBitsToDouble(
                        sharedPrefs.getLong(
                                key,
                                defaultValue as Long
                        )
                )
                else -> {
                    val json = sharedPrefs.getString(key, defaultValue as? String)
                    json?.let { gson.get().fromJson(json, returnType) }
                }
            }
            (value as? T)?.let { Maybe.fromCallable { it } } ?: Maybe.empty()
        }
    }

    override fun remove(key: String): Completable {
        return Completable.fromAction {
            sharedPrefs.edit().remove(key).apply()
        }
    }

    override fun clear(): Completable {
        return Completable.fromAction {
            sharedPrefs
                    .edit()
                    .clear()
                    .apply()
        }
    }

    private fun <T> getDumbReturnValue(returnType: Class<T>): T {
        val defaultValue: Any? = when (returnType) {
            String::class.java -> null
            java.lang.Boolean::class.java,
            Boolean::class.java -> false
            java.lang.Integer::class.java,
            Int::class.java -> 0
            java.lang.Long::class.java,
            Long::class.java -> 0L
            java.lang.Float::class.java,
            Float::class.java -> 0f
            java.lang.Double::class.java,
            Double::class.java -> 0.0
            else -> null
        }
        return defaultValue as T
    }

}