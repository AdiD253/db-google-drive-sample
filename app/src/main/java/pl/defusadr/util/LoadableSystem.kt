package pl.defusadr.util

import io.reactivex.rxjava3.core.Single

interface LoadableSystem {

  fun load(): Single<Boolean>

}