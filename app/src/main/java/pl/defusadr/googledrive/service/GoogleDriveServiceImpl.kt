package pl.defusadr.googledrive.service

import pl.defusadr.googledrive.model.DriveFileDTO
import pl.defusadr.googledrive.type.DriveMimeType
import pl.defusadr.googledrive.type.DrivePermissionRole
import pl.defusadr.googledrive.type.DrivePermissionType
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.Permission
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import pl.defusadr.googledrive.ext.mapToCompletable
import pl.defusadr.googledrive.ext.mapToSingle
import java.io.ByteArrayOutputStream
import java.net.URLConnection
import java.util.concurrent.Executors
import javax.inject.Inject

class GoogleDriveServiceImpl @Inject constructor() : GoogleDriveService {

    companion object {
        private const val DEFAULT_FILE_FIELDS = "files(id, name, parents, mimeType, permissions)"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var driveService: Drive

    override fun init(driveService: Drive) {
        this.driveService = driveService
    }

    override fun uploadFile(
        file: java.io.File,
        mimeType: DriveMimeType,
        folderId: String?
    ): Single<DriveFileDTO> =
        Tasks.call(executor, {
            val metadata = File()
                .setParents(listOf(folderId))
                .setName(file.name)
            val fileContent = FileContent(URLConnection.guessContentTypeFromName(file.name), file)
            driveService.files()
                .create(metadata, fileContent)
                .execute()
        }).mapToSingle {
            DriveFileDTO(
                id = it.id,
                name = it.name,
                mimeType = DriveMimeType.getDriveMimeTypeForValue(it.mimeType),
                parents = it.parents,
            )
        }

    override fun updateFile(fileId: String, file: java.io.File): Single<DriveFileDTO> =
        Tasks.call(executor, {
            val metadata = File()
                .setName(file.name)
            val fileContent = FileContent(URLConnection.guessContentTypeFromName(file.name), file)
            driveService.files()
                .update(fileId, metadata, fileContent)
                .execute()
        }).mapToSingle {
            DriveFileDTO(
                id = it.id,
                name = it.name,
                mimeType = DriveMimeType.getDriveMimeTypeForValue(it.mimeType),
                parents = it.parents,
            )
        }

    override fun createFolder(folderName: String, folderId: String?): Single<DriveFileDTO> =
        Tasks.call(executor, {
            val metadata = File()
                .setParents(listOf(folderId))
                .setMimeType(DriveMimeType.FOLDER.type)
                .setName(folderName)
            val createdFolder = driveService.files()
                .create(metadata)
                .execute()
            DriveFileDTO(
                id = createdFolder.id,
                name = createdFolder.name,
                mimeType = DriveMimeType.getDriveMimeTypeForValue(createdFolder.mimeType),
                parents = createdFolder.parents
            )
        }).mapToSingle()

    override fun listFiles(): Single<List<DriveFileDTO>> =
        Tasks.call(executor, {
            val files = driveService.files()
                .list()
                .setFields(DEFAULT_FILE_FIELDS)
                .execute()
            files.files.map {
                DriveFileDTO(
                    id = it.id,
                    name = it.name,
                    mimeType = DriveMimeType.getDriveMimeTypeForValue(it.mimeType),
                    parents = it.parents,
                    parentName = findFileParentName(it, files),
                    hasSharePermissions = hasSharePermissions(it)
                )
            }
        }).mapToSingle()

    override fun findFilesForQuery(query: String): Single<List<DriveFileDTO>> =
        Tasks.call(executor, {
            val files = driveService.files()
                .list()
                .setQ(query)
                .setFields(DEFAULT_FILE_FIELDS)
                .execute()
            files.files.map {
                DriveFileDTO(
                    id = it.id,
                    name = it.name,
                    mimeType = DriveMimeType.getDriveMimeTypeForValue(it.mimeType),
                    parents = it.parents,
                    parentName = findFileParentName(it, files),
                    hasSharePermissions = hasSharePermissions(it)
                )
            }
        }).mapToSingle()

    override fun downloadFile(fileId: String): Single<String> =
        Tasks.call(executor, {
            val outputStream = ByteArrayOutputStream()
            driveService.files()
                .get(fileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray().decodeToString()
        }).mapToSingle()

    override fun deleteFile(fileId: String): Completable =
        Tasks.call(executor, {
            driveService.files().delete(fileId).execute()
        }).mapToCompletable()

    override fun createReadPermissionForElement(id: String): Completable =
        Tasks.call(executor, {
            driveService.permissions().create(
                id,
                Permission()
                    .setType(DrivePermissionType.ANYONE.value)
                    .setRole(DrivePermissionRole.READER.value)
            ).execute()
        }).mapToCompletable()

    private fun findFileParentName(file: File, fileList: FileList) =
        fileList.files.find { it.id == file.parents.firstOrNull() }?.name

    private fun hasSharePermissions(file: File) =
        file.permissions.any {
            it.type == DrivePermissionType.ANYONE.value && it.role == DrivePermissionRole.READER.value
        }
}