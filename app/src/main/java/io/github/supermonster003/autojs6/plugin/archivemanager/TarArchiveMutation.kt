@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.tukaani.xz.LZMA2Options
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal val TAR_MUTATION_CAPABILITIES = ArchiveMutationCapabilities(
    operations = setOf(
        ArchiveOperation.ADD,
        ArchiveOperation.DELETE,
        ArchiveOperation.RENAME,
    ),
    strategy = ArchiveMutationStrategy.FULL_REWRITE,
    minimumHostProtocolVersion = 8,
    metadataEffects = setOf(
        ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
        ArchiveMutationMetadataEffect.UNIX_ATTRIBUTES_DROPPED,
    ),
)

internal val TAR_GZIP_MUTATION_CAPABILITIES = TAR_MUTATION_CAPABILITIES.copy(
    metadataEffects = TAR_MUTATION_CAPABILITIES.metadataEffects +
        ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
)

internal val TAR_XZ_MUTATION_CAPABILITIES = TAR_MUTATION_CAPABILITIES.copy(
    metadataEffects = TAR_MUTATION_CAPABILITIES.metadataEffects +
        ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
)

internal const val TAR_XZ_MUTATION_COMPRESSION_LEVEL = 4
internal const val TAR_XZ_MUTATION_MAX_ENCODER_MEMORY_KIB = 64 * 1_024

internal fun tarMutationCompressionLevel(format: ArchiveFormat): Int = when (format) {
    ArchiveFormat.TAR -> 0
    ArchiveFormat.TAR_GZIP -> 6
    ArchiveFormat.TAR_XZ -> TAR_XZ_MUTATION_COMPRESSION_LEVEL.also { level ->
        val encoderMemoryUsageKiB = LZMA2Options(level).encoderMemoryUsage
        check(encoderMemoryUsageKiB <= TAR_XZ_MUTATION_MAX_ENCODER_MEMORY_KIB) {
            "TAR.XZ mutation encoder requires $encoderMemoryUsageKiB KiB"
        }
    }
    ArchiveFormat.ZIP,
    ArchiveFormat.SEVEN_Z,
    ArchiveFormat.RAR,
    ArchiveFormat.TAR_BZIP2,
    ArchiveFormat.TAR_ZSTD,
    -> error("${format.displayName} does not have a TAR mutation compression level")
}

internal fun tarMutationCapabilities(format: ArchiveFormat): ArchiveMutationCapabilities? =
    when (format) {
        ArchiveFormat.TAR -> TAR_MUTATION_CAPABILITIES
        ArchiveFormat.TAR_GZIP -> TAR_GZIP_MUTATION_CAPABILITIES
        ArchiveFormat.TAR_XZ -> TAR_XZ_MUTATION_CAPABILITIES
        ArchiveFormat.ZIP,
        ArchiveFormat.SEVEN_Z,
        ArchiveFormat.RAR,
        ArchiveFormat.TAR_BZIP2,
        ArchiveFormat.TAR_ZSTD,
        -> null
    }

internal fun tarMutationAvailability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
    when {
        tarMutationCapabilities(snapshot.format) == null -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
        )
        snapshot.volumeIdentities.isNotEmpty() -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE,
        )
        snapshot.entries.any { it.pathStatus != ArchiveEntryPathStatus.SAFE } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH,
            )
        snapshot.entries.any {
            !it.isDirectory &&
                (!it.capabilities.canOpen || it.compressionMethodId != TAR_REGULAR_FILE_METHOD)
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
        )
        snapshot.entries.any {
            !it.capabilities.canDelete || !it.capabilities.canRename
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
        )
        else -> ArchiveMutationAvailability.available(
            requireNotNull(tarMutationCapabilities(snapshot.format)),
        )
    }

internal data class TarArchiveMutationPlan(
    val entries: List<TarArchiveMutationEntry>,
    override val format: ArchiveFormat,
    override val operation: ArchiveOperation,
    override val sourceVersion: ArchiveMutationSourceVersion,
    override val workEstimate: ArchiveMutationWorkEstimate,
    override val metadataEffects: Set<ArchiveMutationMetadataEffect>,
) : PreparedArchiveMutation

internal data class TarArchiveMutationEntry(
    val archivePath: String,
    val source: TarArchiveMutationSource,
) {
    val isDirectory: Boolean
        get() = source is TarArchiveMutationSource.AddedDirectory ||
            (source as? TarArchiveMutationSource.Existing)?.entry?.isDirectory == true

    val size: Long
        get() = when (source) {
            is TarArchiveMutationSource.Existing -> source.entry.uncompressedSize
            is TarArchiveMutationSource.AddedFile -> source.file.size
            is TarArchiveMutationSource.AddedDirectory -> 0L
        }

    val lastModified: Long
        get() = when (source) {
            is TarArchiveMutationSource.Existing -> source.entry.modifiedTimeMillis ?: -1L
            is TarArchiveMutationSource.AddedFile -> source.file.lastModified
            is TarArchiveMutationSource.AddedDirectory -> source.lastModified
        }
}

