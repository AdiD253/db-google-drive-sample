package pl.defusadr.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import pl.defusadr.settings.SettingsType.*
import pl.defusadr.settings.defaults.SettingsDefaultsProvider
import java.util.*
import javax.inject.Inject

class SettingsManagerImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingsDefaultsProvider: SettingsDefaultsProvider
) : SettingsManager {

  companion object {
    private const val FILE_NAME = "shared_prefs"
  }

  private val booleanCache: EnumMap<Settings, Boolean> = EnumMap(Settings::class.java)
  private val stringCache: EnumMap<Settings, String> = EnumMap(Settings::class.java)
  private val intCache: EnumMap<Settings, Int> = EnumMap(Settings::class.java)
  private val longCache: EnumMap<Settings, Long> = EnumMap(Settings::class.java)

  private lateinit var sharedPreferences: SharedPreferences

  override fun load(): Single<Boolean> =
    getSharedPreferences()
      .flatMapCompletable {
        sharedPreferences = it
        checkAndSetDefaults()
      }
      .andThen(Single.just(true))
      .onErrorReturnItem(false)

  private fun getSharedPreferences(): Single<SharedPreferences> =
    Single.just(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))

  override fun getFlag(setting: Settings): Single<Boolean> = Single.create { emitter ->
    if (setting.type != BOOLEAN) emitter.onError(
      IllegalArgumentException("${setting.name} is not Boolean")
    )
    emitter.onSuccess(
      booleanCache[setting] ?: error(
        IllegalStateException("Settings was not initialized properly - ${setting.name}")
      )
    )
  }

  override fun getIntSetting(setting: Settings): Single<Int> = Single.create { emitter ->
    if (setting.type != INT) emitter.onError(
      IllegalArgumentException("${setting.name} is not Int")
    )
    emitter.onSuccess(
      intCache[setting] ?: error(
        IllegalStateException("Settings was not initialized properly - ${setting.name}")
      )
    )
  }

  override fun getLongSetting(setting: Settings): Single<Long> = Single.create { emitter ->
    if (setting.type != LONG) emitter.onError(
      IllegalArgumentException("${setting.name} is not Long")
    )
    emitter.onSuccess(
      longCache[setting] ?: error(
        IllegalStateException("Settings was not initialized properly - ${setting.name}")
      )
    )
  }

  override fun getStringSetting(setting: Settings): Single<String> = Single.create { emitter ->
    if (setting.type != STRING) emitter.onError(
      IllegalArgumentException("${setting.name} is not String")
    )
    emitter.onSuccess(
      stringCache[setting] ?: error(
        IllegalStateException("Settings was not initialized properly - ${setting.name}")
      )
    )
  }

  override fun setFlag(
    setting: Settings,
    value: Boolean
  ): Completable =
    Completable.fromRunnable {
      if (setting.type != BOOLEAN) throw IllegalArgumentException("${setting.name} is not Boolean")
      booleanCache[setting] = value
      sharedPreferences.edit()
        .putBoolean(setting.name, value)
        .apply()
    }

  override fun setSettings(
    setting: Settings,
    value: Int
  ): Completable =
    Completable.fromRunnable {
      if (setting.type != INT) throw IllegalArgumentException("${setting.name} is not Int")
      intCache[setting] = value
      sharedPreferences.edit()
        .putInt(setting.name, value)
        .apply()
    }

  override fun setSettings(
    setting: Settings,
    value: Long
  ): Completable =
    Completable.fromRunnable {
      if (setting.type != LONG) throw IllegalArgumentException("${setting.name} is not Long")
      longCache[setting] = value
      sharedPreferences.edit()
        .putLong(setting.name, value)
        .apply()
    }

  override fun setSettings(
    setting: Settings,
    value: String
  ): Completable =
    Completable.fromRunnable {
      if (setting.type != STRING) throw IllegalArgumentException("${setting.name} is not String")
      stringCache[setting] = value
      sharedPreferences.edit()
        .putString(setting.name, value)
        .apply()
    }

  private fun checkAndSetDefaults(): Completable = Completable.create {
    Settings.values()
      .forEach { setting ->
        if (sharedPreferences.contains(setting.name)) {
          when (setting.type) {
            BOOLEAN -> booleanCache[setting] =
              sharedPreferences.getBoolean(
                setting.name,
                BOOLEAN.defaultVal as Boolean
              )
            INT -> intCache[setting] =
              sharedPreferences.getInt(setting.name, INT.defaultVal as Int)
            STRING -> stringCache[setting] =
              sharedPreferences.getString(setting.name, STRING.defaultVal as String)
            LONG -> longCache[setting] =
              sharedPreferences.getLong(setting.name, LONG.defaultVal as Long)
          }
        } else {
          when (setting.type) {
            BOOLEAN -> {
              booleanCache[setting] =
                settingsDefaultsProvider.getDefaultValueFor(setting) as Boolean
              sharedPreferences.edit()
                .putBoolean(setting.name, booleanCache[setting]!!)
                .apply()
            }
            INT -> {
              intCache[setting] =
                settingsDefaultsProvider.getDefaultValueFor(setting) as Int
              sharedPreferences.edit()
                .putInt(setting.name, intCache[setting]!!)
                .apply()
            }
            STRING -> {
              stringCache[setting] =
                settingsDefaultsProvider.getDefaultValueFor(setting) as String
              sharedPreferences.edit()
                .putString(setting.name, stringCache[setting])
                .apply()
            }
            LONG -> {
              longCache[setting] =
                settingsDefaultsProvider.getDefaultValueFor(setting) as Long
              sharedPreferences.edit()
                .putLong(setting.name, longCache[setting]!!)
                .apply()
            }
          }
        }
      }
    it.onComplete()
  }
}