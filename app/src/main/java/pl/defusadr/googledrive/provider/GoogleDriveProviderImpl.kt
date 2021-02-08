package pl.defusadr.googledrive.provider

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import pl.defusadr.googledrive.service.GoogleDriveService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import pl.defusadr.googledrive.provider.GoogleDriveProvider
import javax.inject.Inject

class GoogleDriveProviderImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val driveService: GoogleDriveService
) : GoogleDriveProvider {

    override fun verifyGooglePlayServices(): Boolean {
        val connectionResult =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        return connectionResult == ConnectionResult.SUCCESS
    }

    override fun verifyGoogleDrivePermissions(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        return GoogleSignIn.hasPermissions(
            account,
            Scope(Scopes.DRIVE_FILE), Scope(Scopes.DRIVE_APPFOLDER)
        )
    }

    override fun requestGoogleDrivePermissions(fragment: Fragment, requestCode: Int) {
        val signInOptions = GoogleSignInOptions.Builder()
            .requestEmail()
            .requestScopes(Scope(Scopes.DRIVE_FILE), Scope(Scopes.DRIVE_APPFOLDER))
            .build()
        val signInIntent = GoogleSignIn.getClient(fragment.requireContext(), signInOptions).signInIntent

        fragment.startActivityForResult(signInIntent, requestCode)
    }

    override fun initDriveService(applicationContext: Context, intent: Intent?): Single<Unit> =
        getSignInAccountSingle(applicationContext, intent)
            .map {
                val credential = GoogleAccountCredential.usingOAuth2(
                    applicationContext,
                    listOf(Scopes.DRIVE_FILE, Scopes.DRIVE_APPFOLDER)
                ).apply {
                    selectedAccount = it.account
                }

                Drive.Builder(
                    NetHttpTransport.Builder().build(),
                    GsonFactory(),
                    credential
                )
                    .build()
            }.flatMap {
                Single.just(driveService.init(it))
            }

    private fun getSignInAccountSingle(
        context: Context,
        intent: Intent? = null
    ): Single<GoogleSignInAccount> =
        if (intent != null) getSignInAccountFromSignInResult(intent)
        else Single.just(GoogleSignIn.getLastSignedInAccount(context))

    private fun getSignInAccountFromSignInResult(result: Intent): Single<GoogleSignInAccount> =
        Single.create<GoogleSignInAccount> { emitter ->
            GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener {
                    emitter.onSuccess(it)
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }

            .observeOn(Schedulers.io())
}
