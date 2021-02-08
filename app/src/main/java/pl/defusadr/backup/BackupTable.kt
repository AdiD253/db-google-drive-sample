package pl.defusadr.backup

import pl.defusadr.db.entity.SampleEntity

enum class BackupTable(val tableName: String) {
    SAMPLE(SampleEntity.TABLE_NAME)
}