internal sealed interface TarArchiveMutationSource {
    data class Existing(val entry: ArchiveEntry) : TarArchiveMutationSource
    data class AddedFile(val file: ArchiveMutationAddedFile) : TarArchiveMutationSource
    data class AddedDirectory(val lastModified: Long = -1L) : TarArchiveMutationSource
}

/** Produces a complete, collision-free replacement directory before host output reservation. */
internal object TarArchiveMutationPlanner {

    fun plan(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): TarArchiveMutationPlan {
        val capabilities = requireNotNull(tarMutationCapabilities(snapshot.format)) {
            "${snapshot.format.displayName} does not have a TAR mutation provider"
        }
        val original = snapshot.entries.map { entry ->
            TarArchiveMutationEntry(entry.path, TarArchiveMutationSource.Existing(entry))
        }
        val entries = when (request) {
            is ArchiveMutationRequest.Delete -> delete(original, request.paths, snapshot)
            is ArchiveMutationRequest.Rename -> rename(original, request, snapshot)
            is ArchiveMutationRequest.AddDirectory -> addDirectory(original, request, snapshot)
            is ArchiveMutationRequest.AddFiles -> addFiles(original, request, snapshot)
            is ArchiveMutationRequest.AddTree -> addTree(original, request, snapshot)
        }
        validateFinalEntries(entries, snapshot.structureLimits, snapshot.format)
        entries.forEach { requireRewritableEntry(it, snapshot.format) }
        return TarArchiveMutationPlan(
            entries = entries.toList(),
            format = snapshot.format,
            operation = request.operation,
            sourceVersion = ArchiveMutationSourceVersion.capture(snapshot),
            workEstimate = workEstimate(snapshot, entries),
            metadataEffects = capabilities.metadataEffects,
        )
    }

    private fun delete(
        original: List<TarArchiveMutationEntry>,
        requestedPaths: Set<String>,
        snapshot: ArchiveSnapshot,
    ): List<TarArchiveMutationEntry> {
        if (requestedPaths.isEmpty()) {
            fail(ArchiveFailureCode.EMPTY_SELECTION, "No TAR entries selected", snapshot.format)
        }
        val paths = requestedPaths.mapTo(linkedSetOf()) { requested ->
            validatedSelectionPath(requested, snapshot)
        }
        paths.forEach { path ->
            if (!pathExists(original, path)) {
                fail(
                    ArchiveFailureCode.UNKNOWN_SELECTION,
                    "The selected TAR entry no longer exists",
                    snapshot.format,
                )
            }
        }
        return original.filterNot { planned -> paths.any { planned.archivePath.isAtOrBelow(it) } }
    }

    private fun rename(
        original: List<TarArchiveMutationEntry>,
        request: ArchiveMutationRequest.Rename,
        snapshot: ArchiveSnapshot,
    ): List<TarArchiveMutationEntry> {
        val sourcePath = validatedSelectionPath(request.path, snapshot)
        if (!pathExists(original, sourcePath)) {
            fail(
                ArchiveFailureCode.UNKNOWN_SELECTION,
                "The selected TAR entry no longer exists",
                snapshot.format,
            )
        }
        val newLeaf = requireLeafName(request.newDisplayName, snapshot.format)
        val parent = sourcePath.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
        val targetPath = portableChildPath(parent, newLeaf, snapshot.structureLimits)
        if (targetPath == sourcePath) {
            fail(
                ArchiveFailureCode.INVALID_DESTINATION_NAME,
                "The TAR entry name did not change",
                snapshot.format,
            )
        }
        return original.map { planned ->
            if (planned.archivePath.isAtOrBelow(sourcePath)) {
                val suffix = planned.archivePath.removePrefix(sourcePath)
                planned.copy(archivePath = targetPath + suffix)
            } else {
                planned
            }
        }
    }

    private fun addDirectory(
        original: List<TarArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddDirectory,
        snapshot: ArchiveSnapshot,
    ): List<TarArchiveMutationEntry> {
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        val path = portableChildPath(
            parent,
            requireLeafName(request.displayName, snapshot.format),
            snapshot.structureLimits,
        )
        if (pathExists(original, path)) {
            fail(
                ArchiveFailureCode.DUPLICATE_PATH,
                "A TAR entry with this name already exists",
                snapshot.format,
            )
        }
        return original + TarArchiveMutationEntry(path, TarArchiveMutationSource.AddedDirectory())
    }

