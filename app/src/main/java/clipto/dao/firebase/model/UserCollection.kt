package clipto.dao.firebase.model

import clipto.common.extensions.notNull
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.domain.FileRef
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UserCollection(
    private val firebaseId: String,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseFirestore: FirebaseFirestore
) {

    private val publicNotesRef by lazy { firebaseFirestore.collection(FirebaseDaoHelper.COLLECTION_PUBLIC_NOTES).document(firebaseId).collection(FirebaseDaoHelper.COLLECTION_PUBLIC_NOTES) }
    private val userRef = firebaseFirestore.collection(FirebaseDaoHelper.COLLECTION_USERS).document(firebaseId)
    private val clipsDeletedRef = userRef.collection(FirebaseDaoHelper.COLLECTION_USER_CLIPS_DELETED)
    private val filesDeletedRef = userRef.collection(FirebaseDaoHelper.COLLECTION_USER_FILES_DELETED)
    private val filtersRef = userRef.collection(FirebaseDaoHelper.COLLECTION_USER_FILTERS)
    private val clipsRef = userRef.collection(FirebaseDaoHelper.COLLECTION_USER_CLIPS)
    private val filesRef = userRef.collection(FirebaseDaoHelper.COLLECTION_USER_FILES)

    fun getFilesRef() = filesRef

    fun getFiltersRef() = filtersRef

    fun getActiveClipsRef() = clipsRef

    fun getDeletedClipsRef() = clipsDeletedRef

    fun getFilesDeletedRef() = filesDeletedRef

    fun getFilterRef(id: String) = filtersRef.document(id)

    fun getActiveClipRef(id: String) = clipsRef.document(id)

    fun getActiveFileRef(id: String) = filesRef.document(id)

    fun getDeletedClipRef(id: String) = clipsDeletedRef.document(id)

    fun getDeletedFileRef(id: String) = filesDeletedRef.document(id)

    fun getUserFileRef(folder: String, name: String) = firebaseStorage.getReference("$firebaseId/$folder/$name")

    fun getPublicNoteRef(noteId: String) = publicNotesRef.document(noteId)

    fun getUserFileRef(fileRef: FileRef): StorageReference = getUserFileRef(fileRef.folder.notNull(), fileRef.getUid().notNull())

    fun getUserFileThumbRef(fileRef: FileRef): StorageReference = getUserFileRef(
        folder = "${fileRef.folder.notNull()}/thumbs",
        name = "${fileRef.getUid().notNull()}_200x200"
    )

    fun createBatch() = firebaseFirestore.batch()
}