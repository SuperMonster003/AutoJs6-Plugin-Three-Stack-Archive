package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Intent
import java.util.UUID

/** Private entry from the system document picker; host-only mutations remain negotiated. */
class StandaloneArchiveActivity : ArchiveManagerActivity() {
    internal override fun resolveRequest(): ArchiveOpenRequest? = runCatching {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        require(uri.scheme == "content" && !uri.authority.isNullOrBlank())
        val name = ArchiveIntentPolicy.validateDisplayName(intent.getStringExtra(EXTRA_NAME)) ?: return null
        val size = intent.getLongExtra(EXTRA_SIZE, Long.MIN_VALUE)
        require(ArchiveIntentPolicy.isSupportedArchive(intent.type, name))
        require(ArchiveIntentPolicy.isReportedSizeAccepted(size))
        ArchiveOpenRequest(uri, uri, name, UUID.randomUUID().toString(),
            name, UUID.randomUUID().toString(), size, ArchiveRequestedAction.OPEN, null)
    }.getOrNull()

    companion object {
        const val EXTRA_NAME = "io.github.supermonster003.autojs6.plugin.three.stack.archive.extra.LOCAL_NAME"
        const val EXTRA_SIZE = "io.github.supermonster003.autojs6.plugin.three.stack.archive.extra.LOCAL_SIZE"
    }
}
