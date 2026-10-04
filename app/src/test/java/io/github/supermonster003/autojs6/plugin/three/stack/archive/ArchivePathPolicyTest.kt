package io.github.supermonster003.autojs6.plugin.three.stack.archive

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
            "safe/unpaired-\uD800.txt",
        )

        invalidPaths.forEach { path ->
            expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_PATH) {
                ArchivePathPolicy.validateEntryPath(path, isDirectory = false)
            }
        }
    }

    @Test
    fun `escapes unsafe source code points for read only display`() {
        assertEquals(
            "../folder/bidi\\u202E.txt\\u000A",
            ArchivePathPolicy.unsafeSourceNameForDisplay("../folder/bidi\u202E.txt\n"),
        )
        assertEquals(
            "(empty name)",
            ArchivePathPolicy.unsafeSourceNameForDisplay(""),
        )
    }

    @Test
    fun `enforces path length and depth limits`() {
        val limits = ArchiveStructureLimits(maxPathLength = 1_024, maxDepth = 64)
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
    fun `caller supplied limits cannot relax hard structure ceilings`() {
        val requested = ArchiveStructureLimits(
            maxEntries = 300_000,
            maxPathNodes = 600_000,
            maxPathLength = 70_000,
            maxDepth = 2_000,
        )
        assertEquals(ArchiveStructureLimits.DEFAULT, requested.restrictedToHardLimits())

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.PATH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath(
                "a".repeat(ArchiveStructureLimits.DEFAULT.maxPathLength + 1),
                isDirectory = false,
                limits = requested,
            )
        }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED) {
            ArchivePathPolicy.validateEntryPath(
                (1..ArchiveStructureLimits.DEFAULT.maxDepth + 1).joinToString("/") { "a" },
                isDirectory = false,
                limits = requested,
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

    @Test
    fun `destination collision keys are case insensitive and Unicode normalized`() {
        assertEquals(
            ArchivePathPolicy.destinationCollisionKey("Archive"),
            ArchivePathPolicy.destinationCollisionKey("archive"),
        )
        assertEquals(
            ArchivePathPolicy.destinationCollisionKey("\u00E9"),
            ArchivePathPolicy.destinationCollisionKey("e\u0301"),
        )
        assertEquals("Folder/File", ArchivePathPolicy.collisionKey("Folder/File"))
    }
}
