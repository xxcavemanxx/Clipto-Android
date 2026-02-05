package clipto.repository

import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.common.presentation.mvvm.model.AuthorizationState
import clipto.dao.TxHelper
import clipto.dao.firebase.ClipFirebaseDao
import clipto.dao.firebase.FileFirebaseDao
import clipto.dao.firebase.FilterFirebaseDao
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.*
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Filter
import clipto.domain.User
import clipto.extensions.createTag
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.google.firebase.auth.FirebaseAuth
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: Lazy<IApi>,
    private val txHelper: TxHelper,
    private val appState: AppState,
    private val userState: UserState,
    private val mainState: MainState,
    private val userBoxDao: UserBoxDao,
    private val clipBoxDao: ClipBoxDao,
    private val fileBoxDao: FileBoxDao,
    private val fileMapper: FileMapper,
    private val filterBoxDao: FilterBoxDao,
    private val clipboardState: ClipboardState,
    private val settingsBoxDao: SettingsBoxDao,
    private val filterFirebaseDao: FilterFirebaseDao,
    private val firebaseDaoHelper: FirebaseDaoHelper,
    private val objectBoxDaoHelper: ObjectBoxDaoHelper,
    private val filterRepository: IFilterRepository,
    private val clipFirebaseDao: ClipFirebaseDao,
    private val fileFirebaseDao: FileFirebaseDao,
    private val clipRepository: IClipRepository,
    private val fileRepository: IFileRepository
) : IUserRepository {

    override fun init(): Completable = Completable
        .fromCallable {
            val user = userBoxDao.getUser()
            if (user != null) {
                userState.user.setValue(user)
                onAuthStateChanged()
            } else {
                userState.user.setValue(User.NULL)
            }
        }

    override fun terminate(): Completable = Completable
        .fromCallable {
            firebaseDaoHelper.terminate()
            objectBoxDaoHelper.removeAll()
            userState.user.setValue(User.NULL)
            mainState.resetFilter()
            mainState.clearSelection()
            clipboardState.clearAll()
        }

    override fun generateAppLink(): Maybe<String> = api.get().getInvitationLink()

    override fun login(user: User): Single<User> = Single
        .fromCallable<User> {
            txHelper.inTx("User login") {
                settingsBoxDao.clear()
                userBoxDao.login(user.toBox())
            }
        }
        .doOnSuccess { userState.forceSync.setValue(true) }
        .doOnSuccess { userState.user.setValue(it) }

    override fun logout(user: User): Single<User> = filterRepository.terminate()
        .andThen(fileRepository.terminate())
        .andThen(clipRepository.terminate())
        .andThen(terminate())
        .toSingle { User.NULL }

    override fun update(user: User): Single<User> = Single
        .fromCallable { userBoxDao.save(user.toBox()) }
        .doOnSuccess { userState.user.setValue(it, force = true) }
        .map { it }

    override fun delete(user: User): Single<User> = api.get()
        .deleteAccount()
        .andThen(logout(user))

    override fun upgrade(): Single<String> = Single.fromCallable { StringBuilder() }
        .flatMap { sb -> upgradeFiles(sb) }
        .map { sb -> sb.appendLine("___") }
        .flatMap { sb -> upgradeTags(sb) }
        .map { sb -> sb.appendLine("___") }
        .flatMap { sb -> upgradeData(sb) }
        .doOnSuccess { log("upgrade :: {}", it) }
        .map { it.toString() }

    private fun upgradeFiles(sb: StringBuilder): Single<StringBuilder> = Single
        .fromCallable {
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                val clips = clipBoxDao.getLegacyFiles()
                log("upgradeFiles :: {}", clips.size)
                clips.forEach { clip ->
                    val fileRefs = clip.files.map { fileMapper.mapMetaToFileRef(it) }
                    val prevFileIds = clip.fileIds
                    val newFileIds = fileRefs
                        .mapNotNull { it.getUid() }
                        .filter { !prevFileIds.contains(it) }
                        .toMutableList()
                    txHelper.inTx("migrate file") {
                        newFileIds.addAll(prevFileIds)
                        clip.fileIds = newFileIds
                        clip.files = emptyList()

                        val batch = collection.createBatch()
                        fileRefs.forEach { fileRef ->
                            val prevFileRef = fileBoxDao.getByUid(fileRef.getUid())
                            if (prevFileRef == null) {
                                fileBoxDao.save(fileRef)
                                fileFirebaseDao.saveInBatch(fileRef, batch, collection)
                                log("migrate :: save new file from meta :: {}", fileRef.getUid())
                            }
                        }

                        val clipBox = clip.toBox()
                        val clipChanges = mutableMapOf<String, Any?>(
                            FirebaseDaoHelper.ATTR_CLIP_FILE_IDS to clip.fileIds,
                            FirebaseDaoHelper.ATTR_CLIP_FILE_METADATA to FirebaseDaoHelper.getFieldValueDelete()
                        )
                        log("update file :: {} -> {}", clipBox.firestoreId, clip.fileIds)
                        clipBoxDao.save(clipBox)
                        clipFirebaseDao.saveInBatch(clip, batch, collection, clipChanges)

                        batch.commit().addOnCompleteListener { log("upgrade completed for a clip: {}", clipBox.firestoreId) }
                    }
                }
                sb.appendLine("[LOCAL] updated clips with files: ${clips.size}")
            }
            sb
        }

    private fun upgradeTags(sb: StringBuilder): Single<StringBuilder> = Single
        .fromCallable {
            val legacyClips = clipBoxDao.getLegacyTags()
            val collection = firebaseDaoHelper.getAuthUserCollection()
            val filters = appState.getFilters()
            var newTags = 0
            var clearedClips = 0
            var updatedClips = 0
            val updatedTags = mutableSetOf<String>()
            firebaseDaoHelper.splitBatches(legacyClips) { clips ->
                txHelper.inTx("upgradeTags") {
                    var hasChanges = false
                    val batch = collection?.createBatch()
                    val createdFilters = mutableListOf<FilterBox>()
                    clips.forEach { clip ->
                        val clipTags = clip.tags?.split("~")?.filter { it.isNotBlank() } ?: emptyList()
                        clipTags.forEach { tagName ->
                            val tagFilter = filters.findFilterByTagName(tagName)?.toBox()
                            if (tagFilter == null) {
                                log("upgradeTags :: create new tag :: {}", tagName)
                                val newFilter = Filter.createTag(tagName, useOldIdGenerator = true).toBox()
                                createdFilters.add(newFilter)
                                filters.filtersByUid[newFilter.uid!!] = newFilter
                                newTags++
                            }
                        }

                        val newTagIds = clipTags
                            .mapNotNull { tagName -> filters.findFilterByTagName(tagName) }
                            .mapNotNull { filter -> filter.uid }
                            .distinct()
                            .minus(clip.tagIds)

                        clip.tagIds
                            .minus(updatedTags)
                            .mapNotNull { filters.findFilterByTagId(it) }
                            .forEach { updated ->
                                log("upgradeTags :: update existing tag :: {} -> {}", updated.uid, updated.name)
                                filterFirebaseDao.save(updated, batch, collection)
                            }
                        updatedTags.addAll(clip.tagIds)

                        if (newTagIds.isNotEmpty()) {
                            log("upgradeTags :: update clip tags :: {} -> {}", clip.firestoreId, newTagIds)
                            val newClip = clip.toBox(new = true)
                            newClip.tagIds = newClip.tagIds.plus(newTagIds).distinct()
                            newClip.tags = null
                            filterBoxDao.update(clip, newClip)
                            clipBoxDao.save(newClip)

                            batch?.takeIf { newClip.isSynced() }?.let {
                                val changes = mutableMapOf<String, Any?>(
                                    FirebaseDaoHelper.ATTR_CLIP_TAG_IDS to newClip.tagIds,
                                    FirebaseDaoHelper.ATTR_CLIP_TAGS to FirebaseDaoHelper.getFieldValueDelete()
                                )
                                clipFirebaseDao.saveInBatch(newClip, batch, collection, changes)
                            }

                            hasChanges = true
                            updatedClips++
                        } else {
                            clip.tags = null
                            clipBoxDao.save(clip)

                            batch?.takeIf { clip.isSynced() }?.let {
                                val changes = mutableMapOf<String, Any?>(
                                    FirebaseDaoHelper.ATTR_CLIP_TAG_IDS to clip.tagIds,
                                    FirebaseDaoHelper.ATTR_CLIP_TAGS to FirebaseDaoHelper.getFieldValueDelete()
                                )
                                clipFirebaseDao.saveInBatch(clip, batch, collection, changes)
                            }

                            hasChanges = true
                            clearedClips++
                        }
                    }

                    createdFilters.forEach { filter ->
                        log("upgradeTags :: create new tag :: {}", filter.name)
                        filterBoxDao.save(filter)
                        batch?.let { filterFirebaseDao.save(filter, batch, collection) }
                        hasChanges = true
                    }

                    if (hasChanges) {
                        log("upgradeTags :: changes :: {}", hasChanges)
                        batch?.commit()
                    }
                }
            }

            sb.appendLine("[LOCAL] updated clips with tags: $updatedClips")
            sb.appendLine("[LOCAL] cleared clips with tags: $clearedClips")
            sb.appendLine("[LOCAL] updated existing tags: ${updatedTags.size}")
            sb.appendLine("[LOCAL] added new tags: $newTags")
        }

    private fun upgradeData(sb: StringBuilder): Single<StringBuilder> =
        if (userState.isAuthorized()) {
            api.get()
                .upgradeData()
                .onErrorReturn { it.stackTraceToString() }
                .map { sb.appendLine("[CLOUD] $it") }
                .toSingle()
        } else {
            Single.just(sb)
        }

    private fun onAuthStateChanged() {
        FirebaseAuth.getInstance().addAuthStateListener {
            val user = userState.user.requireValue()
            val currentUser = it.currentUser
            if (!userState.isSignOutInProgress() && user.isAuthorized() && user.firebaseId != currentUser?.uid) {
                userState.authorizationState.setValue(AuthorizationState.AUTHORIZATION_REQUIRED)
                Analytics.errorWrongAuthState()
            }
        }
    }

}