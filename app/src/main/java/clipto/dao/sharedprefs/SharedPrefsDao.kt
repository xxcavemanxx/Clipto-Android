package clipto.dao.sharedprefs

import clipto.dao.objectbox.FileBoxDao
import clipto.dao.sharedprefs.data.AddClipScreenData
import clipto.dao.sharedprefs.data.AddFileScreenData
import clipto.dao.sharedprefs.data.MainListData
import clipto.store.app.AppState
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsDao @Inject constructor(
    private val appState: AppState,
    private val fileBoxDao: FileBoxDao,
    private val dataStoreApi: DataStoreApi,
    private val sharedPrefsState: SharedPrefsState,
) {

    companion object {
        const val INSTANCE_ID = "INSTANCE_ID"
        const val ADD_CLIP_DATA = "ADD_CLIP_DATA"
        const val ADD_FILE_DATA = "ADD_FILE_DATA"
        const val MAIN_LIST_DATA = "MAIN_LIST_DATA"
    }

    fun getMainListData(): Single<MainListData> {
        return dataStoreApi
            .read(MAIN_LIST_DATA, MainListData::class.java)
            .toSingle(MainListData())
            .doOnSuccess { sharedPrefsState.mainListData.setValue(it) }
    }

    fun saveMainListData(data: MainListData): Single<MainListData> {
        return dataStoreApi.save(MAIN_LIST_DATA, data, MainListData::class.java)
            .toSingle { data }
            .doOnSuccess { sharedPrefsState.mainListData.setValue(it) }
    }

    fun getAddClipData(): Single<AddClipScreenData> {
        return dataStoreApi
            .read(ADD_CLIP_DATA, AddClipScreenData::class.java)
            .map { data ->
                if (data.folderId != null && fileBoxDao.getByUid(data.folderId) == null) {
                    data.copy(folderId = null)
                } else {
                    data
                }
            }
            .toSingle(AddClipScreenData())
    }

    fun saveAddClipData(data: AddClipScreenData): Single<AddClipScreenData> {
        return dataStoreApi.save(ADD_CLIP_DATA, data, AddClipScreenData::class.java)
            .toSingle { data }
    }

    fun getAddFileData(): Single<AddFileScreenData> {
        return dataStoreApi
            .read(ADD_FILE_DATA, AddFileScreenData::class.java)
            .map { data ->
                if (data.folderId != null && fileBoxDao.getByUid(data.folderId) == null) {
                    data.copy(folderId = null)
                } else {
                    data
                }
            }
            .toSingle(AddFileScreenData())
    }

    fun saveAddFileData(data: AddFileScreenData): Single<AddFileScreenData> {
        return dataStoreApi.save(ADD_FILE_DATA, data, AddFileScreenData::class.java)
            .toSingle { data }
    }

    fun getInstanceId(): Single<String> {
        return dataStoreApi
            .read(INSTANCE_ID, String::class.java)
            .toSingle()
            .onErrorResumeNext {
                val id = appState.getInstanceId()
                dataStoreApi.save(INSTANCE_ID, id, String::class.java).toSingle { id }
            }
            .doOnSuccess { appState.instanceId.setValue(it) }
    }

}