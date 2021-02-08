package pl.defusadr.googledrive.type

enum class DrivePermissionRole(val value: String) {
  COMMENTER("commenter"),
  READER("reader"),
  WRITER("writer"),
  OWNER("owner")
}