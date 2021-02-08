package pl.defusadr.backup

import android.content.Context
import android.content.Intent
import pl.defusadr.googledrive.model.DriveFileDTO
import pl.defusadr.googledrive.provider.GoogleDriveProvider
import pl.defusadr.googledrive.service.GoogleDriveService
import pl.defusadr.googledrive.type.DriveMimeType
import pl.defusadr.backup.manager.DatabaseBackupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

const val DATABASE_BACKUP_DIR = "db"
const val DATABASE_BACKUP_CONFIG_FILE = "config.json"

sealed class DatabaseBackupUseCase {

    class ObserveContentChanges @Inject constructor(
        private val backupManager: DatabaseBackupManager
    ) {
        fun executeStart() = backupManager.startDatabaseContentObserver()

        fun executeStop() = backupManager.stopDatabaseContentObserver()
    }

    class ImportDatabase @Inject constructor(
        @ApplicationContext private val applicationContext: Context,
        private val driveProvider: GoogleDriveProvider,
        private val driveService: GoogleDriveService,
        private val backupManger: DatabaseBackupManager
    ) : DatabaseBackupUseCase() {
        fun execute(signInIntent: Intent?): Observable<String> =
            driveProvider.initDriveService(applicationContext, signInIntent)
                .flatMap {
                    backupManger.countDatabaseContent()
                }
                .flatMapObservable {
                    if (it == 0) importDriveContent()
                    else Observable.empty()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        private fun importDriveContent(): Observable<String> {
            return driveService.listFiles()
                .flatMapObservable { files ->
                    Observable.fromIterable(
                        files
                            .distinctBy { it.id }
                            .filter { it.mimeType != DriveMimeType.FOLDER })
                }
                .concatMapSingle { file ->
                    driveService.downloadFile(file.id)
                        .map { fileContent ->
                            file.name to fileContent
                        }
                }
                .flatMapSingle {
                    backupManger.insertFileContent(it.first, it.second)
                }
                .doOnSubscribe {
                    backupManger.stopDatabaseContentObserver()
                }
                .doOnComplete {
                    backupManger.startDatabaseContentObserver()
                }
                .doOnError {
                    backupManger.startDatabaseContentObserver()
                }
        }
    }

    class ExportDatabase @Inject constructor(
        @ApplicationContext private val applicationContext: Context,
        private val driveProvider: GoogleDriveProvider,
        private val driveService: GoogleDriveService,
        private val backupManger: DatabaseBackupManager
    ) : DatabaseBackupUseCase() {

        /**
         * Performs export of current database to the Google Drive
         * Should be called after gaining required Google Drive permissions
         */
        fun execute(signInIntent: Intent? = null): Observable<DriveFileDTO> =
            driveProvider.initDriveService(applicationContext, signInIntent)
                .flatMap {
                    driveService.findFilesForQuery(
                        "name = '$DATABASE_BACKUP_CONFIG_FILE'"
                    )
                }
                .flatMapObservable { configFile ->
                    if (configFile.isEmpty()) {
                        /**
                         * No backup files on Google Drive.
                         * 1. Export all tables to json files
                         * 2. Create and upload config.json
                         */
                        createFullBackup()
                    } else {
                        /**
                         * Backup files are present on Google Drive.
                         * 1. Download config file
                         * 2. Compare config table modification time with local modification time
                         * 2.1: If different:
                         * 2.1.1: Update each table file to Drive
                         * 2.1.2: Generate config.json file
                         */
                        createPartialBackup(configFile.first().id)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())


        private fun createFullBackup(): Observable<DriveFileDTO> {
            var dbFolderId = ""
            //check if current db backup folder exists
            return driveService.findFilesForQuery("name ='$DATABASE_BACKUP_DIR'")
                .flatMap { databaseDir ->
                    if (databaseDir.isEmpty()) {
                        //create db backup folder
                        driveService.createFolder(DATABASE_BACKUP_DIR)
                    } else {
                        //return existing database folder
                        Single.just(databaseDir.first())
                    }
                }
                .flatMap {
                    dbFolderId = it.id
                    //export all tables and config file and save them to [File]
                    Single.zip(
                        backupManger.getAllTables(),
                        backupManger.getConfiguration(),
                        { dbTables, config ->
                            listOf(*dbTables.toTypedArray(), config)
                        }
                    )
                }
                .flatMapObservable {
                    Observable.fromIterable(it)
                }
                .concatMapSingle {
                    driveService.uploadFile(it, DriveMimeType.FILE, dbFolderId)
                }
        }

        private fun createPartialBackup(dbFolderId: String): Observable<DriveFileDTO> {
            var driveTableFiles = listOf<DriveFileDTO>()
            return driveService.listFiles()
                .flatMap { files ->
                    driveTableFiles = files
                    val configFile = files.find { it.name == DATABASE_BACKUP_CONFIG_FILE }
                    if (configFile != null) {
                        driveService.downloadFile(configFile.id)
                    } else {
                        /**
                         * No config file found on Google Drive
                         * TODO: Implement correct state handling
                         * Possible scenario:
                         *  Delete all files and perform full backup again
                         */
                        Single.error(IllegalStateException("No config found. Aborting"))
                    }
                }
                .flatMap {
                    backupManger.getTablesToBackupByConfigContent(it)
                }
                .flatMapObservable {
                    Observable.fromIterable(it)
                }
                .concatMapSingle { fileToBackup ->
                    val fileId = driveTableFiles.find { it.name == fileToBackup.name }?.id
                    if (fileId == null) {   //should not happen, but needs handing
                        driveService.uploadFile(fileToBackup, DriveMimeType.FILE, dbFolderId)
                    } else {
                        driveService.updateFile(fileId, fileToBackup)
                    }
                }
        }
    }


}