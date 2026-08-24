package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Build
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.exception.ZipException as Zip4jException
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.enums.AesVersion
import net.lingala.zip4j.model.enums.CompressionMethod as Zip4jCompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod as Zip4jEncryptionMethod
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile as CommonsZipFile
import java.io.FilterInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Collections
import java.util.Locale

/** Backend-neutral ZIP metadata used by the scanner and extractor. */
internal data class ZipEntryMetadata(
    val name: String,
    val isDirectory: Boolean,
    val method: Int,
    val isEncrypted: Boolean,
    val encryptionMethod: ArchiveEncryptionMethod?,
    val canExtract: Boolean,
    val compressedSize: Long,
    val size: Long,
    val crc: Long?,
    val time: Long,
    internal val backendToken: Any,
    internal val rawName: ByteArray? = null,
    internal val usesUtf8ForNames: Boolean = false,
    internal val nameFromOriginalBytes: Boolean = true,
)

internal interface OpenZipArchive : Closeable {
    val entries: List<ZipEntryMetadata>

    fun getEntry(name: String): ZipEntryMetadata?

    fun getInputStream(entry: ZipEntryMetadata): InputStream
}

/** Opens a ZIP with one stable filename charset for indexing and extraction. */
internal object ZipArchiveAccess {

    fun detectCharset(source: ArchiveReadSource, locale: Locale = Locale.getDefault()): Charset =
        if (requiresZip4jBackend()) {
            detectZip4jCharset(source, locale)
        } else {
            detectCommonsCharset(source, locale)
        }

    fun open(
        source: ArchiveReadSource,
        charsetName: String?,
        password: CharArray? = null,
    ): OpenZipArchive {
        try {
            val charset = charsetName
                ?.let(Charset::forName)
                ?: detectCharset(source)
            return if (requiresZip4jBackend()) {
                Zip4jOpenZipArchive(requireNotNull(source.localFile), charset, password)
            } else {
                openCommons(source, charset, password)
            }
        } finally {
            password?.fill('\u0000')
        }
    }

    fun supportedFilenameCharsetNames(): List<String> = charsetCandidates(Locale.ROOT)
        .map(Charset::name)

    fun hasZipSignature(source: File): Boolean = hasZipSignature(source.asArchiveReadSource())

    fun hasZipSignature(source: ArchiveReadSource): Boolean {
        return try {
            if (!source.isRegularFile || source.identity().length < ZIP_SIGNATURE_SIZE) return false
            source.openInputStream().use { input ->
                val buffer = ByteArray(SIGNATURE_SCAN_BUFFER_SIZE + ZIP_SIGNATURE_SIZE - 1)
                var carry = 0
                var found = false
                while (!found) {
                    val read = input.read(buffer, carry, SIGNATURE_SCAN_BUFFER_SIZE)
                    if (read < 0) break
                    val length = carry + read
                    for (offset in 0..length - ZIP_SIGNATURE_SIZE) {
                        if (isZipSignature(buffer, offset)) {
                            found = true
                            break
                        }
                    }
                    carry = minOf(ZIP_SIGNATURE_SIZE - 1, length)
                    if (carry > 0) {
                        buffer.copyInto(buffer, 0, length - carry, length)
                    }
                }
                found
            }
        } catch (_: IOException) {
            false
        }
    }

    private fun detectCommonsCharset(source: ArchiveReadSource, locale: Locale): Charset {
        val rawNames = openCommons(source, CP437, null).use { archive ->
            archive.entries.asSequence()
                .filterNot(ZipEntryMetadata::usesUtf8ForNames)
                .filter(ZipEntryMetadata::nameFromOriginalBytes)
                .mapNotNull(ZipEntryMetadata::rawName)
                .map(ByteArray::clone)
                .toList()
        }
        if (rawNames.isEmpty() || rawNames.all(::isAscii)) return CP437

        return detectRawFilenameCharset(rawNames, locale) ?: CP437
    }

