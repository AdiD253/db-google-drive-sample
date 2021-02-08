package pl.defusadr.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pl.defusadr.db.dao.BackupDao
import pl.defusadr.db.entity.SampleEntity

@Database(
  entities = [
    SampleEntity::class
  ],
  version = 1,
  exportSchema = false
)
internal abstract class AppRoomDatabase : RoomDatabase() {

  abstract fun backupDao(): BackupDao

  companion object {
    private const val DB_NAME = "app_database.db"
    private lateinit var instance: AppRoomDatabase
    fun getInstance(context: Context): AppRoomDatabase =
      if (this::instance.isInitialized) instance
      else
        Room.databaseBuilder(
          context,
          AppRoomDatabase::class.java,
          DB_NAME
        )
          .fallbackToDestructiveMigration()
          .build()
          .apply {
            instance = this
          }
  }
}