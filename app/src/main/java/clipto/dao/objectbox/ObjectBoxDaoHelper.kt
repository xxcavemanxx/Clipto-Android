package clipto.dao.objectbox

import clipto.dao.TxHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectBoxDaoHelper @Inject constructor(
    private val txHelper: TxHelper,
    private val clipBoxDao: ClipBoxDao,
    private val userBoxDao: UserBoxDao,
    private val fileBoxDao: FileBoxDao,
    private val filterBoxDao: FilterBoxDao,
    private val settingsBoxDao: SettingsBoxDao,
    private val linkPreviewBoxDao: LinkPreviewBoxDao
) {

    fun initAll() {
        txHelper.inTx("initAll") {
            userBoxDao.init()
            clipBoxDao.init()
            filterBoxDao.init()
            fileBoxDao.init()
            settingsBoxDao.init()
            linkPreviewBoxDao.init()
        }
    }

    fun removeAll() {
        txHelper.inTx("removeAll") {
            userBoxDao.clear()
            clipBoxDao.clear()
            fileBoxDao.clear()
            filterBoxDao.clear()
            settingsBoxDao.clear()
            linkPreviewBoxDao.clear()
        }
    }

}