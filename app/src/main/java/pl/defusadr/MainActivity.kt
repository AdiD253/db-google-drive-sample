package pl.defusadr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import pl.defusadr.backup.DatabaseBackupLifecycleObserver
import pl.defusadr.backup.DatabaseBackupUseCase
import pl.defusadr.defusadr.R
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject
  lateinit var databaseBackupLifecycleObserver: DatabaseBackupLifecycleObserver

  @Inject
  lateinit var importDatabaseUseCase: DatabaseBackupUseCase.ImportDatabase

  @Inject
  lateinit var exportDatabaseUseCase: DatabaseBackupUseCase.ExportDatabase

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    lifecycle.addObserver(databaseBackupLifecycleObserver)
  }
}