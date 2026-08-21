package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchivePathPolicyTest {

    @Test
    fun `preserves the original Unicode spelling of a valid entry path`() {
        val result = ArchivePathPolicy.validateEntryPath("folder/e\u0301.txt", isDirectory = false)

        assertEquals("folder/e\u0301.txt", result.path)
        assertEquals("e\u0301.txt", result.displayName)
        assertEquals(listOf("folder", "e\u0301.txt"), result.segments)
    }

    @Test
    fun `normalizes portable separators and harmless path segments`() {
        val result = ArchivePathPolicy.validateEntryPath("folder\\.\\nested//file.txt", isDirectory = false)

        assertEquals("folder/nested/file.txt", result.path)
        assertEquals("file.txt", result.displayName)
    }

    @Test
    fun `rejects traversal absolute drive and control paths`() {
        val invalidPaths = listOf(
            "../escape.txt",
            "safe/../escape.txt",
            "/absolute.txt",
            "safe\\..\\escape.txt",
            "C:/escape.txt",
            "safe/C:/escape.txt",
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
        val limits = ArchiveSecurityLimits(maxPathLength = 1_024, maxDepth = 64)
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.PATH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath("a".repeat(1_025), isDirectory = false, limits = limits)
        }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath(
                (1..65).joinToString("/") { "a" },
                isDirectory = false,
                limits = limits,
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
