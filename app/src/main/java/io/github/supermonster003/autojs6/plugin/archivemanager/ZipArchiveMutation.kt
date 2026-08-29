package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

internal val ZIP_MUTATION_CAPABILITIES = ArchiveMutationCapabilities(
    operations = setOf(
        ArchiveOperation.ADD,
        ArchiveOperation.DELETE,
        ArchiveOperation.RENAME,
    ),
    strategy = ArchiveMutationStrategy.FULL_REWRITE,
    minimumHostProtocolVersion = 8,
    metadataEffects = setOf(
        ArchiveMutationMetadataEffect.ARCHIVE_COMMENT_DROPPED,
        ArchiveMutationMetadataEffect.ENTRY_COMMENTS_DROPPED,
        ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
        ArchiveMutationMetadataEffect.UNIX_ATTRIBUTES_DROPPED,
        ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
        ArchiveMutationMetadataEffect.ENCRYPTION_SETTINGS_NORMALIZED,
    ),
)

internal fun zipMutationAvailability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
    when {
        snapshot.format != ArchiveFormat.ZIP -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
        )
        snapshot.volumeIdentities.isNotEmpty() -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE,
        )
        snapshot.entries.any { it.pathStatus != ArchiveEntryPathStatus.SAFE } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH,
            )
        snapshot.entries.any { !it.isDirectory && it.isEncrypted && !it.capabilities.canOpen } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.PASSWORD_REQUIRED,
            )
        snapshot.entries.any { !it.isDirectory && !it.capabilities.canOpen } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
            )
        snapshot.entries.any {
            !it.capabilities.canDelete || !it.capabilities.canRename
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
        )
        else -> ArchiveMutationAvailability.available(ZIP_MUTATION_CAPABILITIES)
    }

internal data class ZipArchiveMutationPlan(
    val entries: List<ZipArchiveMutationEntry>,
    val encryptAddedFiles: Boolean,
    override val operation: ArchiveOperation,
    override val sourceVersion: ArchiveMutationSourceVersion,
    override val workEstimate: ArchiveMutationWorkEstimate,
    override val metadataEffects: Set<ArchiveMutationMetadataEffect>,
) : PreparedArchiveMutation {
    override val format = ArchiveFormat.ZIP
}

internal data class ZipArchiveMutationEntry(
    val archivePath: String,
    val source: ZipArchiveMutationSource,
) {
    val isDirectory: Boolean
        get() = source is ZipArchiveMutationSource.AddedDirectory ||
            (source as? ZipArchiveMutationSource.Existing)?.entry?.isDirectory == true

    val size: Long
        get() = when (source) {
            is ZipArchiveMutationSource.Existing -> source.entry.uncompressedSize
            is ZipArchiveMutationSource.AddedFile -> source.file.size
            is ZipArchiveMutationSource.AddedDirectory -> 0L
        }

    val lastModified: Long
        get() = when (source) {
            is ZipArchiveMutationSource.Existing -> source.entry.modifiedTimeMillis ?: -1L
            is ZipArchiveMutationSource.AddedFile -> source.file.lastModified
            is ZipArchiveMutationSource.AddedDirectory -> source.lastModified
        }
}

internal sealed interface ZipArchiveMutationSource {
    data class Existing(val entry: ArchiveEntry) : ZipArchiveMutationSource
    data class AddedFile(val file: ArchiveMutationAddedFile) : ZipArchiveMutationSource
    data class AddedDirectory(val lastModified: Long = -1L) : ZipArchiveMutationSource
}

/** Produces a complete, collision-free replacement directory before the host output is reserved. */
internal object ZipArchiveMutationPlanner {

    fun plan(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): ZipArchiveMutationPlan {
        require(snapshot.format == ArchiveFormat.ZIP) { "Only ZIP archives can be rewritten" }
        val original = snapshot.entries.map { entry ->
            ZipArchiveMutationEntry(entry.path, ZipArchiveMutationSource.Existing(entry))
        }
        val entries = when (request) {
            is ArchiveMutationRequest.Delete -> delete(original, request.paths, snapshot)
            is ArchiveMutationRequest.Rename -> rename(original, request, snapshot)
            is ArchiveMutationRequest.AddDirectory -> addDirectory(original, request, snapshot)
            is ArchiveMutationRequest.AddFiles -> addFiles(original, request, snapshot)
            is ArchiveMutationRequest.AddTree -> addTree(original, request, snapshot)
        }
        validateFinalEntries(entries, snapshot.structureLimits)
        entries.forEach(::requireRewritableEntry)
        return ZipArchiveMutationPlan(
            entries = entries.toList(),
            encryptAddedFiles = snapshot.entries.any(ArchiveEntry::isEncrypted),
            operation = request.operation,
            sourceVersion = ArchiveMutationSourceVersion.capture(snapshot),
            workEstimate = workEstimate(snapshot, entries),
            metadataEffects = ZIP_MUTATION_CAPABILITIES.metadataEffects,
        )
    }

