package pl.defusadr.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "samples")
internal data class SampleEntity(
  @PrimaryKey val id: String,
  val QR: String
) {

  companion object {
    const val TABLE_NAME = "samples"
  }
}