    /**
     * Android 7 uses Zip4j because Commons Compress calls FileTime methods that are absent on
     * that platform version. Candidate scoring preserves the same filename-encoding behavior.
     */
    private fun detectZip4jCharset(source: ArchiveReadSource, locale: Locale): Charset {
        val localFile = requireNotNull(source.localFile) {
            "Zip4j requires a process-readable archive file"
        }
        ZipCentralDirectoryNameReader.readNonUtf8Names(localFile)?.let { rawNames ->
            // Zip4j on Android 7 can expose UTF-8-flagged names through its configured fallback
            // charset. UTF-8 is therefore the only lossless default when every legacy name is
            // ASCII; ASCII itself decodes identically under all supported candidates.
            if (rawNames.isEmpty()) return UTF8
            detectRawFilenameCharset(rawNames, locale)?.let { return it }
        }
        return charsetCandidates(locale)
            .mapIndexedNotNull { index, charset ->
                val names = runCatching {
                    Zip4jEntryAccess(localFile, charset, null).use { access ->
                        access.headers.map(FileHeader::getFileName)
                    }
                }.getOrNull() ?: return@mapIndexedNotNull null
                val score = names.sumOf { textQuality(it, locale) }
                Triple(charset, score, -index)
            }
            .maxWithOrNull(compareBy<Triple<Charset, Int, Int>> { it.second }.thenBy { it.third })
            ?.first
            ?: CP437
    }

    /** Uses round-trip-safe central-directory bytes instead of backend-decoded candidate names. */
    internal fun detectRawFilenameCharset(
        rawNames: List<ByteArray>,
        locale: Locale,
    ): Charset? = charsetCandidates(locale)
        .mapIndexedNotNull { index, charset ->
            scoreRawNames(charset, rawNames, locale)?.let { Triple(charset, it, -index) }
        }
        .maxWithOrNull(compareBy<Triple<Charset, Int, Int>> { it.second }.thenBy { it.third })
        ?.first

