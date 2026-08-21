package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Build
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile as CommonsZipFile
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as PlatformZipFile

/** Backend-neutral ZIP metadata used by the scanner and extractor. */
internal data class ZipEntryMetadata(
    val name: String,
    val isDirectory: Boolean,
    val method: Int,
    val isEncrypted: Boolean,
    val canExtract: Boolean,
    val compressedSize: Long,
    val size: Long,
    val crc: Long,
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

    fun detectCharset(source: File, locale: Locale = Locale.getDefault()): Charset =
        if (requiresPlatformBackend()) {
            detectPlatformCharset(source, locale)
        } else {
            detectCommonsCharset(source, locale)
        }

    fun open(source: File, charsetName: String?): OpenZipArchive {
        val charset = charsetName
            ?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: detectCharset(source)
        return if (requiresPlatformBackend()) {
            openPlatform(source, charset)
        } else {
            openCommons(source, charset)
        }
    }

    private fun detectCommonsCharset(source: File, locale: Locale): Charset {
        val rawNames = openCommons(source, CP437).use { archive ->
            archive.entries.asSequence()
                .filterNot(ZipEntryMetadata::usesUtf8ForNames)
                .filter(ZipEntryMetadata::nameFromOriginalBytes)
                .mapNotNull(ZipEntryMetadata::rawName)
                .map(ByteArray::clone)
                .toList()
        }
        if (rawNames.isEmpty() || rawNames.all(::isAscii)) return CP437

        return charsetCandidates(locale)
            .mapIndexedNotNull { index, charset ->
                scoreRawNames(charset, rawNames, locale)?.let { Triple(charset, it, -index) }
            }
            .maxWithOrNull(compareBy<Triple<Charset, Int, Int>> { it.second }.thenBy { it.third })
            ?.first
            ?: CP437
    }

    /**
     * Android 7's platform ZIP reader supports a caller-supplied legacy charset. Trying the
     * candidates also rejects byte sequences that a charset cannot decode.
     */
    private fun detectPlatformCharset(source: File, locale: Locale): Charset {
        return charsetCandidates(locale)
            .mapIndexedNotNull { index, charset ->
                val names = runCatching {
                    PlatformZipFile(source, charset).use { zipFile ->
                        buildList {
                            val entries = zipFile.entries()
                            while (entries.hasMoreElements()) add(entries.nextElement().name)
                        }
                    }
                }.getOrNull() ?: return@mapIndexedNotNull null
                val score = names.sumOf { textQuality(it, locale) }
                Triple(charset, score, -index)
            }
            .maxWithOrNull(compareBy<Triple<Charset, Int, Int>> { it.second }.thenBy { it.third })
            ?.first
            ?: CP437
    }

    /** Avoids the Path-based Commons builder branch and gives the archive ownership of the channel. */
    @Suppress("DEPRECATION")
    private fun openCommons(source: File, charset: Charset): OpenZipArchive {
        val channel = RandomAccessFile(source, "r").channel
        val zipFile = try {
            CommonsZipFile(channel, charset.name())
        } catch (error: Throwable) {
            runCatching { channel.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
        return try {
            CommonsOpenZipArchive(zipFile)
        } catch (error: Throwable) {
            runCatching { zipFile.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }

    private fun openPlatform(source: File, charset: Charset): OpenZipArchive =
        PlatformOpenZipArchive(PlatformZipFile(source, charset))

    /** Commons Compress 1.27+ calls ZipEntry FileTime methods absent from Android 7.x. */
    private fun requiresPlatformBackend(): Boolean {
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

    private val CP437: Charset = Charset.forName("IBM437")
    private const val MULTIBYTE_COMPACTNESS_BONUS = 4
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
) : OpenZipArchive {
    override val entries: List<ZipEntryMetadata> = buildList {
        val sourceEntries = zipFile.entries
        while (sourceEntries.hasMoreElements()) {
            val entry = sourceEntries.nextElement()
            add(
                ZipEntryMetadata(
                    name = entry.name,
                    isDirectory = entry.isDirectory,
                    method = entry.method,
                    isEncrypted = entry.generalPurposeBit.usesEncryption(),
                    canExtract = zipFile.canReadEntryData(entry),
                    compressedSize = entry.compressedSize,
                    size = entry.size,
                    crc = entry.crc,
                    time = entry.time,
                    backendToken = entry,
                    rawName = entry.rawName,
                    usesUtf8ForNames = entry.generalPurposeBit.usesUTF8ForNames(),
                    nameFromOriginalBytes = entry.nameSource == ZipArchiveEntry.NameSource.NAME,
                ),
            )
        }
    }
    private val entriesByName = entries.associateBy(ZipEntryMetadata::name)

    override fun getEntry(name: String): ZipEntryMetadata? = entriesByName[name]

    override fun getInputStream(entry: ZipEntryMetadata): InputStream =
        zipFile.getInputStream(entry.backendToken as ZipArchiveEntry)

    override fun close() = zipFile.close()
}

private class PlatformOpenZipArchive(
    private val zipFile: PlatformZipFile,
) : OpenZipArchive {
    override val entries: List<ZipEntryMetadata> = buildList {
        val sourceEntries = zipFile.entries()
        while (sourceEntries.hasMoreElements()) {
            val entry = sourceEntries.nextElement()
            add(
                ZipEntryMetadata(
                    name = entry.name,
                    isDirectory = entry.isDirectory,
                    method = entry.method,
                    // The Android 7 platform API does not expose the general-purpose ZIP flags.
                    isEncrypted = false,
                    canExtract = entry.method == ZipEntry.STORED || entry.method == ZipEntry.DEFLATED,
                    compressedSize = entry.compressedSize,
                    size = entry.size,
                    crc = entry.crc,
                    time = entry.time,
                    backendToken = entry,
                ),
            )
        }
    }
    private val entriesByName = entries.associateBy(ZipEntryMetadata::name)

    override fun getEntry(name: String): ZipEntryMetadata? = entriesByName[name]

    override fun getInputStream(entry: ZipEntryMetadata): InputStream =
        zipFile.getInputStream(entry.backendToken as ZipEntry)

    override fun close() = zipFile.close()
}
