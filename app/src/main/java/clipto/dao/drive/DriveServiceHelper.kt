package clipto.dao.drive

import android.app.Application
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveServiceHelper @Inject constructor(
    private val context: Application
) {

    private var driveService: Drive? = null

    @Synchronized
    fun getDriveService(): Drive {
        val currentService = driveService
        if (currentService != null) {
            return currentService
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("User is not signed in to Google Account")

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singletonList(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account ?: account.email?.let { android.accounts.Account(it, "com.google") }

        val service = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Clipto")
            .build()

        driveService = service
        return service
    }

    @Synchronized
    fun reset() {
        driveService = null
    }

    fun findFileByName(name: String): String? {
        val service = getDriveService()
        val query = "name = '$name' and 'appDataFolder' in parents and trashed = false"
        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .setFields("files(id, name)")
            .execute()
        return result.files?.firstOrNull()?.id
    }

    fun listFiles(): List<File> {
        val service = getDriveService()
        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id, name, mimeType, modifiedTime)")
            .execute()
        return result.files ?: emptyList()
    }

    fun downloadFile(fileId: String): String {
        val service = getDriveService()
        val outputStream = ByteArrayOutputStream()
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        return outputStream.toString("UTF-8")
    }

    fun downloadFileStream(fileId: String): InputStream {
        val service = getDriveService()
        return service.files().get(fileId).executeMediaAsInputStream()
    }

    fun uploadFile(name: String, content: String, mimeType: String): String {
        val service = getDriveService()
        val existingId = findFileByName(name)

        val metadata = File().apply {
            this.name = name
            if (existingId == null) {
                parents = Collections.singletonList("appDataFolder")
            }
        }

        val contentStream = ByteArrayContent.fromString(mimeType, content)

        return if (existingId != null) {
            val updated = service.files().update(existingId, metadata, contentStream).execute()
            updated.id
        } else {
            val created = service.files().create(metadata, contentStream).execute()
            created.id
        }
    }

    fun uploadFileStream(name: String, contentStream: AbstractInputStreamContent): String {
        val service = getDriveService()
        val existingId = findFileByName(name)

        val metadata = File().apply {
            this.name = name
            if (existingId == null) {
                parents = Collections.singletonList("appDataFolder")
            }
        }

        return if (existingId != null) {
            val updated = service.files().update(existingId, metadata, contentStream).execute()
            updated.id
        } else {
            val created = service.files().create(metadata, contentStream).execute()
            created.id
        }
    }

    fun deleteFile(fileId: String) {
        val service = getDriveService()
        service.files().delete(fileId).execute()
    }

    fun deleteFileByName(name: String) {
        val fileId = findFileByName(name)
        if (fileId != null) {
            deleteFile(fileId)
        }
    }
}
