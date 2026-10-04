package io.github.supermonster003.autojs6.plugin.three.stack.archive

/** Stable, user-facing reasons why the current archive session cannot modify its contents. */
internal enum class ArchiveManagementReadOnlyReason {
    CURRENT_SESSION_READ_ONLY,
    HOST_REPLACEMENT_UNAVAILABLE,
    FORMAT_NOT_SUPPORTED,
    MULTI_VOLUME_ARCHIVE,
    UNSAFE_ENTRY_PATH,
    PASSWORD_REQUIRED,
    UNSUPPORTED_ENTRY_METHOD,
    BACKEND_VARIANT_READ_ONLY,
}

internal data class ArchiveManagementStatus(
    val capabilities: ArchiveMutationCapabilities?,
    val readOnlyReason: ArchiveManagementReadOnlyReason?,
) {
    val isWritable: Boolean
        get() = capabilities != null && readOnlyReason == null

    init {
        require(capabilities != null || readOnlyReason != null)
    }
}

/**
 * Resolves both the backend's dynamic mutation boundary and the access granted to this Activity.
 * Backend limitations win over session limitations so the information dialog reports the most
 * actionable archive-specific reason.
 */
internal fun ArchiveEngine.managementStatus(
    snapshot: ArchiveSnapshot,
    requestedAction: ArchiveRequestedAction,
    hasHostReplacementSession: Boolean,
): ArchiveManagementStatus {
    val capabilities = mutationCapabilities(snapshot.format)
        ?: return ArchiveManagementStatus(
            capabilities = null,
            readOnlyReason = ArchiveManagementReadOnlyReason.FORMAT_NOT_SUPPORTED,
        )
    val backendAvailability = mutationAvailability(snapshot)
    backendAvailability.unavailableReason?.let { reason ->
        return ArchiveManagementStatus(
            capabilities = capabilities,
            readOnlyReason = reason.toManagementReadOnlyReason(),
        )
    }
    if (requestedAction != ArchiveRequestedAction.MANAGE) {
        return ArchiveManagementStatus(
            capabilities = capabilities,
            readOnlyReason = ArchiveManagementReadOnlyReason.CURRENT_SESSION_READ_ONLY,
        )
    }
    if (!hasHostReplacementSession) {
        return ArchiveManagementStatus(
            capabilities = capabilities,
            readOnlyReason = ArchiveManagementReadOnlyReason.HOST_REPLACEMENT_UNAVAILABLE,
        )
    }
    return ArchiveManagementStatus(capabilities = capabilities, readOnlyReason = null)
}

private fun ArchiveMutationUnavailableReason.toManagementReadOnlyReason():
    ArchiveManagementReadOnlyReason = when (this) {
        ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED ->
            ArchiveManagementReadOnlyReason.FORMAT_NOT_SUPPORTED
        ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE ->
            ArchiveManagementReadOnlyReason.MULTI_VOLUME_ARCHIVE
        ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH ->
            ArchiveManagementReadOnlyReason.UNSAFE_ENTRY_PATH
        ArchiveMutationUnavailableReason.PASSWORD_REQUIRED ->
            ArchiveManagementReadOnlyReason.PASSWORD_REQUIRED
        ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD ->
            ArchiveManagementReadOnlyReason.UNSUPPORTED_ENTRY_METHOD
        ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY ->
            ArchiveManagementReadOnlyReason.BACKEND_VARIANT_READ_ONLY
    }
