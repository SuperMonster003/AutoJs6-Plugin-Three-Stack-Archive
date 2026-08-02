package io.github.supermonster003.autojs6.plugin.archivebrowser

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchivePathPolicyTest {

    @Test
    fun `normalizes valid entry path to NFC`() {
        val result = ArchivePathPolicy.validateEntryPath("folder/e\u0301.txt", isDirectory = false)

        assertEquals("folder/\u00E9.txt", result.path)
        assertEquals("\u00E9.txt", result.displayName)
        assertEquals(listOf("folder", "\u00E9.txt"), result.segments)
    }

    @Test
    fun `rejects traversal absolute backslash drive and control paths`() {
        val invalidPaths = listOf(
            "../escape.txt",
            "safe/../escape.txt",
            "/absolute.txt",
            "safe\\escape.txt",
            "C:/escape.txt",
            "safe/C:/escape.txt",
            "safe//empty.txt",
            "safe/./dot.txt",
            "safe/control\u0000.txt",
            "safe/bidi\u202Etxt",
        )

        invalidPaths.forEach { path ->
            expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_PATH) {
                ArchivePathPolicy.validateEntryPath(path, isDirectory = false)
            }
        }
    }

    @Test
    fun `enforces path length and depth limits`() {
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.PATH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath("a".repeat(1_025), isDirectory = false)
        }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath(
                (1..65).joinToString("/") { "a" },
                isDirectory = false,
            )
        }
    }

    @Test
    fun `validates destination root as one safe segment`() {
        assertEquals("Archive", ArchivePathPolicy.validateDestinationRootName("Archive"))
        listOf("", "..", "a/b", "a\\b", "C:", "bad\nname").forEach { name ->
            expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ) {
                ArchivePathPolicy.validateDestinationRootName(name)
            }
        }
    }
}
