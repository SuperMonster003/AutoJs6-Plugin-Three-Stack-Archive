# Release notes

## Unreleased

_Unreleased_

- `Added` Renamed the product to Archive Manager and the file action to Open archive
- `Added` Explorer Action v5 browses archives in the native AutoJs6 file list with the existing path bar, theme, and Back navigation
- `Added` Explorer Action v6 opens supported archive entries with the host document, image, audio, and video viewers
- `Added` Extract to... shortcut for choosing a destination and extracting the entire archive
- `Added` Explorer Action v4 adds Compress... to ordinary file and folder menus and to the five-action same-parent multi-selection bar
- `Added` ZIP creation with default naming, compression levels, progress, cancellation, and automatic conflict numbering
- `Added` Non-solid 7Z creation with levels 0 through 9 and optional AES-256 content encryption; filenames remain visible and filename encryption is not misreported
- `Added` TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST creation with format-specific levels and complete compound-extension updates
- `Added` Browse and extract ZipCrypto/AES encrypted ZIP files, retry a wrong password in place, and optionally create AES-256 ZIP files whose names remain visible with matching password confirmation
- `Added` Browse, preview, and extract ordinary or solid 7Z archives with common compression/filter pipelines, AES content encryption, and header encryption; missing and wrong passwords receive explicit diagnostics
- `Added` Browse, preview, and extract uncompressed TAR files in the native host list with header-checksum validation; links, device nodes, and sparse entries remain list-only
- `Added` Browse, preview, and extract TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST through the same native host paths; format detection verifies both the compressor signature and inner TAR structure
- `Added` Compatible, Strict, and Custom extraction budgets; over-budget archives remain read-only browsable and show estimated output, exceeded dimensions, and one-time confirmation before writing
- `Fixed` ZIP listings now use directory metadata and handle self-extracting-style preambles, legacy filename encodings, Windows separators, and more readable ZIP methods
- `Fixed` Standard `.z01 + .zip` split ZIP files now identify the required earlier volumes instead of reporting the final volume as damaged
- `Fixed` ZIP filename encoding can be overridden when automatic detection is wrong, and extraction reuses the selected encoding
- `Fixed` Unknown or imprecise sizes, valid DocumentsProvider URIs, and extra host write grants no longer reject a valid archive before parsing
- `Fixed` ZIP browsing and extraction now work on Android 7.x without calling runtime APIs that only exist on newer systems
- `Fixed` Wrong passwords now map consistently to PASSWORD/WRONG_PASSWORD, and AES v2 entries with a zero stored CRC are no longer misreported as damaged
- `Fixed` Default extraction folders for compound extensions such as TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST now remove the complete suffix instead of retaining `.tar`
- `Fixed` Archives containing parent traversal, absolute, drive-prefixed, or control-character names remain browsable; unsafe names move to a read-only isolation folder, remain previewable when their data is readable, and require explicit skipping before extraction
- `Fixed` Extraction no longer overwrites existing files or directories when a destination provider treats case or Unicode-equivalent names as identical; equivalent output-folder names are numbered automatically
- `Improved` Removed the fixed 4 GiB input cap and browse-time extraction-size/ratio gates while retaining path containment, integrity checks, and failure cleanup
- `Improved` Added a checkable Roadmap and rewrote README and CHANGELOG to separate current behavior from planned work
- `Improved` The transitional standalone screen now follows system day/night mode and Material dynamic colors
- `Improved` ZIP, 7Z, and TAR-family output streams through a UID-bound host session to same-directory temporary output and commits atomically without storage permission or overwriting existing files
- `Improved` Archive format and entry capabilities are checked consistently across preview, extraction, and creation, so unavailable options stay disabled
- `Improved` Archive failures identify the format, processing stage, stable code, and reason; debug builds can copy complete diagnostics
- `Improved` Ordinary archives now browse through independent positional channels over the host's read-only descriptor without a whole-file copy; incompatible inputs or readers (currently including encrypted ZIP) fall back to private cache, which is cleaned on close, failure, or expiry
- `Dependency` Added Apache License 2.0 licensed Zip4j 2.11.5 for encrypted ZIP streams, AES-256 creation, and the Android 7.x compatibility path
- `Dependency` Added 0BSD-licensed XZ for Java 1.12 for pure-Java TAR.XZ/TXZ reading and writing without native ABIs
- `Dependency` Added BSD-licensed zstd-jni 1.5.7-15 for TAR.ZST/TZST reading and writing; all four Android ABIs pass 16 KiB ELF alignment and RELRO checks

## v1.0.1

_2026/08/08_

- `Fixed` Empty service binding when enabling the plugin in Plugin Center
- `Improved` Simplified the plugin name, description, and usage text

## v1.0.0

_2026/08/02_

- `Added` Initial release for browsing ZIP, JAR, AAR, and WAR files and extracting selected files or folders
- `Added` Added search, selection, progress, cancellation, temporary-input cleanup, and localized UI
