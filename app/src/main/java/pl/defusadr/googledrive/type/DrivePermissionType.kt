package pl.defusadr.googledrive.type

enum class DrivePermissionType(val value: String) {
  USER("user"),
  GROUP("group"),
  DOMAIN("domain"),
  ANYONE("anyone")
}