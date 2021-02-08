package pl.defusadr.backup.manager

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject
import pl.defusadr.backup.BackupTable
import pl.defusadr.backup.DATABASE_BACKUP_CONFIG_FILE
import pl.defusadr.backup.config.ConfigContent
import pl.defusadr.backup.model.SampleBackupItem
import pl.defusadr.backup.model.toBackupItem
import pl.defusadr.backup.model.toEntity
import pl.defusadr.db.AppRoomDatabase
import pl.defusadr.settings.Settings
import pl.defusadr.settings.SettingsManager
import pl.defusadr.util.format
import pl.defusadr.util.fromJson
import pl.defusadr.util.mapEach
import java.io.File
import java.lang.reflect.Type
import java.util.*
import javax.inject.Inject

private const val CONFIG_DB_VERSION = "version"
private const val CONFIG_DB_CONTENT = "db"
private const val DATE_FULL_FORMAT = "dd.MM.yyyy HH:mm:ss"


internal class DatabaseBackupManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val moshi: Moshi
) : DatabaseBackupManager {

    private lateinit var database: AppRoomDatabase
    private lateinit var dbContentObserver: DatabaseContentObserver
    private var databaseContentObserverActive = false

    override fun load(): Single<Boolean> {
        this.database = AppRoomDatabase.getInstance(context)
        this.dbContentObserver = DatabaseContentObserver(database.backupDao())

        return initDatabaseConfig()
            .subscribeOn(Schedulers.io())
            .andThen(
                Completable.fromAction {
                    startDatabaseContentObserver()
                })
            .andThen(Single.just(true))
            .onErrorReturnItem(false)
    }


    override fun getAllTables(): Single<List<File>> =
        Single.merge(
            BackupTable.values()
                .map { exportTable(it) }
        )
            .toList()

    override fun countDatabaseContent(): Single<Int> =
        Single.fromCallable {
            database.backupDao()
                .countDatabaseContent()
        }

    override fun insertFileContent(fileName: String, fileContent: String): Single<String> =
        if (fileName == DATABASE_BACKUP_CONFIG_FILE) {
            settingsManager.setSettings(Settings.DATABASE_CONFIG, fileContent)
        } else {
            importTable(fileName.substringBefore('.'), fileContent)
        }
            .andThen(Single.just(fileName))

    override fun getTablesToBackupByConfigContent(configJson: String): Single<List<File>> =
        Single.zip(
            Single.just(configJson.fromJson<ConfigContent>(moshi)),
            settingsManager.getStringSetting(Settings.DATABASE_CONFIG)
                .flatMap { Single.just(it.fromJson<ConfigContent>(moshi)) },
            { remoteConfig, localConfig ->
                prepareExportContentByDifference(remoteConfig, localConfig)
            }
        )
            .flatMap { exportContent ->
                Single.just(
                    if (exportContent.isEmpty()) listOf()
                    else listOf(*exportContent.toTypedArray(), getConfiguration())
                )
            }
            .flatMapPublisher {
                Single.merge(it)
            }
            .toList()

    private fun prepareExportContentByDifference(
        remoteConfig: ConfigContent,
        localConfig: ConfigContent
    ): List<Single<File>> {
        val exportFiles = mutableListOf<Single<File>>()
        remoteConfig.getBackupTablesByDifferenceBy(localConfig).forEach { tableName ->
            exportFiles.add(exportTable(tableName))
        }
        return exportFiles
    }

    override fun startDatabaseContentObserver() {
        if (this::database.isInitialized && !databaseContentObserverActive) {
            databaseContentObserverActive = true
            dbContentObserver.start { backupTable ->
                updateDatabaseConfig(backupTable)
            }
        }
    }

    override fun stopDatabaseContentObserver() {
        databaseContentObserverActive = false
        dbContentObserver.stop()
    }

    override fun getConfiguration(): Single<File> =
        settingsManager.getStringSetting(Settings.DATABASE_CONFIG)
            .flatMap { config ->
                val file = File(context.cacheDir, DATABASE_BACKUP_CONFIG_FILE).apply {
                    writeText(config)
                }
                Single.just(file)
            }

    private fun initDatabaseConfig(): Completable =
        settingsManager.getStringSetting(Settings.DATABASE_CONFIG)
            .flatMapCompletable {
                if (it.isEmpty()) {
                    createDatabaseConfigFile()
                } else {
                    verifyDatabaseConfigVersion()
                }
            }

    private fun updateDatabaseConfig(table: BackupTable) {
        synchronized(this) {
            settingsManager.getStringSetting(Settings.DATABASE_CONFIG)
                .flatMapCompletable {
                    val configObject = JSONObject(it)
                    val databaseContent = configObject.getJSONObject(CONFIG_DB_CONTENT)
                    databaseContent.put(table.tableName, Date().format(DATE_FULL_FORMAT))
                    configObject.put(CONFIG_DB_CONTENT, databaseContent)
                    settingsManager.setSettings(Settings.DATABASE_CONFIG, configObject.toString())
                }
                .blockingAwait()
        }
    }

    private fun verifyDatabaseConfigVersion(): Completable =
        settingsManager.getStringSetting(Settings.DATABASE_CONFIG)
            .flatMapCompletable {
                val configObject = JSONObject(it)
                val configVersion = configObject.getInt(CONFIG_DB_VERSION)
                val currentDbVersion = database.openHelper.readableDatabase.version
                if (configVersion == currentDbVersion) Completable.complete()
                else {
                    configObject.put(CONFIG_DB_VERSION, currentDbVersion)
                    settingsManager.setSettings(Settings.DATABASE_CONFIG, configObject.toString())
                }
            }

    private fun createDatabaseConfigFile(): Completable {
        val configObject = JSONObject()
        configObject.put(CONFIG_DB_VERSION, database.openHelper.readableDatabase.version)
        configObject.put(CONFIG_DB_CONTENT, createTableConfigContent())
        return settingsManager.setSettings(Settings.DATABASE_CONFIG, configObject.toString())
    }

    private fun createTableConfigContent(): JSONObject = JSONObject().apply {
        BackupTable.values().forEach {
            put(it.tableName, Date().format(DATE_FULL_FORMAT))
        }
    }

    private fun exportTable(table: BackupTable): Single<File> =
        when (table) {
            BackupTable.SAMPLE -> exportSamples()
        }

    private fun importTable(
        tableName: String,
        tableContent: String
    ): Completable {
        val contentObject = JSONObject(tableContent)
        val tableEntities = contentObject.getJSONArray(tableName).toString()
        return when (tableName) {
            BackupTable.SAMPLE.tableName -> {
                val items = getTableContent<SampleBackupItem>(tableEntities)
                importSamples(items)
            }
            else -> Completable.error(IllegalArgumentException("Cannot find table name $tableName"))
        }
    }

    private inline fun <reified T> getTableContent(tableContent: String): List<T> {
        val type: Type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(tableContent) ?: listOf()
    }

    private fun importSamples(samples: List<SampleBackupItem>): Completable =
        Completable.fromCallable {
            val sampleEntities = samples.map { it.toEntity() }
            database.backupDao()
                .clearAndInsertSamples(sampleEntities)
        }

    private fun exportSamples(): Single<File> =
        database.backupDao()
            .getSamples()
            .mapEach { it.toBackupItem() }
            .map { backupItems ->
                createJsonFile(
                    tableContent = backupItems,
                    name = BackupTable.SAMPLE.tableName
                )
            }

    private inline fun <reified T> createJsonFile(tableContent: List<T>, name: String): File {
        val type: Type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        val items = adapter.toJson(tableContent)
        val itemsJsonArray = JSONArray(items)
        val fileContent = JSONObject().apply {
            put(name, itemsJsonArray)
        }
        return File(context.cacheDir, "$name.json").apply {
            writeText(fileContent.toString())
        }
    }
}