    private fun delete(
        original: List<ZipArchiveMutationEntry>,
        requestedPaths: Set<String>,
        snapshot: ArchiveSnapshot,
    ): List<ZipArchiveMutationEntry> {
        if (requestedPaths.isEmpty()) fail(ArchiveFailureCode.EMPTY_SELECTION, "No ZIP entries selected")
        val paths = requestedPaths.mapTo(linkedSetOf()) { requested ->
            validatedSelectionPath(requested, snapshot)
        }
        paths.forEach { path ->
            if (!pathExists(original, path)) {
                fail(ArchiveFailureCode.UNKNOWN_SELECTION, "The selected ZIP entry no longer exists")
            }
        }
        return original.filterNot { planned -> paths.any { planned.archivePath.isAtOrBelow(it) } }
    }

    private fun rename(
        original: List<ZipArchiveMutationEntry>,
        request: ArchiveMutationRequest.Rename,
        snapshot: ArchiveSnapshot,
    ): List<ZipArchiveMutationEntry> {
        val sourcePath = validatedSelectionPath(request.path, snapshot)
        if (!pathExists(original, sourcePath)) {
            fail(ArchiveFailureCode.UNKNOWN_SELECTION, "The selected ZIP entry no longer exists")
        }
        val newLeaf = requireLeafName(request.newDisplayName)
        val parent = sourcePath.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
        val targetPath = portableChildPath(parent, newLeaf, snapshot.structureLimits)
        if (targetPath == sourcePath) {
            fail(ArchiveFailureCode.INVALID_DESTINATION_NAME, "The ZIP entry name did not change")
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
        original: List<ZipArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddDirectory,
        snapshot: ArchiveSnapshot,
    ): List<ZipArchiveMutationEntry> {
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        val path = portableChildPath(parent, requireLeafName(request.displayName), snapshot.structureLimits)
        if (pathExists(original, path)) {
            fail(ArchiveFailureCode.DUPLICATE_PATH, "A ZIP entry with this name already exists")
        }
        return original + ZipArchiveMutationEntry(path, ZipArchiveMutationSource.AddedDirectory())
    }

    private fun addFiles(
        original: List<ZipArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddFiles,
        snapshot: ArchiveSnapshot,
    ): List<ZipArchiveMutationEntry> {
        if (request.files.isEmpty()) fail(ArchiveFailureCode.EMPTY_SELECTION, "No files selected")
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        val additions = request.files.map { file ->
            val path = portableChildPath(parent, requireLeafName(file.displayName), snapshot.structureLimits)
            ZipArchiveMutationEntry(path, ZipArchiveMutationSource.AddedFile(file))
        }
        return original + additions
    }

    private fun addTree(
        original: List<ZipArchiveMutationEntry>,
        request: ArchiveMutationRequest.AddTree,
        snapshot: ArchiveSnapshot,
    ): List<ZipArchiveMutationEntry> {
        if (request.entries.isEmpty()) {
            fail(ArchiveFailureCode.EMPTY_SELECTION, "No files or folders were selected")
        }
        val parent = validatedParentPath(request.parentPath, original, snapshot)
        data class ValidatedInput(
            val path: String,
            val added: ArchiveMutationAddedTreeEntry,
            val rootKey: String,
            val sourceRoot: String,
        )

        val validated = request.entries.map { added ->
            val path = ArchivePathPolicy.validateEntryPath(
                sourceName = added.relativePath,
                isDirectory = added is ArchiveMutationAddedTreeEntry.Directory,
                limits = snapshot.structureLimits,
            ).path
            if (path != added.relativePath) {
                fail(ArchiveFailureCode.INVALID_PATH, "The selected items contain an ambiguous path")
            }
            if (
                added is ArchiveMutationAddedTreeEntry.FileEntry &&
                added.file.displayName != path.substringAfterLast('/')
            ) {
                fail(ArchiveFailureCode.INVALID_DESTINATION_NAME, "A selected file name changed while scanning")
            }
            val sourceRoot = path.substringBefore('/')
            val rootKey = added.inputRootId?.let { inputRootId ->
                if (
                    inputRootId.length !in 1..ExplorerActionProtocol.MAX_ARCHIVE_INPUT_NODE_ID_LENGTH ||
                    inputRootId.any { character ->
                        character.isWhitespace() || character.code < 0x20 || character.code == 0x7F
                    }
                ) {
                    fail(ArchiveFailureCode.INVALID_PATH, "A selected folder root identity is invalid")
                }
                "input:$inputRootId"
            } ?: "path:$sourceRoot"
            ValidatedInput(path, added, rootKey, sourceRoot)
        }
        val rootNames = LinkedHashMap<String, String>()
        val entryKeys = HashSet<Pair<String, String>>()
        validated.forEach { input ->
            val previousName = rootNames.putIfAbsent(input.rootKey, input.sourceRoot)
            if (previousName != null && previousName != input.sourceRoot) {
                fail(ArchiveFailureCode.INVALID_PATH, "A selected folder root name changed while scanning")
            }
            if (!entryKeys.add(input.rootKey to input.path)) {
                fail(ArchiveFailureCode.DUPLICATE_PATH, "The selected items contain a duplicate path")
            }
        }
        val directories = validated
            .filter { input -> input.added is ArchiveMutationAddedTreeEntry.Directory }
            .mapTo(hashSetOf()) { input -> input.rootKey to input.path }
        validated.forEach { input ->
            val immediateParent = input.path.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            if (immediateParent.isNotEmpty() && input.rootKey to immediateParent !in directories) {
                fail(ArchiveFailureCode.INVALID_PATH, "A selected folder tree has a missing directory")
            }
        }

        val rootEntries = LinkedHashMap<String, ValidatedInput>()
        validated.filter { input -> '/' !in input.path }.forEach { input ->
            if (rootEntries.put(input.rootKey, input) != null) {
                fail(ArchiveFailureCode.DUPLICATE_PATH, "The selected items contain a duplicate root")
            }
        }
        if (rootEntries.keys != rootNames.keys) {
            fail(ArchiveFailureCode.INVALID_PATH, "A selected folder root is missing")
        }

        val occupiedRootKeys = occupiedChildKeys(original, parent)
        rootEntries.values.forEach { input ->
            if (
                input.added is ArchiveMutationAddedTreeEntry.FileEntry &&
                !occupiedRootKeys.add(ArchivePathPolicy.destinationCollisionKey(input.sourceRoot))
            ) {
                fail(ArchiveFailureCode.DUPLICATE_PATH, "A selected file conflicts with another root")
            }
        }
        val targetRoots = LinkedHashMap<String, String>()
        rootEntries.forEach { (rootKey, input) ->
            val targetRoot = if (input.added is ArchiveMutationAddedTreeEntry.Directory) {
                availableChildName(
                    occupiedKeys = occupiedRootKeys,
                    parent = parent,
                    requested = input.sourceRoot,
                    limits = snapshot.structureLimits,
                )
            } else {
                input.sourceRoot
            }
            targetRoots[rootKey] = targetRoot
        }
        val additions = validated.map { input ->
            val targetRoot = targetRoots.getValue(input.rootKey)
            val remappedRelativePath = targetRoot + input.path.removePrefix(input.sourceRoot)
            val archivePath = if (parent.isEmpty()) {
                remappedRelativePath
            } else {
                "$parent/$remappedRelativePath"
            }
            val source = when (val added = input.added) {
                is ArchiveMutationAddedTreeEntry.Directory ->
                    ZipArchiveMutationSource.AddedDirectory(added.lastModified)
                is ArchiveMutationAddedTreeEntry.FileEntry ->
                    ZipArchiveMutationSource.AddedFile(added.file)
            }
            ZipArchiveMutationEntry(archivePath, source)
        }
        return original + additions
    }

    private fun occupiedChildKeys(
        original: List<ZipArchiveMutationEntry>,
        parent: String,
    ): MutableSet<String> = original.mapNotNullTo(hashSetOf()) { entry ->
        val relative = when {
            parent.isEmpty() -> entry.archivePath
            entry.archivePath.startsWith("$parent/") -> entry.archivePath.removePrefix("$parent/")
            else -> return@mapNotNullTo null
        }
        relative.substringBefore('/').takeIf(String::isNotEmpty)
            ?.let(ArchivePathPolicy::destinationCollisionKey)
    }

    private fun availableChildName(
        occupiedKeys: MutableSet<String>,
        parent: String,
        requested: String,
        limits: ArchiveStructureLimits,
    ): String {
        if (occupiedKeys.add(ArchivePathPolicy.destinationCollisionKey(requested))) return requested
        for (index in 2..MAX_AUTO_RENAME_ATTEMPTS) {
            val candidate = "$requested ($index)"
            requireLeafName(candidate)
            portableChildPath(parent, candidate, limits)
            if (occupiedKeys.add(ArchivePathPolicy.destinationCollisionKey(candidate))) return candidate
        }
        fail(ArchiveFailureCode.DUPLICATE_PATH, "No available ZIP folder name could be reserved")
    }

    private fun validatedParentPath(
        requested: String,
        original: List<ZipArchiveMutationEntry>,
        snapshot: ArchiveSnapshot,
    ): String {
        if (requested.isEmpty()) return ArchivePathPolicy.ROOT_PATH
        val path = validatedSelectionPath(requested, snapshot)
        val explicitFile = original.firstOrNull { it.archivePath == path && !it.isDirectory }
        if (explicitFile != null || !pathExists(original, path)) {
            fail(ArchiveFailureCode.UNKNOWN_SELECTION, "The destination ZIP folder no longer exists")
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
            fail(ArchiveFailureCode.INVALID_PATH, "Unsafe ZIP paths cannot be rewritten")
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

    private fun requireLeafName(value: String): String =
        ArchiveIntentPolicy.validateDisplayName(value)
            ?: fail(ArchiveFailureCode.INVALID_DESTINATION_NAME, "The ZIP entry name is invalid")

    private fun pathExists(entries: List<ZipArchiveMutationEntry>, path: String): Boolean =
        entries.any { it.archivePath.isAtOrBelow(path) }

    private fun validateFinalEntries(
        entries: List<ZipArchiveMutationEntry>,
        limits: ArchiveStructureLimits,
    ) {
        if (entries.size > limits.maxEntries) {
            fail(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED, "The rewritten ZIP has too many entries")
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
                    "The rewritten ZIP contains a duplicate or conflicting path",
                )
            }
        }
        entries.forEach { entry ->
            var parent = entry.archivePath.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            while (parent.isNotEmpty()) {
                if (explicitTypes[parent] == false) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "A rewritten ZIP file is also used as a directory",
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
                    if (equivalentEntries.map(ZipArchiveMutationEntry::isDirectory).distinct().size > 1) {
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT
                    } else {
                        ArchiveFailureCode.DUPLICATE_PATH
                    },
                    "The ZIP change introduces case-insensitive or Unicode-equivalent names",
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
                        "The ZIP change uses a case-insensitive or Unicode-equivalent file as a directory",
                    )
                }
                parent = parent.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            }
        }
    }

    private fun ZipArchiveMutationEntry.isUnchangedExistingPath(): Boolean =
        (source as? ZipArchiveMutationSource.Existing)?.entry?.path == archivePath

    private fun requireRewritableEntry(planned: ZipArchiveMutationEntry) {
        val entry = (planned.source as? ZipArchiveMutationSource.Existing)?.entry ?: return
        if (entry.pathStatus != ArchiveEntryPathStatus.SAFE) {
            fail(ArchiveFailureCode.INVALID_PATH, "Unsafe ZIP paths cannot be preserved by rewriting")
        }
        if (entry.isDirectory) return
        if (entry.isEncrypted && !entry.capabilities.canOpen) {
            fail(ArchiveFailureCode.PASSWORD_REQUIRED, "The ZIP password must be applied before editing")
        }
        if (!entry.capabilities.canOpen || entry.compressionMethod !in SUPPORTED_METHODS) {
            fail(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "A retained ZIP entry uses a compression method that cannot be rewritten",
            )
        }
        if (entry.isEncrypted && entry.encryptionMethod !in SUPPORTED_ENCRYPTION_METHODS) {
            fail(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "A retained ZIP entry uses an encryption method that cannot be rewritten",
            )
        }
    }

    private fun String.isAtOrBelow(parent: String): Boolean =
        this == parent || startsWith("$parent/")

    private fun workEstimate(
        snapshot: ArchiveSnapshot,
        entries: List<ZipArchiveMutationEntry>,
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
                            "Rewritten ZIP size metadata overflows",
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

    private fun fail(code: ArchiveFailureCode, message: String): Nothing =
        throw ArchiveValidationException(code, message, format = ArchiveFormat.ZIP)

    private val SUPPORTED_METHODS = setOf(
        ArchiveCompressionMethod.STORED,
        ArchiveCompressionMethod.DEFLATED,
    )
    private val SUPPORTED_ENCRYPTION_METHODS = setOf(
        ArchiveEncryptionMethod.ZIP_CRYPTO,
        ArchiveEncryptionMethod.AES,
    )
    private const val MAX_AUTO_RENAME_ATTEMPTS = 100_000
}