    private fun addFiles(
        original: List<TarArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddFiles,
        snapshot: ArchiveSnapshot,
    ): List<TarArchiveMutationEntry> {
        if (request.files.isEmpty()) {
            fail(ArchiveFailureCode.EMPTY_SELECTION, "No files selected", snapshot.format)
        }
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        val additions = request.files.map { file ->
            val path = portableChildPath(
                parent,
                requireLeafName(file.displayName, snapshot.format),
                snapshot.structureLimits,
            )
            TarArchiveMutationEntry(path, TarArchiveMutationSource.AddedFile(file))
        }
        return original + additions
    }

    private fun addTree(
        original: List<TarArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddTree,
        snapshot: ArchiveSnapshot,
    ): List<TarArchiveMutationEntry> {
        if (request.entries.isEmpty()) {
            fail(
                ArchiveFailureCode.EMPTY_SELECTION,
                "The selected folder is empty or unavailable",
                snapshot.format,
            )
        }
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        val validated = request.entries.map { added ->
            val path = ArchivePathPolicy.validateEntryPath(
                sourceName = added.relativePath,
                isDirectory = added is ArchiveMutationAddedTreeEntry.Directory,
                limits = snapshot.structureLimits,
            ).path
            if (path != added.relativePath) {
                fail(
                    ArchiveFailureCode.INVALID_PATH,
                    "The selected folder contains an ambiguous path",
                    snapshot.format,
                )
            }
            if (
                added is ArchiveMutationAddedTreeEntry.FileEntry &&
                added.file.displayName != path.substringAfterLast('/')
            ) {
                fail(
                    ArchiveFailureCode.INVALID_DESTINATION_NAME,
                    "A selected file name changed while scanning",
                    snapshot.format,
                )
            }
            path to added
        }
        val sourceRoots = validated.mapTo(linkedSetOf()) { (path, _) -> path.substringBefore('/') }
        if (sourceRoots.size != 1) {
            fail(
                ArchiveFailureCode.INVALID_PATH,
                "The selected folder does not have one stable root",
                snapshot.format,
            )
        }
        val sourceRoot = sourceRoots.single()
        val directories = validated
            .filter { (_, added) -> added is ArchiveMutationAddedTreeEntry.Directory }
            .mapTo(hashSetOf()) { (path, _) -> path }
        if (sourceRoot !in directories) {
            fail(
                ArchiveFailureCode.INVALID_PATH,
                "The selected folder root is missing",
                snapshot.format,
            )
        }
        validated.forEach { (path, _) ->
            val immediateParent = path.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            if (immediateParent.isNotEmpty() && immediateParent !in directories) {
                fail(
                    ArchiveFailureCode.INVALID_PATH,
                    "The selected folder tree has a missing directory",
                    snapshot.format,
                )
            }
        }

        val targetRoot = availableChildName(
            original = original,
            parent = parent,
            requested = sourceRoot,
            limits = snapshot.structureLimits,
            format = snapshot.format,
        )
        val additions = validated.map { (relativePath, added) ->
            val remappedRelativePath = targetRoot + relativePath.removePrefix(sourceRoot)
            val archivePath = if (parent.isEmpty()) {
                remappedRelativePath
            } else {
                "$parent/$remappedRelativePath"
            }
            val source = when (added) {
                is ArchiveMutationAddedTreeEntry.Directory ->
                    TarArchiveMutationSource.AddedDirectory(added.lastModified)
                is ArchiveMutationAddedTreeEntry.FileEntry ->
                    TarArchiveMutationSource.AddedFile(added.file)
            }
            TarArchiveMutationEntry(archivePath, source)
        }
        return original + additions
    }

    private fun availableChildName(
        original: List<TarArchiveMutationEntry>,
        parent: String,
        requested: String,
        limits: ArchiveStructureLimits,
        format: ArchiveFormat,
    ): String {
        val occupiedKeys = original.mapNotNullTo(hashSetOf()) { entry ->
            val relative = when {
                parent.isEmpty() -> entry.archivePath
                entry.archivePath.startsWith("$parent/") -> entry.archivePath.removePrefix("$parent/")
                else -> return@mapNotNullTo null
            }
            relative.substringBefore('/').takeIf(String::isNotEmpty)
                ?.let(ArchivePathPolicy::destinationCollisionKey)
        }
        if (ArchivePathPolicy.destinationCollisionKey(requested) !in occupiedKeys) return requested
        for (index in 2..MAX_AUTO_RENAME_ATTEMPTS) {
            val candidate = "$requested ($index)"
            requireLeafName(candidate, format)
            portableChildPath(parent, candidate, limits)
            if (ArchivePathPolicy.destinationCollisionKey(candidate) !in occupiedKeys) return candidate
        }
        fail(
            ArchiveFailureCode.DUPLICATE_PATH,
            "No available TAR folder name could be reserved",
            format,
        )
    }

