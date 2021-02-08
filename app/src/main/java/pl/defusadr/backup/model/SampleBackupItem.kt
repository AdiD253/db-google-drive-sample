package pl.defusadr.backup.model

import androidx.room.PrimaryKey
import pl.defusadr.db.entity.SampleEntity

internal class SampleBackupItem(
  @PrimaryKey val id: String,
  val sampleField: String
)

internal fun SampleBackupItem.toEntity(): SampleEntity =
  SampleEntity(
    id, sampleField
  )

internal fun SampleEntity.toBackupItem(): SampleBackupItem =
  SampleBackupItem(
    id, QR
  )
