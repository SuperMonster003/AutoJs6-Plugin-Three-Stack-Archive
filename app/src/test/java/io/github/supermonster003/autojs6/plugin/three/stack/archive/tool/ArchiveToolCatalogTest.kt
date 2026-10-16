package io.github.supermonster003.autojs6.plugin.three.stack.archive.tool

import org.autojs.plugin.tool.api.PluginToolLimits
import org.autojs.plugin.tool.api.PluginToolNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ArchiveToolCatalogTest {

    @Test
    fun catalogDeclaresListReadAndExtractWithTheDirectoryOutput() {
        val json = ArchiveToolCatalog.JSON
        assertTrue(PluginToolNames.isValidShortName(ArchiveToolCatalog.SHORT_NAME))
        assertFalse(PluginToolNames.isReservedShortName(ArchiveToolCatalog.SHORT_NAME))
        listOf("list", "read", "extract").forEach { tool ->
            assertTrue(json.contains(""""name": "$tool""""))
            assertTrue(PluginToolNames.fitsMcpName(PluginToolNames.fullName(ArchiveToolCatalog.SHORT_NAME, tool)))
        }
        assertTrue(json.toByteArray(Charsets.UTF_8).size < PluginToolLimits.MAX_CATALOG_BYTES)
        assertEquals("every tool takes the archive as a file", 3, Regex(""""archive": \{"type": "string", "x-autojs6-type": "file"""").findAll(json).count())
        assertEquals("every tool takes a password", 3, Regex(""""password": \{"type": "string", "minLength": 1, "maxLength": 1024""").findAll(json).count())
        assertEquals(2, Regex(""""risk": "readonly"""").findAll(json).count())
        assertEquals(1, Regex(""""risk": "write"""").findAll(json).count())
        assertTrue(json.contains(""""outputs": [{"name": "files", "kind": "directory""""))
        Regex(""""description": "([^"]*(?:\\"[^"]*)*)",\n""").findAll(json).forEach {
            assertTrue(it.groupValues[1].length <= PluginToolLimits.MAX_DESCRIPTION_CHARS)
        }
        assertTrue("descriptions stay ASCII", json.all { it.code in 0x0a..0x7e })
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
        assertEquals("every object of the schemas is closed", json.split("\"type\": \"object\"").size - 1, json.split("\"additionalProperties\": false").size - 1)
    }

    @Test
    fun thePluginToolAarMatchesItsLock() {
        val libs = File("../libs").takeIf { it.isDirectory } ?: File("libs")
        val lock = File(libs, "plugin-tool-api.lock.json").readText()
        val aar = File(libs, "plugin-tool-api.aar").readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(aar).joinToString("") { "%02x".format(it) }
        assertTrue("the lock names the AAR SHA-256", lock.contains(""""sha256": "$sha256""""))
        assertTrue("the lock names the AAR length", lock.contains(""""length": ${aar.size}"""))
    }
}