    /** Avoids the Path-based Commons builder branch and gives the archive ownership of the channel. */
    @Suppress("DEPRECATION")
    private fun openCommons(
        source: ArchiveReadSource,
        charset: Charset,
        password: CharArray?,
    ): OpenZipArchive {
        val channel = source.openSeekableChannel()
        val zipFile = try {
            CommonsZipFile(channel, charset.name())
        } catch (error: Throwable) {
            runCatching { channel.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
        return try {
            CommonsOpenZipArchive(zipFile, source, charset, password)
        } catch (error: Throwable) {
            runCatching { zipFile.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }

    /** Commons Compress 1.27+ calls ZipEntry FileTime methods absent from Android 7.x. */
    private fun requiresZip4jBackend(): Boolean {
        val isAndroidRuntime =
            System.getProperty("java.runtime.name").equals("Android Runtime", ignoreCase = true) ||
                System.getProperty("java.vm.name").orEmpty().contains("Dalvik", ignoreCase = true)
        return isAndroidRuntime && Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    }

    private fun scoreRawNames(charset: Charset, rawNames: List<ByteArray>, locale: Locale): Int? {
        var result = 0
        rawNames.forEach { rawName ->
            val decoded = try {
                charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(rawName))
                    .toString()
            } catch (_: CharacterCodingException) {
                return null
            }
            if (!decoded.toByteArray(charset).contentEquals(rawName)) return null
            result += textQuality(decoded, locale)
            val decodedCodePoints = decoded.codePointCount(0, decoded.length)
            result += (rawName.size - decodedCodePoints).coerceAtLeast(0) * MULTIBYTE_COMPACTNESS_BONUS
        }
        return result
    }

    private fun textQuality(value: String, locale: Locale): Int {
        var result = 0
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            result += when {
                codePoint in 0x20..0x7E -> if (Character.isLetterOrDigit(codePoint)) 2 else 1
                isLocaleScript(codePoint, locale) -> 10
                isCjkFilenameScript(codePoint) -> 10
                isCyrillicOrArabic(codePoint) -> 6
                Character.isLetterOrDigit(codePoint) -> 3
                Character.getType(codePoint) in UNSAFE_CHARACTER_TYPES -> -100
                Character.getType(codePoint) in SYMBOL_CHARACTER_TYPES -> -2
                else -> 0
            }
            offset += Character.charCount(codePoint)
        }
        return result
    }

    private fun isLocaleScript(codePoint: Int, locale: Locale): Boolean = when (locale.language) {
        "zh" -> codePoint.isHan()
        "ja" -> codePoint.isHan() || codePoint in 0x3040..0x30FF
        "ko" -> codePoint.isHan() || codePoint in 0xAC00..0xD7AF
        "ru" -> codePoint in 0x0400..0x052F
        "ar" -> codePoint in 0x0600..0x06FF || codePoint in 0x0750..0x077F
        else -> false
    }

    private fun isCjkFilenameScript(codePoint: Int): Boolean =
        codePoint.isHan() ||
            codePoint in 0x3040..0x30FF ||
            codePoint in 0xAC00..0xD7AF

    private fun isCyrillicOrArabic(codePoint: Int): Boolean =
        codePoint in 0x0400..0x052F ||
            codePoint in 0x0600..0x06FF ||
            codePoint in 0x0750..0x077F

    private fun Int.isHan(): Boolean =
        this in 0x3400..0x4DBF || this in 0x4E00..0x9FFF || this in 0xF900..0xFAFF

    private fun charsetCandidates(locale: Locale): List<Charset> = buildList {
        preferredCharsetName(locale)?.let { addIfSupported(it) }
        listOf(
            "UTF-8",
            "GB18030",
            "Shift_JIS",
            "EUC-KR",
            "windows-1251",
            "windows-1256",
            "windows-1252",
            "IBM437",
        ).forEach { addIfSupported(it) }
    }.distinctBy { it.name().uppercase(Locale.ROOT) }

    private fun MutableList<Charset>.addIfSupported(name: String) {
        runCatching { Charset.forName(name) }.getOrNull()?.let(::add)
    }

    private fun preferredCharsetName(locale: Locale): String? = when (locale.language) {
        "zh" -> "GB18030"
        "ja" -> "Shift_JIS"
        "ko" -> "EUC-KR"
        "ru" -> "windows-1251"
        "ar" -> "windows-1256"
        "fr", "es" -> "windows-1252"
        else -> null
    }

    private fun isAscii(value: ByteArray): Boolean = value.all { (it.toInt() and 0x80) == 0 }

    private fun isZipSignature(bytes: ByteArray, offset: Int): Boolean {
        if (bytes[offset] != 'P'.code.toByte() || bytes[offset + 1] != 'K'.code.toByte()) {
            return false
        }
        val third = bytes[offset + 2].toInt() and 0xFF
        val fourth = bytes[offset + 3].toInt() and 0xFF
        return third == 3 && fourth == 4 ||
            third == 5 && fourth == 6 ||
            third == 7 && fourth == 8
    }

    private val CP437: Charset = Charset.forName("IBM437")
    private val UTF8: Charset = Charset.forName("UTF-8")
    private const val MULTIBYTE_COMPACTNESS_BONUS = 4
    private const val SIGNATURE_SCAN_BUFFER_SIZE = 64 * 1024
    private const val ZIP_SIGNATURE_SIZE = 4
    private val UNSAFE_CHARACTER_TYPES = setOf(
        Character.CONTROL.toInt(),
        Character.FORMAT.toInt(),
        Character.PRIVATE_USE.toInt(),
        Character.SURROGATE.toInt(),
        Character.UNASSIGNED.toInt(),
    )
    private val SYMBOL_CHARACTER_TYPES = setOf(
        Character.MATH_SYMBOL.toInt(),
        Character.CURRENCY_SYMBOL.toInt(),
        Character.MODIFIER_SYMBOL.toInt(),
        Character.OTHER_SYMBOL.toInt(),
    )
}

private class CommonsOpenZipArchive(
    private val zipFile: CommonsZipFile,
    source: ArchiveReadSource,
    charset: Charset,
    password: CharArray?,
) : OpenZipArchive {
    private val sourceEntries = buildList {
        val entries = zipFile.entries
        while (entries.hasMoreElements()) add(entries.nextElement())
    }
    private val encryptedEntryAccess: Zip4jEntryAccess?
    override val entries: List<ZipEntryMetadata>

    init {
        var openedEncryptedAccess: Zip4jEntryAccess? = null
        try {
            val localFile = source.localFile
            if (sourceEntries.any { it.generalPurposeBit.usesEncryption() } && localFile == null) {
                throw ArchiveLocalFileRequiredException(
                    "Encrypted ZIP access requires a private local copy",
                )
            }
            openedEncryptedAccess = if (
                sourceEntries.any { it.generalPurposeBit.usesEncryption() } && localFile != null
            ) {
                Zip4jEntryAccess(localFile, charset, password)
            } else {
                null
            }
            entries = buildList {
                sourceEntries.forEachIndexed { ordinal, entry ->
                    val encryptedHeader = if (entry.generalPurposeBit.usesEncryption()) {
                        openedEncryptedAccess?.claimHeader(ordinal, entry.name)
                    } else {
                        null
                    }
                    add(
                        ZipEntryMetadata(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            method = encryptedHeader?.actualCompressionMethodCode() ?: entry.method,
                            isEncrypted = entry.generalPurposeBit.usesEncryption(),
                            encryptionMethod = encryptedHeader?.archiveEncryptionMethod(),
                            canExtract = if (entry.generalPurposeBit.usesEncryption()) {
                                encryptedHeader?.canExtractWithZip4j() == true
                            } else {
                                zipFile.canReadEntryData(entry)
                            },
                            compressedSize = entry.compressedSize,
                            size = entry.size,
                            crc = if (encryptedHeader != null) {
                                encryptedHeader.archiveCrc()
                            } else {
                                entry.crc.takeIf { it >= 0L }
                            },
                            time = entry.time,
                            backendToken = CommonsZipEntryToken(entry, encryptedHeader),
                            rawName = entry.rawName,
                            usesUtf8ForNames = entry.generalPurposeBit.usesUTF8ForNames(),
                            nameFromOriginalBytes = entry.nameSource == ZipArchiveEntry.NameSource.NAME,
                        ),
                    )
                }
            }
            encryptedEntryAccess = openedEncryptedAccess
        } catch (error: Throwable) {
            runCatching { openedEncryptedAccess?.close() }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            throw error
        }
    }
    private val entriesByName = entries.associateBy(ZipEntryMetadata::name)

    override fun getEntry(name: String): ZipEntryMetadata? = entriesByName[name]

    override fun getInputStream(entry: ZipEntryMetadata): InputStream {
        val token = entry.backendToken as CommonsZipEntryToken
        return token.encryptedHeader?.let { encryptedHeader ->
            checkNotNull(encryptedEntryAccess).getInputStream(encryptedHeader)
        } ?: zipFile.getInputStream(token.commonsEntry)
    }

    override fun close() {
        var failure: Throwable? = null
        try {
            encryptedEntryAccess?.close()
        } catch (error: Throwable) {
            failure = error
        }
        try {
            zipFile.close()
        } catch (error: Throwable) {
            failure?.addSuppressed(error) ?: run { failure = error }
        }
        failure?.let { throw it }
    }
}

private data class CommonsZipEntryToken(
    val commonsEntry: ZipArchiveEntry,
    val encryptedHeader: FileHeader?,
)

private class Zip4jOpenZipArchive(
    source: File,
    charset: Charset,
    password: CharArray?,
) : OpenZipArchive {
    private val access: Zip4jEntryAccess
    override val entries: List<ZipEntryMetadata>

    init {
        val openedAccess = Zip4jEntryAccess(source, charset, password)
        try {
            entries = openedAccess.headers.map { header ->
                ZipEntryMetadata(
                    name = header.fileName,
                    isDirectory = header.isDirectory,
                    method = header.actualCompressionMethodCode(),
                    isEncrypted = header.isEncrypted,
                    encryptionMethod = header.archiveEncryptionMethod(),
                    canExtract = header.canExtractWithZip4j(),
                    compressedSize = header.compressedSize,
                    size = header.uncompressedSize,
                    crc = header.archiveCrc(),
                    time = header.lastModifiedTimeEpoch,
                    backendToken = header,
                )
            }
            access = openedAccess
        } catch (error: Throwable) {
            runCatching { openedAccess.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }
    private val entriesByName = entries.associateBy(ZipEntryMetadata::name)

    override fun getEntry(name: String): ZipEntryMetadata? = entriesByName[name]

    override fun getInputStream(entry: ZipEntryMetadata): InputStream =
        access.getInputStream(entry.backendToken as FileHeader)

    override fun close() = access.close()
}

/** Owns the password-bearing Zip4j handle used for encrypted entry streams. */
private class Zip4jEntryAccess(
    source: File,
    charset: Charset,
    password: CharArray?,
) : Closeable {
    private val passwordChars = password?.clone()
    private val zipFile = Zip4jFile(source, passwordChars).apply { setCharset(charset) }
    private val claimedHeaders = Collections.newSetFromMap(
        java.util.IdentityHashMap<FileHeader, Boolean>(),
    )

    val headers: List<FileHeader> = zipFile.fileHeaders

    init {
        try {
            validatePasswordIfProvided()
        } catch (error: Throwable) {
            runCatching { zipFile.close() }.exceptionOrNull()?.let(error::addSuppressed)
            passwordChars?.fill('\u0000')
            throw error
        }
    }

    fun claimHeader(ordinal: Int, name: String): FileHeader? {
        val ordinalMatch = headers.getOrNull(ordinal)
            ?.takeIf { it !in claimedHeaders }
        val result = ordinalMatch ?: headers.firstOrNull {
            it.fileName == name && it !in claimedHeaders
        }
        result?.let(claimedHeaders::add)
        return result
    }

    fun getInputStream(header: FileHeader): InputStream = try {
        Zip4jFailureMappingInputStream(zipFile.getInputStream(header))
    } catch (error: IOException) {
        throw error.asArchiveEntryException()
    }

    private fun validatePasswordIfProvided() {
        if (passwordChars == null) return
        val encryptedHeader = headers.firstOrNull {
            it.isEncrypted && !it.isDirectory && it.canExtractWithZip4j()
        } ?: return
        zipFile.getInputStream(encryptedHeader).use { }
    }

    override fun close() {
        try {
            zipFile.close()
        } finally {
            passwordChars?.fill('\u0000')
        }
    }
}

private class Zip4jFailureMappingInputStream(
    input: InputStream,
) : FilterInputStream(input) {
    override fun read(): Int = mapFailure { super.read() }

    override fun read(buffer: ByteArray): Int = mapFailure { super.read(buffer) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        mapFailure { super.read(buffer, offset, length) }

    override fun skip(byteCount: Long): Long = mapFailure { super.skip(byteCount) }

    override fun close() = mapFailure { super.close() }

    private inline fun <T> mapFailure(action: () -> T): T = try {
        action()
    } catch (error: IOException) {
        throw error.asArchiveEntryException()
    }
}

private fun FileHeader.actualCompressionMethodCode(): Int =
    if (compressionMethod == Zip4jCompressionMethod.AES_INTERNAL_ONLY) {
        aesExtraDataRecord?.compressionMethod?.code ?: compressionMethod.code
    } else {
        compressionMethod.code
    }

private fun FileHeader.archiveEncryptionMethod(): ArchiveEncryptionMethod? = when {
    !isEncrypted -> null
    encryptionMethod == Zip4jEncryptionMethod.ZIP_STANDARD -> ArchiveEncryptionMethod.ZIP_CRYPTO
    encryptionMethod == Zip4jEncryptionMethod.AES -> ArchiveEncryptionMethod.AES
    else -> ArchiveEncryptionMethod.OTHER
}

private fun FileHeader.canExtractWithZip4j(): Boolean =
    (!isEncrypted || encryptionMethod in ZIP4J_SUPPORTED_ENCRYPTION_METHODS) &&
        actualCompressionMethodCode() in ZIP4J_SUPPORTED_COMPRESSION_METHOD_CODES

private fun FileHeader.archiveCrc(): Long? =
    crc.takeIf {
        it >= 0L &&
            !(encryptionMethod == Zip4jEncryptionMethod.AES &&
                aesExtraDataRecord?.aesVersion == AesVersion.TWO &&
                it == 0L)
    }

private fun IOException.asArchiveEntryException(): ArchiveExtractionException {
    val zip4jError = generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<Zip4jException>()
        .firstOrNull()
    val code = when (zip4jError?.type) {
        Zip4jException.Type.WRONG_PASSWORD -> ArchiveFailureCode.WRONG_PASSWORD
        Zip4jException.Type.CHECKSUM_MISMATCH -> ArchiveFailureCode.CRC_MISMATCH
        Zip4jException.Type.UNKNOWN_COMPRESSION_METHOD,
        Zip4jException.Type.UNSUPPORTED_ENCRYPTION,
        -> ArchiveFailureCode.UNSUPPORTED_METHOD
        else -> ArchiveFailureCode.MALFORMED_ARCHIVE
    }
    return ArchiveExtractionException(
        code = code,
        message = when (code) {
            ArchiveFailureCode.WRONG_PASSWORD -> "ZIP password is incorrect"
            ArchiveFailureCode.CRC_MISMATCH -> "ZIP entry checksum verification failed"
            ArchiveFailureCode.UNSUPPORTED_METHOD ->
                "ZIP entry uses an unsupported compression or encryption method"
            else -> "ZIP entry data cannot be read"
        },
        cause = this,
        format = ArchiveFormat.ZIP,
        stage = if (code == ArchiveFailureCode.WRONG_PASSWORD) {
            ArchiveFailureStage.PASSWORD
        } else {
            ArchiveFailureStage.ENTRY_DATA
        },
    )
}

private val ZIP4J_SUPPORTED_COMPRESSION_METHOD_CODES = setOf(
    Zip4jCompressionMethod.STORE.code,
    Zip4jCompressionMethod.DEFLATE.code,
)
private val ZIP4J_SUPPORTED_ENCRYPTION_METHODS = setOf(
    Zip4jEncryptionMethod.ZIP_STANDARD,
    Zip4jEncryptionMethod.AES,
)