    private fun validatedParentPath(
        requested: String,
        original: List<TarArchiveMutationEntry>,
        snapshot: ArchiveSnapshot,
    ): String {
        if (requested.isEmpty()) return ArchivePathPolicy.ROOT_PATH
        val path = validatedSelectionPath(requested, snapshot)
        val explicitFile = original.firstOrNull { it.archivePath == path && !it.isDirectory }
        if (explicitFile != null || !pathExists(original, path)) {
            fail(
                ArchiveFailureCode.UNKNOWN_SELECTION,
                "The destination TAR folder no longer exists",
                snapshot.format,
            )
        }
        return path
    }

    private fun validatedSelectionPath(path: String, snapshot: ArchiveSnapshot): String {
        val normalized = ArchivePathPolicy.normalizeSelectionPath(
            path = path,
            limits = snapshot.structureLimits,
            allowRoot = false,
        )
        if (snapshot.isIsolatedPath(normalized)) {
            fail(
                ArchiveFailureCode.INVALID_PATH,
                "Unsafe TAR paths cannot be rewritten",
                snapshot.format,
            )
        }
        return normalized
    }

    private fun portableChildPath(
        parent: String,
        leaf: String,
        limits: ArchiveStructureLimits,
    ): String = ArchivePathPolicy.validateEntryPath(
        sourceName = if (parent.isEmpty()) leaf else "$parent/$leaf",
        isDirectory = false,
        limits = limits,
    ).path

    private fun requireLeafName(value: String, format: ArchiveFormat): String =
        ArchiveIntentPolicy.validateDisplayName(value)
            ?: fail(
                ArchiveFailureCode.INVALID_DESTINATION_NAME,
                "The TAR entry name is invalid",
                format,
            )

    private fun pathExists(entries: List<TarArchiveMutationEntry>, path: String): Boolean =
        entries.any { it.archivePath.isAtOrBelow(path) }

    private fun validateFinalEntries(
        entries: List<TarArchiveMutationEntry>,
        limits: ArchiveStructureLimits,
        format: ArchiveFormat,
    ) {
        if (entries.size > limits.maxEntries) {
            fail(
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                "The rewritten TAR has too many entries",
                format,
            )
        }
        val explicitTypes = HashMap<String, Boolean>(entries.size)
        entries.forEach { entry ->
            val path = ArchivePathPolicy.validateEntryPath(
                sourceName = entry.archivePath,
                isDirectory = entry.isDirectory,
                limits = limits,
            ).path
            val previousType = explicitTypes.putIfAbsent(path, entry.isDirectory)
            if (previousType != null) {
                fail(
                    if (previousType == entry.isDirectory) {
                        ArchiveFailureCode.DUPLICATE_PATH
                    } else {
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT
                    },
                    "The rewritten TAR contains a duplicate or conflicting path",
                    format,
                )
            }
        }
        entries.forEach { entry ->
            var parent = entry.archivePath.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            while (parent.isNotEmpty()) {
                if (explicitTypes[parent] == false) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "A rewritten TAR file is also used as a directory",
                        format,
                    )
                }
                parent = parent.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            }
        }
        val portableEntries = entries.groupBy { entry ->
            ArchivePathPolicy.destinationCollisionKey(entry.archivePath)
        }
        portableEntries.values.forEach { equivalentEntries ->
            if (
                equivalentEntries.size > 1 &&
                equivalentEntries.any { !it.isUnchangedExistingPath() }
            ) {
                fail(
                    if (equivalentEntries.map(TarArchiveMutationEntry::isDirectory).distinct().size > 1) {
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT
                    } else {
                        ArchiveFailureCode.DUPLICATE_PATH
                    },
                    "The TAR change introduces case-insensitive or Unicode-equivalent names",
                    format,
                )
            }
        }
        entries.forEach { entry ->
            var parent = entry.archivePath.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            while (parent.isNotEmpty()) {
                val portableParent = ArchivePathPolicy.destinationCollisionKey(parent)
                val equivalentFiles = portableEntries[portableParent].orEmpty().filterNot {
                    it.isDirectory
                }
                if (
                    equivalentFiles.isNotEmpty() &&
                    (!entry.isUnchangedExistingPath() || equivalentFiles.any { !it.isUnchangedExistingPath() })
                ) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "The TAR change uses a case-insensitive or Unicode-equivalent file as a directory",
                        format,
                    )
                }
                parent = parent.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            }
        }
    }

    private fun TarArchiveMutationEntry.isUnchangedExistingPath(): Boolean =
        (source as? TarArchiveMutationSource.Existing)?.entry?.path == archivePath

    private fun requireRewritableEntry(
        planned: TarArchiveMutationEntry,
        format: ArchiveFormat,
    ) {
        val entry = (planned.source as? TarArchiveMutationSource.Existing)?.entry ?: return
        if (entry.pathStatus != ArchiveEntryPathStatus.SAFE) {
            fail(
                ArchiveFailureCode.INVALID_PATH,
                "Unsafe TAR paths cannot be preserved by rewriting",
                format,
            )
        }
        if (entry.isDirectory) return
        if (!entry.capabilities.canOpen || entry.compressionMethodId != TAR_REGULAR_FILE_METHOD) {
            fail(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "A retained TAR entry type cannot be rewritten safely",
                format,
            )
        }
    }

    private fun String.isAtOrBelow(parent: String): Boolean =
        this == parent || startsWith("$parent/")

    private fun workEstimate(
        snapshot: ArchiveSnapshot,
        entries: List<TarArchiveMutationEntry>,
    ): ArchiveMutationWorkEstimate {
        var files = 0
        var directories = 0
        var knownBytes = 0L
        var unknownFiles = 0
        entries.forEach { entry ->
            if (entry.isDirectory) {
                directories++
            } else {
                files++
                if (entry.size >= 0L) {
                    knownBytes = try {
                        Math.addExact(knownBytes, entry.size)
                    } catch (error: ArithmeticException) {
                        fail(
                            ArchiveFailureCode.MALFORMED_ARCHIVE,
                            "Rewritten TAR size metadata overflows",
                            snapshot.format,
                        )
                    }
                } else {
                    unknownFiles++
                }
            }
        }
        return ArchiveMutationWorkEstimate(
            sourceArchiveBytes = snapshot.sourceLength,
            resultEntryCount = entries.size,
            resultFileCount = files,
            resultDirectoryCount = directories,
            knownContentBytesToRead = knownBytes,
            unknownContentFileCount = unknownFiles,
        )
    }

    private fun fail(
        code: ArchiveFailureCode,
        message: String,
        format: ArchiveFormat,
    ): Nothing = throw ArchiveValidationException(code, message, format = format)

    private const val MAX_AUTO_RENAME_ATTEMPTS = 100_000
}

