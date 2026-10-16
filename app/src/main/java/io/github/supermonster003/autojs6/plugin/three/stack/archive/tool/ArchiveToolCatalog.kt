package io.github.supermonster003.autojs6.plugin.three.stack.archive.tool

/**
 * The catalog this plugin returns from `IPluginToolProvider.getCatalog` (AutoJs6 plugin tool contract v1, MCP Server
 * roadmap P12, decisions D53 - D56): the short name `archive` with `list` and `read` (read-only) and `extract` (write),
 * which AutoJs6 lists as `archive_list`, `archive_read` and `archive_extract`. Archives arrive as file parameters;
 * `extract` writes through the directory output `files`, which may name an existing directory (D56), and uses the
 * COMPATIBLE resource budget. Every tool takes an optional `password` (D55).
 */
internal object ArchiveToolCatalog {

    const val SHORT_NAME = "archive"
    const val TOOL_LIST = "list"
    const val TOOL_READ = "read"
    const val TOOL_EXTRACT = "extract"
    const val OUTPUT_FILES = "files"

    const val DEFAULT_LIMIT = 200
    const val MAX_LIMIT = 1000
    const val MAX_READ_BYTES = 65536
    const val MAX_PATHS = 1000
    const val MAX_PATH_CHARS = 4096
    const val MAX_PASSWORD_CHARS = 1024
    const val MAX_REPORTED_PATHS = 20

    const val READ_TIMEOUT_MS = 120_000L
    const val EXTRACT_TIMEOUT_MS = 300_000L

    private const val FORMATS = "ZIP (also JAR and APK), 7z, RAR, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 or TAR.ZST"
    private const val ARCHIVE = """"archive": {"type": "string", "x-autojs6-type": "file", "description": "The archive file."}"""
    private const val PASSWORD = """"password": {"type": "string", "minLength": 1, "maxLength": $MAX_PASSWORD_CHARS, "description": "Opens encrypted entries (ZIP, 7z, RAR); a 7z archive with encrypted names needs it for anything."}"""

    val JSON: String = """{
  "shortName": "$SHORT_NAME",
  "tools": [
    {
      "name": "$TOOL_LIST",
      "title": "List an archive",
      "description": "Lists the entries of a $FORMATS archive in the AutoJs6 working directory, offline on the phone, a page at a time: path, whether it is a directory, size, packed size, modification time, whether it is encrypted and whether its path is unsafe (archive_extract skips those). directory limits the list to one folder of the archive and everything below it. Split and multi-volume archives are not supported.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ARCHIVE,
          "directory": {"type": "string", "maxLength": $MAX_PATH_CHARS, "description": "Only the entries inside this folder of the archive, for example docs/images."},
          "offset": {"type": "integer", "minimum": 0, "default": 0, "description": "Entries to skip."},
          "limit": {"type": "integer", "minimum": 1, "maximum": $MAX_LIMIT, "default": $DEFAULT_LIMIT, "description": "Entries to return at most."},
          $PASSWORD
        },
        "required": ["archive"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "format": {"type": "string"},
          "total": {"type": "integer"},
          "offset": {"type": "integer"},
          "truncated": {"type": "boolean"},
          "totalUncompressedBytes": {"type": "integer"},
          "entries": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "path": {"type": "string"},
                "directory": {"type": "boolean"},
                "size": {"type": "integer"},
                "packedSize": {"type": "integer"},
                "modified": {"type": "string"},
                "encrypted": {"type": "boolean"},
                "unsafe": {"type": "boolean"}
              },
              "required": ["path", "directory", "size", "encrypted"],
              "additionalProperties": false
            }
          }
        },
        "required": ["format", "total", "offset", "truncated", "totalUncompressedBytes", "entries"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $READ_TIMEOUT_MS,
      "maxConcurrency": 2
    },
    {
      "name": "$TOOL_READ",
      "title": "Read a file inside an archive",
      "description": "Returns the text of one file inside a $FORMATS archive without extracting it, up to maxBytes (at most $MAX_READ_BYTES) decoded as UTF-8; a longer file is cut and marked truncated, and a binary file (NUL bytes in the part read) returns no text but binary true. archive_list shows the paths.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ARCHIVE,
          "path": {"type": "string", "minLength": 1, "maxLength": $MAX_PATH_CHARS, "description": "The path of the file inside the archive, as archive_list shows it."},
          "maxBytes": {"type": "integer", "minimum": 1, "maximum": $MAX_READ_BYTES, "default": $MAX_READ_BYTES, "description": "Bytes to read at most."},
          $PASSWORD
        },
        "required": ["archive", "path"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "path": {"type": "string"},
          "size": {"type": "integer"},
          "bytesRead": {"type": "integer"},
          "truncated": {"type": "boolean"},
          "binary": {"type": "boolean"},
          "text": {"type": "string"}
        },
        "required": ["path", "size", "bytesRead", "truncated", "binary"],
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $READ_TIMEOUT_MS,
      "maxConcurrency": 2
    },
    {
      "name": "$TOOL_EXTRACT",
      "title": "Extract an archive",
      "description": "Extracts a $FORMATS archive, or only the files and folders listed in paths (with their paths inside the archive), into a directory of the AutoJs6 working directory, the output files, offline on the phone. The directory may exist already: files that are there stay as they are and are reported as kept. Unsafe paths and duplicate names are skipped and reported; links are not extracted. At most 100000 entries, 32 GiB per file, 256 GiB in total and a compression ratio of 100000. Split and multi-volume archives are not supported.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ARCHIVE,
          "paths": {"type": "array", "items": {"type": "string", "minLength": 1, "maxLength": $MAX_PATH_CHARS}, "minItems": 1, "maxItems": $MAX_PATHS, "description": "Files and folders of the archive to extract; all of it when absent."},
          $PASSWORD
        },
        "required": ["archive"],
        "additionalProperties": false
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "format": {"type": "string"},
          "filesExtracted": {"type": "integer"},
          "directoriesCreated": {"type": "integer"},
          "bytesWritten": {"type": "integer"},
          "kept": {"type": "integer"},
          "keptPaths": {"type": "array", "items": {"type": "string"}},
          "skipped": {"type": "integer"}
        },
        "required": ["format", "filesExtracted", "directoriesCreated", "bytesWritten", "kept", "keptPaths", "skipped"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $EXTRACT_TIMEOUT_MS,
      "maxConcurrency": 1,
      "outputs": [{"name": "$OUTPUT_FILES", "kind": "directory", "description": "The extracted files and folders."}]
    }
  ]
}"""
}
