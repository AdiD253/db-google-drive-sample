package pl.defusadr.backup.config

import com.squareup.moshi.Json
import pl.defusadr.backup.BackupTable

data class ConfigContent(
    @Json(name = "version") val dbVersion: String,
    @Json(name = "db") val tableContent: TableContent
) {

    fun getBackupTablesByDifferenceBy(otherConfigContent: ConfigContent): List<BackupTable> {
        val backupTables = mutableListOf<BackupTable>()
        val currentContent = this.tableContent
        val otherContent = otherConfigContent.tableContent

        if (currentContent.samplesUpdateTime != otherContent.samplesUpdateTime)
            backupTables.add(BackupTable.SAMPLE)
        //more statements can be added here for other tables if needed
        return backupTables
    }
}

data class TableContent(
    @Json(name = "sample") val samplesUpdateTime: String,
)
