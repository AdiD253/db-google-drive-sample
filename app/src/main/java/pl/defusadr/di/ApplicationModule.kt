package pl.defusadr.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.defusadr.googledrive.provider.GoogleDriveProvider
import pl.defusadr.googledrive.provider.GoogleDriveProviderImpl
import pl.defusadr.googledrive.service.GoogleDriveService
import pl.defusadr.googledrive.service.GoogleDriveServiceImpl
import pl.defusadr.backup.manager.DatabaseBackupManagerImpl
import pl.defusadr.backup.manager.DatabaseBackupManager
import pl.defusadr.settings.SettingsManagerImpl
import pl.defusadr.settings.SettingsManager
import pl.defusadr.settings.defaults.SettingsDefaultsProviderImpl
import pl.defusadr.settings.defaults.SettingsDefaultsProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

  @Provides
  @Singleton
  fun providesMoshi(): Moshi =
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()

  @Provides
  @Singleton
  fun providesGoogleDriveServiceHelper(serviceHelper: GoogleDriveServiceImpl): GoogleDriveService =
    serviceHelper

  @Provides
  @Singleton
  fun provideGoogleDriveManager(driveManager: GoogleDriveProviderImpl): GoogleDriveProvider =
    driveManager

  @Provides
  @Singleton
  internal fun provideDatabaseBackupManager(
    manager: DatabaseBackupManagerImpl
  ): DatabaseBackupManager = manager

  @Provides
  @Singleton
  fun providesSettingsManager(settingsManager: SettingsManagerImpl): SettingsManager =
    settingsManager

  @Provides
  @Singleton
  fun providesDefaultSettings(defaultSettings: SettingsDefaultsProviderImpl): SettingsDefaultsProvider =
    defaultSettings
}