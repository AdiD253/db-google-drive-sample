package pl.defusadr.backup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import javax.inject.Inject

class DatabaseBackupLifecycleObserver @Inject constructor(
    private val contentChangesObserver: DatabaseBackupUseCase.ObserveContentChanges
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun observeDatabaseContent() {
        contentChangesObserver.executeStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopObservingDatabaseContent() {
        contentChangesObserver.executeStop()
    }
}