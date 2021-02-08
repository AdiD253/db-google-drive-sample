package pl.defusadr.settings

import pl.defusadr.settings.SettingsType.*

enum class Settings(val type: SettingsType) {
  DEFAULT_INT(INT),
  DEFAULT_STRING(STRING),
  DEFAULT_BOOLEAN(BOOLEAN),
  DATABASE_CONFIG(STRING)
}

enum class SettingsType(val defaultVal: Any) {
  BOOLEAN(false),
  INT(0),
  LONG(0L),
  STRING("")
}