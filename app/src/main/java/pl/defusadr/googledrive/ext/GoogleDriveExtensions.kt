package pl.defusadr.googledrive.ext

import pl.defusadr.googledrive.model.DriveException
import com.google.android.gms.tasks.Task
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

fun <T> Task<T>.mapToSingle(): Single<T> =
    Single.create<T> { emitter ->
        this
            .addOnSuccessListener {
                emitter.onSuccess(it)
            }
            .addOnFailureListener {
                emitter.onError(DriveException(it))
            }
    }
        .observeOn(Schedulers.io())

fun <T, R> Task<T>.mapToSingle(mapper: (item: T) -> R): Single<R> =
    Single.create<R> { emitter ->
        this
            .addOnSuccessListener {
                emitter.onSuccess(mapper.invoke(it))
            }
            .addOnFailureListener {
                emitter.onError(DriveException(it))
            }
    }
        .observeOn(Schedulers.io())

fun <T> Task<T>.mapToCompletable(): Completable =
    Completable.create { emitter ->
        this
            .addOnSuccessListener {
                emitter.onComplete()
            }
            .addOnFailureListener {
                emitter.onError(DriveException(it))
            }
    }