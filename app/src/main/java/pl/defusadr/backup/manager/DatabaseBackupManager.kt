package pl.defusadr.backup.manager

import io.reactivex.rxjava3.core.Single
import pl.defusadr.util.LoadableSystem
import java.io.File

interface DatabaseBackupManager : LoadableSystem {

    fun getAllTables(): Single<List<File>>

    fun countDatabaseContent(): Single<Int>

    fun getConfiguration(): Single<File>

    fun insertFileContent(fileName: String, fileContent: String): Single<String>

    fun getTablesToBackupByConfigContent(configJson: String): Single<List<File>>

    fun startDatabaseContentObserver()

    fun stopDatabaseContentObserver()
}