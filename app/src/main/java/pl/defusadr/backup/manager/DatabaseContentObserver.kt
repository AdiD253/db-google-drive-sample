package pl.defusadr.backup.manager

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import pl.defusadr.backup.BackupTable
import pl.defusadr.db.dao.BackupDao
import pl.defusadr.util.subscribeAfterFirst

typealias onTableUpdated = (table: BackupTable) -> Unit

internal class DatabaseContentObserver(
  private val backupDao: BackupDao
) {

  private lateinit var disposable: CompositeDisposable

  fun start(onContentUpdated: onTableUpdated) {
    disposable = CompositeDisposable()
    disposable.addAll(
      observeSampleItems(onContentUpdated)
    )
  }

  fun stop() {
    disposable.dispose()
  }

  private fun observeSampleItems(onUpdated: onTableUpdated): Disposable =
    backupDao.subscribeToSamplesChanges()
      .subscribeOn(Schedulers.io())
      .subscribeAfterFirst {
        onUpdated.invoke(BackupTable.SAMPLE)
      }
}