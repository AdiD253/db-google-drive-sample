package pl.defusadr.settings.defaults

import com.squareup.moshi.Moshi
import pl.defusadr.settings.Settings
import javax.inject.Inject

class SettingsDefaultsProviderImpl @Inject constructor(
  private val moshi: Moshi
) : SettingsDefaultsProvider {

  override fun getDefaultValueFor(settingsName: Settings): Any  = settingsName.type.defaultVal

}