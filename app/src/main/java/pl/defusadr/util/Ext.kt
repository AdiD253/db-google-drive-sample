package pl.defusadr.util

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Single
import java.text.SimpleDateFormat
import java.util.*

inline fun <reified T> T.toJson(moshi: Moshi): String = moshi.adapter(T::class.java).toJson(this)

@Throws(IllegalStateException::class)
inline fun <reified T> String.fromJson(moshi: Moshi): T =
  moshi.adapter(T::class.java).fromJson(this)
    ?: throw IllegalStateException("Cannot convert $this to type ${T::class.java.simpleName}")

private val onNextStub: (Any) -> Unit = {}
private val onErrorStub: (Throwable) -> Unit = {}
private val onCompleteStub: () -> Unit = {}

fun <T, R> Single<List<T>>.mapEach(convert: (item: T) -> R): Single<List<R>> =
  map { it.map { item -> convert.invoke(item) } }

fun <T : Any> Flowable<T>.subscribeAfterFirst(
  onError: (Throwable) -> Unit = onErrorStub,
  onComplete: () -> Unit = onCompleteStub,
  onNext: (T) -> Unit = onNextStub
): Disposable = this.skip(1)
  .subscribeBy(onError, onComplete, onNext)

fun Date.format(pattern: String): String {
  val format = SimpleDateFormat(pattern, Locale("pl", "PL"))
  return format.format(this)
}