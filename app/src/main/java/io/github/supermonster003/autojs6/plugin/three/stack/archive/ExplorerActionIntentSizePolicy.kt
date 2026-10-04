package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Intent
import android.os.Parcel

/** Defense-in-depth validation for Explorer Action activity envelopes received over Binder. */
internal object ExplorerActionIntentSizePolicy {

    fun isSafe(intent: Intent): Boolean = runCatching {
        val parcel = Parcel.obtain()
        try {
            intent.writeToParcel(parcel, 0)
            !parcel.hasFileDescriptors() && parcel.dataSize() <= MAX_REQUEST_BYTES
        } finally {
            parcel.recycle()
        }
    }.getOrDefault(false)

    /** Matches the wire-compatible host v16 policy and intentionally stays below 1 MiB. */
    const val MAX_REQUEST_BYTES = 512 * 1024
}
