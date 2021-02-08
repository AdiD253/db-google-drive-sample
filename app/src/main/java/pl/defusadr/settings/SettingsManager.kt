package pl.defusadr.settings

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import pl.defusadr.util.LoadableSystem

interface SettingsManager : LoadableSystem {

  fun getFlag(setting: Settings): Single<Boolean>
  fun getIntSetting(setting: Settings): Single<Int>
  fun getLongSetting(setting: Settings): Single<Long>
  fun getStringSetting(setting: Settings): Single<String>

  fun setFlag(
    setting: Settings,
    value: Boolean
  ): Completable

  fun setSettings(
    setting: Settings,
    value: Int
  ): Completable

  fun setSettings(
    setting: Settings,
    value: Long
  ): Completable

  fun setSettings(
    setting: Settings,
    value: String
  ): Completable
}