/** Rebuilds one ZIP into a host-owned pending file, verifies it fully, then atomically commits it. */
internal class ZipArchiveMutationProvider(
    private val session: ExplorerActionHostSessionClient,
    cacheDirectory: File,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) : ArchiveMutationProvider {
    override val format = ArchiveFormat.ZIP
    override val capabilities = ZIP_MUTATION_CAPABILITIES
    private val verifier = CreatedArchiveVerifier(cacheDirectory, engine)

    override fun availability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
        zipMutationAvailability(snapshot)

    override fun prepare(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): PreparedArchiveMutation {
        val availability = availability(snapshot)
        if (!availability.isAvailable) {
            throw unavailableMutation(availability.unavailableReason)
        }
        check(capabilities.supports(request.operation)) {
            "ZIP mutation provider does not support ${request.operation}"
        }
        return ZipArchiveMutationPlanner.plan(snapshot, request)
    }

    override fun execute(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        prepared: PreparedArchiveMutation,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
        beforeOutputVerification: () -> Unit,
    ): HostOutputTransaction {
        checkCancelled()
        val plan = prepared as? ZipArchiveMutationPlan
            ?: throw IllegalArgumentException("Prepared mutation belongs to another provider")
        require(plan.format == format) { "Prepared mutation format changed" }
        if (!plan.sourceVersion.matches(snapshot)) {
            sourceChanged("ZIP mutation plan belongs to another source snapshot")
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
        val password = snapshot.readerOptions.passwordChars()
        val writesEncryptedEntry = plan.entries.any { entry ->
            (entry.source as? ZipArchiveMutationSource.Existing)?.entry?.isEncrypted == true ||
                (entry.source is ZipArchiveMutationSource.AddedFile && plan.encryptAddedFiles)
        }
        if (writesEncryptedEntry && password == null
        ) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.PASSWORD_REQUIRED,
                message = "The ZIP password must be applied before editing",
                format = ArchiveFormat.ZIP,
                stage = ArchiveFailureStage.PASSWORD,
            )
        }
        try {
            return runTransaction(
                source = source,
                snapshot = snapshot,
                targetId = targetId,
                displayName = displayName,
                plan = plan,
                manifest = manifest,
                password = password,
                checkCancelled = checkCancelled,
                progress = progress,
                beforeOutputVerification = beforeOutputVerification,
            )
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun unavailableMutation(reason: ArchiveMutationUnavailableReason?): ArchiveException =
        when (reason) {
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE -> ArchiveValidationException(
                ArchiveFailureCode.MISSING_VOLUME,
                "Multi-volume ZIP archives are read-only",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH -> ArchiveValidationException(
                ArchiveFailureCode.INVALID_PATH,
                "A ZIP with unsafe entry paths cannot be rewritten",
                format = format,
            )
            ArchiveMutationUnavailableReason.PASSWORD_REQUIRED -> ArchiveValidationException(
                ArchiveFailureCode.PASSWORD_REQUIRED,
                "The ZIP password must be applied before editing",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "This ZIP variant cannot be rewritten by the installed backend",
                format = format,
            )
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
            null,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "The selected archive does not belong to the ZIP mutation provider",
                format = format,
            )
        }

    private fun runTransaction(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        plan: ZipArchiveMutationPlan,
        manifest: ArchiveSourceManifest,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
        beforeOutputVerification: () -> Unit,
    ): HostOutputTransaction {
        val prepared = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = displayName,
            format = ArchiveFormat.ZIP,
        ) {
            session.prepareTargetReplacement(targetId, displayName)
        }
        try {
            val descriptor = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.OPEN,
                outputDisplayName = displayName,
                format = ArchiveFormat.ZIP,
            ) {
                session.openOutput(prepared.id)
            }
            val counters = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.WRITE,
                outputDisplayName = displayName,
                format = ArchiveFormat.ZIP,
            ) {
                descriptor.use { output ->
                    writeReplacement(
                        descriptor = output,
                        source = source,
                        snapshot = snapshot,
                        plan = plan,
                        password = password,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    )
                }
            }
            checkCancelled()
            beforeOutputVerification()
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
                format = ArchiveFormat.ZIP,
            ) {
                session.openPendingOutput(prepared.id)
            }
            runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = displayName,
                format = ArchiveFormat.ZIP,
            ) {
                pendingDescriptor.use { pending ->
                    verifier.verifyPendingOutput(
                        descriptor = pending,
                        format = ArchiveFormat.ZIP,
                        manifest = manifest,
                        counters = counters,
                        password = password,
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
                format = ArchiveFormat.ZIP,
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
                    format = ArchiveFormat.ZIP,
                )
            }
            throw operationFailure
        }
    }

    private fun writeReplacement(
        descriptor: ParcelFileDescriptor,
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        plan: ZipArchiveMutationPlan,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): ArchiveCreationCounters {
        requireUnchangedSource(source, snapshot)
        val readerOptions = snapshot.readerOptions.retainedCopy()
        try {
            engine.openReader(source, ArchiveFormat.ZIP, readerOptions).use { reader ->
                validateReaderSnapshot(reader, snapshot)
                val counters = ArchiveCreationCounters(plan.entries.size)
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
                    ZipOutputStream(
                        BufferedOutputStream(rawOutput, BUFFER_SIZE),
                        password,
                        StandardCharsets.UTF_8,
                    ).use { output ->
                        plan.entries.forEachIndexed { index, entry ->
                            checkCancelled()
                            progress.onProgress(
                                ArchiveMutationProgress(
                                    phase = ArchiveMutationPhase.WRITING,
                                    currentPath = entry.archivePath,
                                    completedEntries = index,
                                    totalEntries = plan.entries.size,
                                ),
                            )
                            if (entry.isDirectory) {
                                output.putNextEntry(zipParameters(entry, plan.encryptAddedFiles))
                                output.closeEntry()
                                counters.directories++
                            } else {
                                output.putNextEntry(zipParameters(entry, plan.encryptAddedFiles))
                                val expected = expectedInput(entry, reader)
                                val fingerprint = expected.input.use { input ->
                                    copyAndFingerprint(
                                        input = input,
                                        output = output,
                                        expectedSize = entry.size.takeIf { it >= 0L },
                                        expectedCrc = expected.crc,
                                        checkCancelled = checkCancelled,
                                        onBytesCopied = { copied ->
                                            counters.bytesRead = Math.addExact(
                                                counters.bytesRead,
                                                copied.toLong(),
                                            )
                                        },
                                    )
                                }
                                counters.recordSourceFingerprint(index, fingerprint)
                                output.closeEntry()
                                counters.files++
                            }
                        }
                    }
                }
                requireUnchangedSource(source, snapshot)
                return counters
            }
        } finally {
            readerOptions.clearPassword()
        }
    }

    private fun expectedInput(
        entry: ZipArchiveMutationEntry,
        reader: ArchiveReader,
    ): ExpectedMutationInput = when (val source = entry.source) {
        is ZipArchiveMutationSource.Existing -> {
            val readerEntry = reader.entryAt(source.entry.ordinal)
                ?: sourceChanged("A retained ZIP entry disappeared while rewriting")
            ExpectedMutationInput(
                input = reader.openEntry(readerEntry),
                crc = source.entry.crc32,
            )
        }
        is ZipArchiveMutationSource.AddedFile -> ExpectedMutationInput(
            input = source.file.openInputStream(),
            crc = null,
        )
        is ZipArchiveMutationSource.AddedDirectory -> error("ZIP directories have no input data")
    }

    private fun zipParameters(
        planned: ZipArchiveMutationEntry,
        encryptAddedFiles: Boolean,
    ): ZipParameters {
        val existing = (planned.source as? ZipArchiveMutationSource.Existing)?.entry
        val encrypt = existing?.isEncrypted ?: (
            planned.source is ZipArchiveMutationSource.AddedFile && encryptAddedFiles
        )
        return ZipParameters().apply {
            fileNameInZip = if (planned.isDirectory) {
                planned.archivePath.trimEnd('/') + '/'
            } else {
                planned.archivePath
            }
            compressionMethod = when (existing?.compressionMethod) {
                ArchiveCompressionMethod.STORED -> CompressionMethod.STORE
                else -> CompressionMethod.DEFLATE
            }
            compressionLevel = CompressionLevel.NORMAL
            if (planned.lastModified > 0L) lastModifiedFileTime = planned.lastModified
            if (compressionMethod == CompressionMethod.STORE && planned.size >= 0L) {
                entrySize = planned.size
            }
            if (encrypt) {
                isEncryptFiles = true
                encryptionMethod = when (existing?.encryptionMethod) {
                    ArchiveEncryptionMethod.ZIP_CRYPTO -> EncryptionMethod.ZIP_STANDARD
                    else -> EncryptionMethod.AES
                }
                if (encryptionMethod == EncryptionMethod.AES) {
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
        }
    }

    private fun validateReaderSnapshot(reader: ArchiveReader, snapshot: ArchiveSnapshot) {
        if (reader.format != ArchiveFormat.ZIP || reader.entries.size != snapshot.entries.size) {
            sourceChanged("ZIP directory metadata changed before rewriting")
        }
        reader.entries.forEachIndexed { index, current ->
            val expected = snapshot.entries[index]
            if (
                current.ordinal != expected.ordinal ||
                current.name != expected.sourceName ||
                current.isDirectory != expected.isDirectory ||
                current.compressionMethodId != expected.compressionMethodId ||
                current.isEncrypted != expected.isEncrypted ||
                current.encryptionMethod != expected.encryptionMethod ||
                current.compressedSize != expected.compressedSize ||
                current.size != expected.uncompressedSize ||
                current.crc != expected.crc32
            ) {
                sourceChanged("ZIP directory metadata changed before rewriting")
            }
        }
    }

    private fun requireUnchangedSource(source: ArchiveReadSource, snapshot: ArchiveSnapshot) {
        val identity = try {
            source.identity()
        } catch (error: IOException) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.SOURCE_CHANGED,
                message = "ZIP source cannot be inspected while rewriting",
                cause = error,
                format = ArchiveFormat.ZIP,
            )
        }
        if (
            identity.length != snapshot.sourceLength ||
            identity.lastModifiedMillis != snapshot.sourceLastModifiedMillis
        ) {
            sourceChanged("ZIP source changed while rewriting")
        }
    }

    private fun copyAndFingerprint(
        input: InputStream,
        output: ZipOutputStream,
        expectedSize: Long?,
        expectedCrc: Long?,
        checkCancelled: () -> Unit,
        onBytesCopied: (Int) -> Unit,
    ): ArchiveCreationSourceFingerprint {
        val digest = MessageDigest.getInstance(SHA_256)
        val crc = CRC32()
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        while (true) {
            checkCancelled()
            val read = buffered.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            copied = Math.addExact(copied, read.toLong())
            if (expectedSize != null && copied > expectedSize) {
                sourceChanged("ZIP entry data grew while it was being rewritten")
            }
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            crc.update(buffer, 0, read)
            onBytesCopied(read)
        }
        if (expectedSize != null && copied != expectedSize) {
            sourceChanged("ZIP entry size changed while it was being rewritten")
        }
        if (expectedCrc != null && expectedCrc >= 0L && crc.value != expectedCrc) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.CRC_MISMATCH,
                message = "ZIP entry CRC changed while it was being rewritten",
                format = ArchiveFormat.ZIP,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        return ArchiveCreationSourceFingerprint(copied, digest.digest())
    }

    private fun ZipArchiveMutationPlan.toManifest(): ArchiveSourceManifest {
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
                            message = "Rewritten ZIP size metadata overflows",
                            cause = error,
                            format = ArchiveFormat.ZIP,
                        )
                    }
                } else {
                    unknownFiles++
                }
            }
            ArchiveSourceEntry(
                targetId = "zip-mutation-$index",
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
            format = ArchiveFormat.ZIP,
            stage = ArchiveFailureStage.INPUT,
        )

    private data class ExpectedMutationInput(
        val input: InputStream,
        val crc: Long?,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val SHA_256 = "SHA-256"
    }
}
