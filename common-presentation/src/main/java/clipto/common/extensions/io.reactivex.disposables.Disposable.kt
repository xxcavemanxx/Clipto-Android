package clipto.common.extensions

import io.reactivex.disposables.Disposable

fun Disposable?.disposeSilently() {
    runCatching { this?.takeIf { !it.isDisposed }?.dispose() }
}

fun Disposable?.isNullOrDisposed(): Boolean = this == null || isDisposed