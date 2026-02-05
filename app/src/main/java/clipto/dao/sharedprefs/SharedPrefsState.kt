package clipto.dao.sharedprefs

import clipto.dao.sharedprefs.data.MainListData
import clipto.store.StoreObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsState @Inject constructor() {

    val mainListData = StoreObject<MainListData>(
        id = "main_list_data"
    )

}