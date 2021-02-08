package pl.defusadr.googledrive.model

import pl.defusadr.googledrive.type.DriveMimeType

data class DriveFileDTO(
  val id: String,
  val name: String,
  val mimeType: DriveMimeType?,
  val parents: List<String>?,
  val parentName: String? = null,
  val hasSharePermissions: Boolean = false
)