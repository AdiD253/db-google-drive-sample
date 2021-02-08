package pl.defusadr.googledrive.type

enum class DriveMimeType(val type: String) {
  DOCUMENT("application/vnd.google-apps.document"),
  FILE("application/vnd.google-apps.file"),
  FOLDER("application/vnd.google-apps.folder"),
  PHOTO("application/vnd.google-apps.photo"),
  UNKNOWN("application/vnd.google-apps.unknown");

  companion object {
    fun getDriveMimeTypeForValue(value: String): DriveMimeType? {
      values().forEach {
        if (value == it.type) {
          return it
        }
      }
      return null
    }
  }
}