package pl.defusadr.googledrive.provider

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.core.Single

interface GoogleDriveProvider {

  /**
   * Verifies whether Google Play Services are present on the device
   */
  fun verifyGooglePlayServices(): Boolean

  /**
   * Verifies if required Google Drive permissions has been granted
   * **/
  fun verifyGoogleDrivePermissions(): Boolean

  /**
   * Requests required Google Drive permissions
   * @param fragment - fragment that requests for permissions
   * @param requestCode - request code for granting permissions, appears in onActivityResult()
   */
  fun requestGoogleDrivePermissions(fragment: Fragment, requestCode: Int)

  /**
   * Initializes GoogleDriveService
   * @return
   */
  fun initDriveService(applicationContext: Context, intent: Intent?): Single<Unit>
}