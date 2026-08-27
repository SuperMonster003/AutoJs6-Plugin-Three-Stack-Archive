package io.github.supermonster003.autojs6.plugin.archivemanager

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import java.security.MessageDigest

/** Debug-only cross-process probe used by the AutoJs6 host URI grant lifecycle gate. */
class UriGrantProbeActivity : Activity() {

    private var callback: Messenger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callback = callbackExtra()

        val digests = ArrayList<String>()
        val failures = ArrayList<String>()
        clipUris().forEach { uri ->
            runCatching { digest(uri) }
                .onSuccess(digests::add)
                .onFailure { error -> failures += error.javaClass.name }
        }
        send(
            MESSAGE_READ_COMPLETE,
            Bundle().apply {
                putStringArrayList(KEY_DIGESTS, digests)
                putStringArrayList(KEY_FAILURES, failures)
            },
        )
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        send(MESSAGE_ACTIVITY_DESTROYED, Bundle.EMPTY)
        callback = null
        super.onDestroy()
    }

    private fun send(what: Int, data: Bundle) {
        runCatching {
            callback?.send(Message.obtain(null, what).apply { this.data = data })
        }
    }

    private fun clipUris(): List<Uri> {
        val clipData = intent.clipData ?: return emptyList()
        return List(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
    }

    private fun digest(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = checkNotNull(contentResolver.openInputStream(uri)) {
            "No stream was returned for $uri"
        }
        input.use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    @Suppress("DEPRECATION")
    private fun callbackExtra(): Messenger? = intent.getParcelableExtra(EXTRA_CALLBACK)

    companion object {
        const val EXTRA_CALLBACK =
            "io.github.supermonster003.autojs6.plugin.archivemanager.debug.CALLBACK"
        const val MESSAGE_READ_COMPLETE = 1
        const val MESSAGE_ACTIVITY_DESTROYED = 2
        const val KEY_DIGESTS = "digests"
        const val KEY_FAILURES = "failures"
    }
}