/** Rebuilds a writable TAR-family format in one source pass, verifies it, then commits it. */
internal class TarArchiveMutationProvider(
    private val session: ExplorerActionHostSessionClient,
    cacheDirectory: File,
    engine: ArchiveEngine = ArchiveEngine.DEFAULT,
    override val format: ArchiveFormat = ArchiveFormat.TAR,
) : ArchiveMutationProvider {
    override val capabilities = requireNotNull(tarMutationCapabilities(format)) {
        "${format.displayName} does not have a TAR mutation provider"
    }
    private val container = format.tarContainer()
    private val compressionLevel = tarMutationCompressionLevel(format)
    private val verifier = CreatedArchiveVerifier(cacheDirectory, engine)

    override fun availability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
        if (snapshot.format == format) {
            tarMutationAvailability(snapshot)
        } else {
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
            )
        }

    override fun prepare(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): PreparedArchiveMutation {
        val availability = availability(snapshot)
        if (!availability.isAvailable) {
            throw unavailableMutation(availability.unavailableReason)
        }
        check(capabilities.supports(request.operation)) {
            "${format.displayName} mutation provider does not support ${request.operation}"
        }
        return TarArchiveMutationPlanner.plan(snapshot, request)
    }

    override fun execute(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        prepared: PreparedArchiveMutation,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): HostOutputTransaction {
        checkCancelled()
        val plan = prepared as? TarArchiveMutationPlan
            ?: throw IllegalArgumentException("Prepared mutation belongs to another provider")
        require(plan.format == format) { "Prepared mutation format changed" }
        if (!plan.sourceVersion.matches(snapshot)) {
            sourceChanged("${format.displayName} mutation plan belongs to another source snapshot")
        }
        val availability = availability(snapshot)
        if (!availability.isAvailable) {
            throw unavailableMutation(availability.unavailableReason)
        }
        val manifest = plan.toManifest()
        progress.onProgress(
            ArchiveMutationProgress(
                phase = ArchiveMutationPhase.PREPARING,
                currentPath = null,
                completedEntries = 0,
                totalEntries = plan.entries.size,
            ),
        )
        return runTransaction(
            source = source,
            snapshot = snapshot,
            targetId = targetId,
            displayName = displayName,
            plan = plan,
            manifest = manifest,
            checkCancelled = checkCancelled,
            progress = progress,
        )
    }

    private fun unavailableMutation(reason: ArchiveMutationUnavailableReason?): ArchiveException =
        when (reason) {
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE -> ArchiveValidationException(
                ArchiveFailureCode.MISSING_VOLUME,
                "Multi-volume ${format.displayName} archives are read-only",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH -> ArchiveValidationException(
                ArchiveFailureCode.INVALID_PATH,
                "A ${format.displayName} archive with unsafe entry paths cannot be rewritten",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "This ${format.displayName} archive contains an entry type that cannot be rewritten safely",
                format = format,
            )
            ArchiveMutationUnavailableReason.PASSWORD_REQUIRED -> ArchiveValidationException(
                ArchiveFailureCode.PASSWORD_REQUIRED,
                "This ${format.displayName} archive requires unavailable credentials",
                format = format,
            )
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
            null,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "The selected archive does not belong to the ${format.displayName} mutation provider",
                format = format,
            )
        }

    private fun runTransaction(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        plan: TarArchiveMutationPlan,
        manifest: ArchiveSourceManifest,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): HostOutputTransaction {
        val prepared = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = displayName,
            format = format,
        ) {
            session.prepareTargetReplacement(targetId, displayName)
        }
        try {
            val descriptor = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.OPEN,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.openOutput(prepared.id)
            }
            val counters = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.WRITE,
                outputDisplayName = displayName,
                format = format,
            ) {
                descriptor.use { output ->
                    writeReplacement(
                        descriptor = output,
                        source = source,
                        snapshot = snapshot,
                        plan = plan,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    )
                }
            }
            checkCancelled()
            progress.onProgress(
                ArchiveMutationProgress(
                    phase = ArchiveMutationPhase.VERIFYING,
                    currentPath = null,
                    completedEntries = plan.entries.size,
                    totalEntries = plan.entries.size,
                ),
            )
            val pendingDescriptor = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.openPendingOutput(prepared.id)
            }
            runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = displayName,
                format = format,
            ) {
                pendingDescriptor.use { pending ->
                    verifier.verifyPendingOutput(
                        descriptor = pending,
                        format = format,
                        manifest = manifest,
                        counters = counters,
                        password = null,
                        checkCancelled = checkCancelled,
                    )
                }
            }
            checkCancelled()
            progress.onProgress(
                ArchiveMutationProgress(
                    phase = ArchiveMutationPhase.COMMITTING,
                    currentPath = null,
                    completedEntries = plan.entries.size,
                    totalEntries = plan.entries.size,
                ),
            )
            return runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.COMMIT,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.commitExactOutput(prepared.id, displayName)
            }
        } catch (operationFailure: Throwable) {
            val rollbackFailure = runCatching { session.abortOutput(prepared.id) }.exceptionOrNull()
            if (rollbackFailure != null) {
                throw ArchiveCreationRollbackException(
                    pendingOutputDisplayName = prepared.displayName,
                    pendingOutputDisplayPath = prepared.displayPath,
                    operationFailure = operationFailure,
                    rollbackFailure = rollbackFailure,
                    format = format,
                )
            }
            throw operationFailure
        }
    }

    private fun writeReplacement(
        descriptor: ParcelFileDescriptor,
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        plan: TarArchiveMutationPlan,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): ArchiveCreationCounters {
        requireUnchangedSource(source, snapshot)
        val retainedByOrdinal = plan.entries.withIndex().mapNotNull { indexed ->
            val existing = indexed.value.source as? TarArchiveMutationSource.Existing
                ?: return@mapNotNull null
            existing.entry.ordinal to indexed
        }.toMap()
        if (retainedByOrdinal.size != plan.entries.count { it.source is TarArchiveMutationSource.Existing }) {
            sourceChanged("${format.displayName} mutation plan contains duplicate source entries")
        }
        val counters = ArchiveCreationCounters(plan.entries.size)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
            BufferedOutputStream(rawOutput, BUFFER_SIZE).use { bufferedOutput ->
                val containerOutput = TarArchiveCompression.openOutput(
                    format = format,
                    output = bufferedOutput,
                    compressionLevel = compressionLevel,
                )
                TarArchiveOutputStream(containerOutput, StandardCharsets.UTF_8.name()).use { output ->
                    configureOutput(output)
                    TarArchiveAccess.open(
                        source = source,
                        container = container,
                        checkCancelled = checkCancelled,
                    ).use { input ->
                        snapshot.entries.forEachIndexed { ordinal, expected ->
                            checkCancelled()
                            val liveTarEntry = input.nextEntry
                                ?: sourceChanged("A TAR entry disappeared while rewriting")
                            TarArchiveAccess.requireValidChecksum(liveTarEntry)
                            val live = TarArchiveAccess.toReaderEntry(
                                input = input,
                                entry = liveTarEntry,
                                ordinal = ordinal,
                                container = container,
                                canMutate = true,
                            )
                            validateLiveEntry(live, expected)
                            retainedByOrdinal[ordinal]?.let { indexed ->
                                writeRetainedEntry(
                                    entryIndex = indexed.index,
                                    planned = indexed.value,
                                    input = input,
                                    output = output,
                                    counters = counters,
                                    checkCancelled = checkCancelled,
                                    progress = progress,
                                    totalEntries = plan.entries.size,
                                )
                            }
                        }
                        if (input.nextEntry != null) {
                            sourceChanged("TAR gained an entry while it was being rewritten")
                        }
                    }
                    plan.entries.withIndex()
                        .filter { it.value.source !is TarArchiveMutationSource.Existing }
                        .forEach { indexed ->
                            writeAddedEntry(
                                entryIndex = indexed.index,
                                planned = indexed.value,
                                output = output,
                                counters = counters,
                                checkCancelled = checkCancelled,
                                progress = progress,
                                totalEntries = plan.entries.size,
                            )
                        }
                }
            }
        }
        requireUnchangedSource(source, snapshot)
        return counters
    }

    private fun writeRetainedEntry(
        entryIndex: Int,
        planned: TarArchiveMutationEntry,
        input: TarArchiveInputStream,
        output: TarArchiveOutputStream,
        counters: ArchiveCreationCounters,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
        totalEntries: Int,
    ) {
        reportWriting(progress, planned.archivePath, entryIndex, totalEntries)
        if (planned.isDirectory) {
            output.putArchiveEntry(tarEntry(planned, 0L))
            output.closeArchiveEntry()
            counters.directories++
            return
        }
        output.putArchiveEntry(tarEntry(planned, planned.size))
        val fingerprint = copyAndFingerprint(
            input = input,
            output = output,
            expectedSize = planned.size,
            checkCancelled = checkCancelled,
            onBytesCopied = { copied ->
                counters.bytesRead = Math.addExact(counters.bytesRead, copied.toLong())
            },
        )
        counters.recordSourceFingerprint(entryIndex, fingerprint)
        output.closeArchiveEntry()
        counters.files++
    }

    private fun writeAddedEntry(
        entryIndex: Int,
        planned: TarArchiveMutationEntry,
        output: TarArchiveOutputStream,
        counters: ArchiveCreationCounters,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
        totalEntries: Int,
    ) {
        reportWriting(progress, planned.archivePath, entryIndex, totalEntries)
        when (val added = planned.source) {
            is TarArchiveMutationSource.AddedDirectory -> {
                output.putArchiveEntry(tarEntry(planned, 0L))
                output.closeArchiveEntry()
                counters.directories++
            }
            is TarArchiveMutationSource.AddedFile -> {
                val resolvedSize = if (added.file.size >= 0L) {
                    added.file.size
                } else {
                    measureAddedFile(planned.archivePath, added.file, checkCancelled)
                }
                output.putArchiveEntry(tarEntry(planned, resolvedSize))
                val input = openAddedFile(planned.archivePath, added.file)
                val fingerprint = input.use { sourceInput ->
                    copyAndFingerprint(
                        input = sourceInput,
                        output = output,
                        expectedSize = resolvedSize,
                        checkCancelled = checkCancelled,
                        onBytesCopied = { copied ->
                            counters.bytesRead = Math.addExact(counters.bytesRead, copied.toLong())
                        },
                    )
                }
                counters.recordSourceFingerprint(entryIndex, fingerprint)
                output.closeArchiveEntry()
                counters.files++
            }
            is TarArchiveMutationSource.Existing ->
                error("Existing TAR entries must be written during the source pass")
        }
    }

    private fun configureOutput(output: TarArchiveOutputStream) {
        output.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
        output.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
        output.setAddPaxHeadersForNonAsciiNames(true)
    }

    private fun tarEntry(
        planned: TarArchiveMutationEntry,
        resolvedSize: Long,
    ): TarArchiveEntry {
        val typeFlag = if (planned.isDirectory) TarConstants.LF_DIR else TarConstants.LF_NORMAL
        val name = if (planned.isDirectory) planned.archivePath.trimEnd('/') + '/' else planned.archivePath
        return TarArchiveEntry(name, typeFlag).apply {
            setSize(resolvedSize)
            setMode(
                if (planned.isDirectory) {
                    TarArchiveEntry.DEFAULT_DIR_MODE
                } else {
                    TarArchiveEntry.DEFAULT_FILE_MODE
                },
            )
            setModTime(planned.lastModified.coerceAtLeast(0L))
        }
    }

    private fun validateLiveEntry(
        live: ArchiveReaderEntry,
        expected: ArchiveEntry,
    ) {
        if (
            live.ordinal != expected.ordinal ||
            live.name != expected.sourceName ||
            live.isDirectory != expected.isDirectory ||
            live.compressionMethod != expected.compressionMethod ||
            live.compressionMethodId != expected.compressionMethodId ||
            live.isEncrypted != expected.isEncrypted ||
            live.encryptionMethod != expected.encryptionMethod ||
            live.compressedSize != expected.compressedSize ||
            live.size != expected.uncompressedSize ||
            live.crc != expected.crc32 ||
            live.time != expected.modifiedTimeMillis
        ) {
            sourceChanged("TAR directory metadata changed before rewriting")
        }
    }

    private fun requireUnchangedSource(source: ArchiveReadSource, snapshot: ArchiveSnapshot) {
        val identity = try {
            source.identity()
        } catch (error: IOException) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.SOURCE_CHANGED,
                message = "TAR source cannot be inspected while rewriting",
                cause = error,
                format = format,
            )
        }
        if (
            identity.length != snapshot.sourceLength ||
            identity.lastModifiedMillis != snapshot.sourceLastModifiedMillis
        ) {
            sourceChanged("TAR source changed while rewriting")
        }
    }

    private fun measureAddedFile(
        archivePath: String,
        file: ArchiveMutationAddedFile,
        checkCancelled: () -> Unit,
    ): Long {
        val input = openAddedFile(archivePath, file)
        return try {
            input.use { sourceInput ->
                val buffer = ByteArray(BUFFER_SIZE)
                var measured = 0L
                while (true) {
                    checkCancelled()
                    val read = sourceInput.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    measured = Math.addExact(measured, read.toLong())
                }
                measured
            }
        } catch (error: ArchiveException) {
            throw error
        } catch (error: IOException) {
            throw ArchiveCreationSourceException(
                sourceArchivePath = archivePath,
                message = "TAR source file cannot be measured: $archivePath",
                cause = error,
            )
        }
    }

    private fun openAddedFile(
        archivePath: String,
        file: ArchiveMutationAddedFile,
    ): InputStream = try {
        file.openInputStream()
    } catch (error: IOException) {
        throw ArchiveCreationSourceException(
            sourceArchivePath = archivePath,
            message = "TAR source file cannot be opened: $archivePath",
            cause = error,
        )
    }

    private fun copyAndFingerprint(
        input: InputStream,
        output: OutputStream,
        expectedSize: Long,
        checkCancelled: () -> Unit,
        onBytesCopied: (Int) -> Unit,
    ): ArchiveCreationSourceFingerprint {
        val digest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        val buffered = if (input is BufferedInputStream || input is TarArchiveInputStream) {
            input
        } else {
            BufferedInputStream(input)
        }
        while (true) {
            checkCancelled()
            val read = buffered.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            copied = Math.addExact(copied, read.toLong())
            if (copied > expectedSize) {
                sourceChanged("TAR entry data grew while it was being rewritten")
            }
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            onBytesCopied(read)
        }
        if (copied != expectedSize) {
            sourceChanged("TAR entry size changed while it was being rewritten")
        }
        return ArchiveCreationSourceFingerprint(copied, digest.digest())
    }

    private fun reportWriting(
        progress: ArchiveMutationProgressListener,
        path: String,
        completedEntries: Int,
        totalEntries: Int,
    ) {
        progress.onProgress(
            ArchiveMutationProgress(
                phase = ArchiveMutationPhase.WRITING,
                currentPath = path,
                completedEntries = completedEntries,
                totalEntries = totalEntries,
            ),
        )
    }

    private fun TarArchiveMutationPlan.toManifest(): ArchiveSourceManifest {
        var knownBytes = 0L
        var unknownFiles = 0L
        var files = 0L
        var directories = 0L
        val manifestEntries = entries.mapIndexed { index, entry ->
            if (entry.isDirectory) {
                directories++
            } else {
                files++
                if (entry.size >= 0L) {
                    knownBytes = try {
                        Math.addExact(knownBytes, entry.size)
                    } catch (error: ArithmeticException) {
                        throw ArchiveValidationException(
                            code = ArchiveFailureCode.MALFORMED_ARCHIVE,
                            message = "Rewritten TAR size metadata overflows",
                            cause = error,
                            format = format,
                        )
                    }
                } else {
                    unknownFiles++
                }
            }
            ArchiveSourceEntry(
                targetId = "tar-mutation-$index",
                relativePath = entry.archivePath,
                archivePath = entry.archivePath,
                kind = if (entry.isDirectory) {
                    ExplorerActionValues.TARGET_DIRECTORY
                } else {
                    ExplorerActionValues.TARGET_FILE
                },
                size = entry.size,
                lastModified = entry.lastModified,
            )
        }
        return ArchiveSourceManifest(
            entries = manifestEntries,
            fileCount = files,
            directoryCount = directories,
            knownSourceBytes = knownBytes,
            unknownSizeFileCount = unknownFiles,
        )
    }

    private fun sourceChanged(message: String): Nothing =
        throw ArchiveValidationException(
            code = ArchiveFailureCode.SOURCE_CHANGED,
            message = message,
            format = format,
            stage = ArchiveFailureStage.INPUT,
        )

    private companion object {
        const val BUFFER_SIZE = 64 * 1_024
        const val SHA_256 = "SHA-256"
    }
}

private const val TAR_REGULAR_FILE_METHOD = "TAR"
