package io.github.supermonster003.autojs6.plugin.archivemanager

/** Prevents opaque session entry IDs from being reused against a changed archive after rescanning. */
internal object ArchiveRetrySnapshotValidator {

    fun requireSameArchive(original: ArchiveSnapshot, retried: ArchiveSnapshot) {
        val sameArchive = original.sourceLength == retried.sourceLength &&
            original.sourceLastModifiedMillis == retried.sourceLastModifiedMillis &&
            original.volumeIdentities == retried.volumeIdentities &&
            original.totalUncompressedBytes == retried.totalUncompressedBytes &&
            original.structureLimits == retried.structureLimits &&
            original.format == retried.format &&
            original.isolatedPathRoot == retried.isolatedPathRoot &&
            original.readerOptions.filenameCharsetName == retried.readerOptions.filenameCharsetName &&
            original.entries.size == retried.entries.size &&
            original.entries.zip(retried.entries).all { (before, after) ->
                before.sameStableMetadataAs(after)
            }
        if (!sameArchive) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_CHANGED,
                "Archive metadata changed before the extraction retry",
            )
        }
    }

    private fun ArchiveEntry.sameStableMetadataAs(other: ArchiveEntry): Boolean =
        path == other.path &&
            sourceName == other.sourceName &&
            displayName == other.displayName &&
            isDirectory == other.isDirectory &&
            pathStatus == other.pathStatus &&
            compressionMethod == other.compressionMethod &&
            compressionMethodId == other.compressionMethodId &&
            isEncrypted == other.isEncrypted &&
            encryptionMethod == other.encryptionMethod &&
            compressedSize == other.compressedSize &&
            uncompressedSize == other.uncompressedSize &&
            crc32 == other.crc32 &&
            modifiedTimeMillis == other.modifiedTimeMillis &&
            ordinal == other.ordinal &&
            capabilities.sameStableMetadataAs(other.capabilities, isEncrypted)

    private fun ArchiveEntryCapabilities.sameStableMetadataAs(
        other: ArchiveEntryCapabilities,
        passwordMayChangeReadability: Boolean,
    ): Boolean = canDelete == other.canDelete &&
        canRename == other.canRename &&
        limitations == other.limitations &&
        (passwordMayChangeReadability || canOpen == other.canOpen) &&
        (passwordMayChangeReadability || canExtract == other.canExtract)
}
