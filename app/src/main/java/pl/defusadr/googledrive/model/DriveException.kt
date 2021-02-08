package pl.defusadr.googledrive.model

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.lang.Exception

class DriveException(throwable: Throwable) : Exception(throwable) {
    override val message: String? =
        if (throwable is GoogleJsonResponseException) throwable.details.message
        else throwable.message
}