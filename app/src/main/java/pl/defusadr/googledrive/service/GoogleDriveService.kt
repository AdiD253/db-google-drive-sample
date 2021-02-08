package pl.defusadr.googledrive.service

import pl.defusadr.googledrive.model.DriveFileDTO
import pl.defusadr.googledrive.type.DriveMimeType
import com.google.api.services.drive.Drive
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File

interface GoogleDriveService {

  fun init(driveService: Drive)

  /**
   * Creates new file in Google Drive
   * @param file - file to upload in Google Drive
   * @param mimeType - mime type of file to create in Google Drive
   * @param folderId - id of folder to locale created file or null for creating in root directory
   * @return Task containing created file id
   */
  fun uploadFile(file: File, mimeType: DriveMimeType, folderId: String? = null): Single<DriveFileDTO>

  /**
   * Updates existing file in Google Drive
   * @param fileId - id of existing file to update
   * @param file - file content to update
   * @return Task containing created file id
   */
  fun updateFile(fileId: String, file: File): Single<DriveFileDTO>

  /**
   * Creates new folder in Google Drive
   * @param folderName - name of the folder to create in Google Drive
   * @param folderId - id of folder to locale created folder or null for creating in root directory
   * @return Task containing created folder id
   */
  fun createFolder(folderName: String, folderId: String? = null): Single<DriveFileDTO>

  /**
   * Returns list of files and folders created by this application
   */
  fun listFiles(): Single<List<DriveFileDTO>>

  /**
   * Returns list of files and folders that matches passed [query]
   */
  fun findFilesForQuery(query: String): Single<List<DriveFileDTO>>

  /**
   * Downloads file from Google Drive
   * @param fileId - id of the file to download
   */
  fun downloadFile(fileId: String): Single<String>

  /**
   * Deletes specific file/folder
   * @param fileId - id of file or folder to delete
   */
  fun deleteFile(fileId: String): Completable

  /**
   * Creates read permission to Google Drive file or folder with the given id.
   */
  fun createReadPermissionForElement(id: String): Completable
}