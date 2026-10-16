package io.github.supermonster003.autojs6.plugin.three.stack.archive.tool

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveCacheStager
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveEngine
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveEntry
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveException
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveExtractionConflictPolicy
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveExtractor
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveFailureCode
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveFormat
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveInputStorage
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveLocalFileRequiredException
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchivePathPolicy
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveReaderOptions
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveResourceBudget
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveScanner
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveSnapshot
import io.github.supermonster003.autojs6.plugin.three.stack.archive.StagedArchive
import io.github.supermonster003.autojs6.plugin.three.stack.archive.archiveManagerPluginInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.tool.api.IPluginToolCallback
import org.autojs.plugin.tool.api.IPluginToolOutputTree
import org.autojs.plugin.tool.api.IPluginToolProvider
import org.autojs.plugin.tool.api.PluginToolAcceptKeys
import org.autojs.plugin.tool.api.PluginToolCancelKeys
import org.autojs.plugin.tool.api.PluginToolCatalogKeys
import org.autojs.plugin.tool.api.PluginToolContract
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorKeys
import org.autojs.plugin.tool.api.PluginToolInputJson
import org.autojs.plugin.tool.api.PluginToolInvokeKeys
import org.autojs.plugin.tool.api.PluginToolResultKeys
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provider of the AutoJs6 plugin tool contract v1 (action `org.autojs.plugin.TOOL_PROVIDER`, MCP Server roadmap P12):
 * `archive_list` and `archive_read` read an archive AutoJs6 hands over as a file parameter, `archive_extract` writes
 * it into the directory output `files` (D53) through [ToolTreeArchiveOutputWriter], with the COMPATIBLE budget and
 * existing files kept (D56). Passwords (D55) live in char arrays that are wiped after the call. Runs in the `:tool`
 * process with two worker threads; a cancel drops a queued call and stops a running one at its next check.
 */
class ArchiveToolService : Service() {

