package io.github.supermonster003.autojs6.plugin.archivemanager

internal data class ArchiveCreationCounters(
    var files: Long = 0L,
    var directories: Long = 0L,
    var bytesRead: Long = 0L,
)

internal fun ArchiveCreationProgressListener.reportScanStarted() {
    onProgress(
        ArchiveCreationProgress(
            phase = ArchiveCreationPhase.SCANNING,
            currentEntry = null,
            completedFiles = 0L,
            completedDirectories = 0L,
            sourceBytesRead = 0L,
            totalFiles = 0L,
            totalDirectories = 0L,
            knownSourceBytes = 0L,
            unknownSizeFiles = 0L,
        ),
    )
}

internal fun ArchiveCreationProgressListener.reportScan(update: ArchiveSourceScanProgress) {
    onProgress(
        ArchiveCreationProgress(
            phase = ArchiveCreationPhase.SCANNING,
            currentEntry = update.currentEntry,
            completedFiles = update.discoveredFiles,
            completedDirectories = update.discoveredDirectories,
            sourceBytesRead = 0L,
            totalFiles = update.discoveredFiles,
            totalDirectories = update.discoveredDirectories,
            knownSourceBytes = update.knownSourceBytes,
            unknownSizeFiles = update.unknownSizeFiles,
        ),
    )
}

internal fun ArchiveCreationProgressListener.reportCompression(
    manifest: ArchiveSourceManifest,
    counters: ArchiveCreationCounters,
    currentEntry: String?,
) = reportCreationPhase(
    phase = ArchiveCreationPhase.COMPRESSING,
    manifest = manifest,
    counters = counters,
    currentEntry = currentEntry,
)

internal fun ArchiveCreationProgressListener.reportCommitting(
    manifest: ArchiveSourceManifest,
    counters: ArchiveCreationCounters,
) = reportCreationPhase(
    phase = ArchiveCreationPhase.COMMITTING,
    manifest = manifest,
    counters = counters,
    currentEntry = null,
)

private fun ArchiveCreationProgressListener.reportCreationPhase(
    phase: ArchiveCreationPhase,
    manifest: ArchiveSourceManifest,
    counters: ArchiveCreationCounters,
    currentEntry: String?,
) {
    onProgress(
        ArchiveCreationProgress(
            phase = phase,
            currentEntry = currentEntry,
            completedFiles = counters.files,
            completedDirectories = counters.directories,
            sourceBytesRead = counters.bytesRead,
            totalFiles = manifest.fileCount,
            totalDirectories = manifest.directoryCount,
            knownSourceBytes = manifest.knownSourceBytes,
            unknownSizeFiles = manifest.unknownSizeFileCount,
        ),
    )
}
