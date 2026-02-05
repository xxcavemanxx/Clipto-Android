package clipto.dao.sharedprefs

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(gson: Lazy<Gson>, private val context: Application) :
        SharedPrefsDataStore(gson), DataStoreApi {

    override fun createSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

}