    private val worker = ThreadPoolExecutor(WORKERS, WORKERS, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { runnable ->
        Thread(runnable, "archive-tool").apply { isDaemon = true }
    }

    /** Invocation ids cancelled before or during their call; bounded because cancels may arrive after a call ended. */
    private val cancelled: MutableSet<String> = Collections.synchronizedSet(
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > MAX_REMEMBERED_CANCELS
        }),
    )

    /** The coroutine job of each running extraction, so that a cancel stops it. */
    private val extractions = ConcurrentHashMap<String, Job>()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private val binder = object : IPluginToolProvider.Stub() {

        override fun getInfo(): PluginInfo = applicationContext.archiveManagerPluginInfo()

        override fun getCatalog(request: Bundle?): Bundle = Bundle().apply {
            putInt(PluginToolCatalogKeys.CONTRACT_VERSION, PluginToolContract.CONTRACT_VERSION)
            putString(PluginToolCatalogKeys.CATALOG_JSON, ArchiveToolCatalog.JSON)
        }

        override fun invoke(request: Bundle?, inputs: Array<out ParcelFileDescriptor>?, callback: IPluginToolCallback?): Bundle {
            val descriptors = inputs?.toList().orEmpty()
            fun refuse(code: String, message: String): Bundle {
                descriptors.forEach { runCatching { it.close() } }
                return Bundle().apply {
                    putBoolean(PluginToolAcceptKeys.ACCEPTED, false)
                    putString(PluginToolAcceptKeys.ERROR_CODE, code)
                    putString(PluginToolAcceptKeys.ERROR_MESSAGE, message)
                }
            }
            val invocationId = request?.getString(PluginToolInvokeKeys.INVOCATION_ID)
            if (request == null || callback == null || invocationId == null) return refuse(PluginToolErrorCodes.FAILED, "the request is incomplete")
            val version = request.getInt(PluginToolInvokeKeys.CONTRACT_VERSION)
            if (!PluginToolContract.supportsContractVersion(version)) {
                return refuse(PluginToolErrorCodes.UNSUPPORTED_CONTRACT_VERSION, "plugin tool contract version $version is not supported")
            }
            val tool = request.getString(PluginToolInvokeKeys.TOOL)
            if (tool !in TOOLS) return refuse(PluginToolErrorCodes.UNKNOWN_TOOL, "no tool \"$tool\"")
            val call = try {
                ToolCall.of(tool!!, request, descriptors)
            } catch (e: Exception) {
                return refuse(PluginToolErrorCodes.INVALID_ARGUMENTS, e.message ?: e.toString())
            }
            try {
                worker.execute { run(invocationId, call, callback) }
            } catch (_: RejectedExecutionException) {
                call.close()
                return Bundle().apply {
                    putBoolean(PluginToolAcceptKeys.ACCEPTED, false)
                    putString(PluginToolAcceptKeys.ERROR_CODE, PluginToolErrorCodes.NOT_READY)
                    putString(PluginToolAcceptKeys.ERROR_MESSAGE, "the archive tool service is shutting down")
                }
            }
            return Bundle().apply { putBoolean(PluginToolAcceptKeys.ACCEPTED, true) }
        }

        override fun cancel(request: Bundle?) {
            val invocationId = request?.getString(PluginToolCancelKeys.INVOCATION_ID) ?: return
            cancelled.add(invocationId)
            extractions[invocationId]?.cancel()
        }
    }

    private fun run(invocationId: String, call: ToolCall, callback: IPluginToolCallback) {
        var options: ArchiveReaderOptions? = null
        var snapshot: ArchiveSnapshot? = null
        var staged: StagedArchive? = null
        try {
            if (cancelled.remove(invocationId)) {
                deliver { callback.onError(error(invocationId, PluginToolErrorCodes.CANCELLED, "cancelled before the call started")) }
                return
            }
            val check = { if (invocationId in cancelled) throw CancellationException("cancelled") }
            val readerOptions = ArchiveReaderOptions(password = call.password).also { options = it }
            var source = ArchiveCacheStager.stage(call.archive, cacheDir, call.reportedSize).also { staged = it }
            // Some backends (zip4j for encrypted ZIP entries) need a private copy, as in the Explorer session.
            fun localCopy(): StagedArchive = ArchiveCacheStager.materialize(source, cacheDir).also { copy ->
                source.close()
                source = copy
                staged = copy
            }
            var scanned = try {
                ArchiveScanner().scan(source.source, readerOptions, check)
            } catch (_: ArchiveLocalFileRequiredException) {
                ArchiveScanner().scan(localCopy().source, readerOptions, check)
            }
            if (call.tool != ArchiveToolCatalog.TOOL_LIST && source.storage == ArchiveInputStorage.SEEKABLE_DESCRIPTOR &&
                scanned.format == ArchiveFormat.ZIP && scanned.entries.any { it.isEncrypted }
            ) {
                scanned.readerOptions.clearPassword()
                scanned = ArchiveScanner().scan(localCopy().source, readerOptions, check)
            }
            snapshot = scanned
            val (result, text) = when (call.tool) {
                ArchiveToolCatalog.TOOL_LIST -> list(scanned, call)
                ArchiveToolCatalog.TOOL_READ -> read(source, scanned, call, check)
                else -> extract(invocationId, source, scanned, call)
            }
            check()
            deliver {
                callback.onResult(Bundle().apply {
                    putString(PluginToolResultKeys.INVOCATION_ID, invocationId)
                    putString(PluginToolResultKeys.RESULT_JSON, result.toString())
                    putString(PluginToolResultKeys.TEXT, text)
                    putString(PluginToolResultKeys.OUTPUTS_JSON, "[]")
                }, emptyArray())
            }
        } catch (_: CancellationException) {
            cancelled.remove(invocationId)
            deliver { callback.onError(error(invocationId, PluginToolErrorCodes.CANCELLED, "cancelled during the call")) }
        } catch (e: ArchiveException) {
            deliver { callback.onError(error(invocationId, codeOf(e.code), "${e.code.name}: ${e.message}")) }
        } catch (e: IllegalArgumentException) {
            deliver { callback.onError(error(invocationId, PluginToolErrorCodes.INVALID_ARGUMENTS, e.message ?: e.toString())) }
        } catch (t: Throwable) {
            val code = if (cancelled.remove(invocationId)) PluginToolErrorCodes.CANCELLED else PluginToolErrorCodes.FAILED
            deliver { callback.onError(error(invocationId, code, "the archive call failed: ${t.message ?: t}")) }
        } finally {
            extractions.remove(invocationId)
            staged?.close()
            options?.clearPassword()
            snapshot?.readerOptions?.clearPassword()
            call.close()
        }
    }

    private fun list(snapshot: ArchiveSnapshot, call: ToolCall): Pair<JSONObject, String> {
        val prefix = call.directory
        val matching = snapshot.entries.filter { prefix == null || it.path.startsWith("$prefix/") }
        require(prefix == null || matching.isNotEmpty() || snapshot.entries.any { it.path == prefix && it.isDirectory }) { "the archive has no folder $prefix" }
        val page = matching.drop(call.offset).take(call.limit)
        val entries = JSONArray()
        page.forEach { entry -> entries.put(entryJson(entry)) }
        val result = JSONObject()
            .put("format", snapshot.format.id)
            .put("total", matching.size)
            .put("offset", call.offset)
            .put("truncated", call.offset + page.size < matching.size)
            .put("totalUncompressedBytes", snapshot.totalUncompressedBytes)
            .put("entries", entries)
        val text = buildString {
            append(page.size).append(" of ").append(matching.size).append(if (matching.size == 1) " entry" else " entries")
            append(" in a ").append(snapshot.format.displayName).append(" archive")
            if (prefix != null) append(" under ").append(prefix)
            page.take(MAX_TEXT_LINES).forEach { entry ->
                append('\n').append(entry.path)
                if (entry.isDirectory) append('/') else append(" (").append(entry.uncompressedSize).append(" bytes)")
            }
            if (page.size > MAX_TEXT_LINES) append("\n...")
        }
        return result to text
    }

    private fun read(staged: StagedArchive, snapshot: ArchiveSnapshot, call: ToolCall, check: () -> Unit): Pair<JSONObject, String> {
        val path = call.path!!
        val entry = snapshot.entries.firstOrNull { it.path == path } ?: throw IllegalArgumentException("the archive has no file $path")
        require(!entry.isDirectory) { "$path is a folder; archive_list shows its files" }
        require(!entry.isEncrypted || snapshot.readerOptions.hasPassword) { "PASSWORD_REQUIRED: $path is encrypted; pass password" }
        if (!entry.canOpen) throw IllegalStateException("$path cannot be read (${entry.compressionMethod.name})")
        val max = call.maxBytes
        val bytes = ArchiveEngine.DEFAULT.openReader(staged.source, snapshot.format, snapshot.readerOptions).use { reader ->
            val live = reader.entryAt(entry.ordinal)?.takeIf { it.name == entry.sourceName } ?: throw IllegalStateException("the archive changed while it was read")
            reader.openEntry(live).use { input -> readUpTo(input, max + 1, check) }
        }
        val truncated = bytes.size > max
        val data = if (truncated) bytes.copyOf(max) else bytes
        val binary = data.any { it == 0.toByte() }
        val result = JSONObject()
            .put("path", path)
            .put("size", entry.uncompressedSize)
            .put("bytesRead", data.size)
            .put("truncated", truncated)
            .put("binary", binary)
        val text = if (binary) {
            "$path is a binary file of ${entry.uncompressedSize} bytes; archive_extract writes it into the working directory"
        } else {
            decodeUtf8(data, truncated).also { result.put("text", it) }
        }
        return result to text
    }

    private fun extract(invocationId: String, staged: StagedArchive, snapshot: ArchiveSnapshot, call: ToolCall): Pair<JSONObject, String> {
        val writer = ToolTreeArchiveOutputWriter(call.tree!!)
        val job = Job()
        extractions[invocationId] = job
        if (invocationId in cancelled) job.cancel()
        val outcome = runBlocking(job) {
            ArchiveExtractor(resourceBudget = ArchiveResourceBudget.COMPATIBLE).extractToWriter(
                source = staged.source,
                snapshot = snapshot,
                selectedPaths = call.paths ?: listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = ArchiveToolCatalog.OUTPUT_FILES,
                writer = writer,
                skipUnsafePaths = true,
                conflictPolicy = ArchiveExtractionConflictPolicy.SKIP,
            )
        }
        val kept = writer.kept
        val result = JSONObject()
            .put("format", snapshot.format.id)
            .put("filesExtracted", outcome.filesExtracted - kept.size)
            .put("directoriesCreated", outcome.directoriesCreated)
            .put("bytesWritten", outcome.bytesWritten - writer.discardedBytes)
            .put("kept", kept.size)
            .put("keptPaths", JSONArray(kept.take(ArchiveToolCatalog.MAX_REPORTED_PATHS)))
            .put("skipped", outcome.entriesSkipped)
        val text = buildString {
            append("Extracted ").append(outcome.filesExtracted - kept.size).append(" files from a ").append(snapshot.format.displayName).append(" archive")
            if (kept.isNotEmpty()) append("; kept ").append(kept.size).append(" existing ").append(if (kept.size == 1) "file" else "files")
            if (outcome.entriesSkipped > 0) append("; skipped ").append(outcome.entriesSkipped).append(" entries")
        }
        return result to text
    }

    private fun entryJson(entry: ArchiveEntry): JSONObject = JSONObject().apply {
        put("path", entry.path)
        put("directory", entry.isDirectory)
        put("size", if (entry.isDirectory) 0L else entry.uncompressedSize)
        if (entry.compressedSize >= 0) put("packedSize", entry.compressedSize)
        entry.modifiedTimeMillis?.let { put("modified", ISO_8601.get()!!.format(Date(it))) }
        put("encrypted", entry.isEncrypted)
        if (!entry.isOutputPathSafe) put("unsafe", true)
    }

    private fun readUpTo(input: InputStream, limit: Int, check: () -> Unit): ByteArray {
        val buffer = ByteArray(limit)
        var total = 0
        while (total < limit) {
            check()
            val read = input.read(buffer, total, limit - total)
            if (read < 0) break
            total += read
        }
        return buffer.copyOf(total)
    }

    /** UTF-8 text; a cut file drops a character the cut split instead of showing a replacement character. */
    private fun decodeUtf8(data: ByteArray, truncated: Boolean): String {
        var end = data.size
        if (truncated) {
            var back = 0
            while (back < 3 && end - back - 1 >= 0 && (data[end - back - 1].toInt() and 0xC0) == 0x80) back++
            val lead = if (end - back - 1 >= 0) data[end - back - 1].toInt() and 0xFF else 0
            val expected = when {
                lead >= 0xF0 -> 4
                lead >= 0xE0 -> 3
                lead >= 0xC0 -> 2
                else -> 1
            }
            if (expected > back + 1) end -= back + 1
        }
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(ByteBuffer.wrap(data, 0, end))
            .toString()
    }

    private fun codeOf(code: ArchiveFailureCode): String = when (code) {
        ArchiveFailureCode.PASSWORD_REQUIRED, ArchiveFailureCode.WRONG_PASSWORD,
        ArchiveFailureCode.EMPTY_SELECTION, ArchiveFailureCode.UNKNOWN_SELECTION,
        ArchiveFailureCode.INVALID_PATH, ArchiveFailureCode.INVALID_DESTINATION_NAME -> PluginToolErrorCodes.INVALID_ARGUMENTS
        ArchiveFailureCode.SOURCE_NOT_FILE, ArchiveFailureCode.SOURCE_UNREADABLE, ArchiveFailureCode.INVALID_SIGNATURE,
        ArchiveFailureCode.MALFORMED_ARCHIVE, ArchiveFailureCode.MISSING_VOLUME, ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET,
        ArchiveFailureCode.UNSUPPORTED_METHOD -> PluginToolErrorCodes.INPUT_UNREADABLE
        ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE, ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED, ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
        ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED, ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
        ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED, ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
        ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED -> PluginToolErrorCodes.LIMIT_EXCEEDED
        else -> PluginToolErrorCodes.FAILED
    }

    /** The host may be gone by the time a result is ready; there is nobody left to tell. */
    private inline fun deliver(block: () -> Unit) {
        try {
            block()
        } catch (_: RemoteException) {
        }
    }

    private fun error(invocationId: String, code: String, message: String) = Bundle().apply {
        putString(PluginToolErrorKeys.INVOCATION_ID, invocationId)
        putString(PluginToolErrorKeys.CODE, code)
        putString(PluginToolErrorKeys.MESSAGE, message.take(MAX_MESSAGE_CHARS))
    }

    /** One accepted call: its arguments, the archive descriptor it owns until it ends, and the tree of an extraction. */
    private class ToolCall(
        val tool: String,
        val archive: ParcelFileDescriptor,
        val reportedSize: Long,
        val password: CharArray?,
        val directory: String?,
        val offset: Int,
        val limit: Int,
        val path: String?,
        val maxBytes: Int,
        val paths: List<String>?,
        val tree: IPluginToolOutputTree?,
        private val descriptors: List<ParcelFileDescriptor>,
    ) {
        private val closed = AtomicBoolean(false)

        fun close() {
            if (!closed.compareAndSet(false, true)) return
            password?.fill('\u0000')
            descriptors.forEach { runCatching { it.close() } }
        }

        companion object {
            fun of(tool: String, request: Bundle, descriptors: List<ParcelFileDescriptor>): ToolCall {
                val arguments = JSONObject(request.getString(PluginToolInvokeKeys.ARGUMENTS_JSON) ?: "{}")
                val inputs = JSONArray(request.getString(PluginToolInvokeKeys.INPUTS_JSON) ?: "[]")
                require(inputs.length() == descriptors.size) { "inputsJson describes ${inputs.length()} descriptors, ${descriptors.size} arrived" }
                val index = (0 until inputs.length()).firstOrNull { inputs.getJSONObject(it).optString(PluginToolInputJson.NAME) == "archive" }
                    ?: throw IllegalArgumentException("the archive did not arrive")
                val archive = descriptors[index]
                val reportedSize = inputs.getJSONObject(index).optLong(PluginToolInputJson.BYTE_COUNT, archive.statSize)
                fun text(name: String): String? = if (arguments.has(name) && !arguments.isNull(name)) arguments.getString(name) else null
                fun archivePath(name: String, value: String): String {
                    val trimmed = value.trim().trim('/')
                    require(trimmed.isNotEmpty() && trimmed.length <= ArchiveToolCatalog.MAX_PATH_CHARS) { "$name must name a path inside the archive" }
                    return trimmed
                }
                val tree = if (tool == ArchiveToolCatalog.TOOL_EXTRACT) {
                    request.getBundle(PluginToolInvokeKeys.OUTPUT_TREES)?.getBinder(ArchiveToolCatalog.OUTPUT_FILES)?.let(IPluginToolOutputTree.Stub::asInterface)
                        ?: throw IllegalArgumentException("archive_extract writes into its directory output ${ArchiveToolCatalog.OUTPUT_FILES}; name a directory for it")
                } else {
                    null
                }
                val offset = arguments.optInt("offset", 0)
                val limit = arguments.optInt("limit", ArchiveToolCatalog.DEFAULT_LIMIT)
                val maxBytes = arguments.optInt("maxBytes", ArchiveToolCatalog.MAX_READ_BYTES)
                require(offset >= 0) { "offset must not be negative" }
                require(limit in 1..ArchiveToolCatalog.MAX_LIMIT) { "limit must lie between 1 and ${ArchiveToolCatalog.MAX_LIMIT}" }
                require(maxBytes in 1..ArchiveToolCatalog.MAX_READ_BYTES) { "maxBytes must lie between 1 and ${ArchiveToolCatalog.MAX_READ_BYTES}" }
                val password = text("password")?.also {
                    require(it.length in 1..ArchiveToolCatalog.MAX_PASSWORD_CHARS) { "password must have 1 to ${ArchiveToolCatalog.MAX_PASSWORD_CHARS} characters" }
                }?.toCharArray()
                return ToolCall(
                    tool = tool,
                    archive = archive,
                    reportedSize = reportedSize,
                    password = password,
                    directory = text("directory")?.trim()?.trim('/')?.takeIf { it.isNotEmpty() },
                    offset = offset,
                    limit = limit,
                    path = text("path")?.let { archivePath("path", it) }.also {
                        require(tool != ArchiveToolCatalog.TOOL_READ || it != null) { "archive_read needs path" }
                    },
                    maxBytes = maxBytes,
                    paths = arguments.optJSONArray("paths")?.let { array ->
                        require(array.length() in 1..ArchiveToolCatalog.MAX_PATHS) { "paths must list 1 to ${ArchiveToolCatalog.MAX_PATHS} paths" }
                        (0 until array.length()).map { archivePath("paths", array.getString(it)) }.distinct()
                    },
                    tree = tree,
                    descriptors = descriptors,
                )
            }
        }
    }

    private companion object {
        const val WORKERS = 2
        const val MAX_REMEMBERED_CANCELS = 64
        const val MAX_TEXT_LINES = 50
        const val MAX_MESSAGE_CHARS = 4096
        val TOOLS = setOf(ArchiveToolCatalog.TOOL_LIST, ArchiveToolCatalog.TOOL_READ, ArchiveToolCatalog.TOOL_EXTRACT)

        /** ThreadLocal.withInitial needs API 26. */
        val ISO_8601: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
    }
}
