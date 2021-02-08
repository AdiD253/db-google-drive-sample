package pl.defusadr.settings.defaults

import pl.defusadr.settings.Settings

interface SettingsDefaultsProvider {
  fun getDefaultValueFor(settingsName: Settings): Any
}