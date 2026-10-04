# Release notes

## v3.0.0

_2026/10/04_

- `Note` The application ID changes from io.github.supermonster003.autojs6.plugin.archivemanager to io.github.supermonster003.autojs6.plugin.three.stack.archive. Android installs this as a separate app; existing apps and data can remain, and settings are not migrated automatically
- `Added` A standalone home screen opens archives for browsing and extraction; creation and in-place changes remain available from AutoJs6
- `Added` Language, dark mode, theme color and four launcher icon choices are available in the shared settings layout

## v2.22.3

_2026/09/19_

- `Fixed` SDK XML v4 parsing warnings with AGP 9.1 and APK native alignment checks incorrectly triggered by JVM unit-test assembly tasks, using shared build plugins 1.8.3
- `Improved` Raise targetSdk to 37 (Android 17) after compileSdk; the plugin's behavior does not depend on the new target

## v2.22.2

_2026/09/13_

- `Fixed` Plugin center version and ABI information matches the installed plugin APK
- `Fixed` Version dates use a consistent English format
- `Improved` Validate release APK versions, signing and the complete variant set before creating download artifacts

## v2.22.1

_2026/09/11_

- `Improved` Build verification of 16 KB page alignment for 64-bit native libraries, including manifest contract checks and JSON reports

## v2.22.0

_2026/08/30_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v21 (version code 5276 or newer)
- `Added` Plugin Center can now identify the required host version, runtime service, and packaged device architectures directly from the plugin
- `Fixed` Public descriptions and instructions now distinguish browsing, extraction, creation, editing, encryption, and volume support without overstating writable formats
- `Fixed` Archive input grants and active-operation state now close before the terminal callback, so an immediate retry is no longer rejected as another operation already running
- `Improved` A format capability matrix and installation guide now make compatibility, read-only boundaries, recovery behavior, and troubleshooting easier to check before installation

## v2.21.0

_2026/08/30_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v21 (version code 5276 or newer)
- `Added` Archive creation now keeps a completion page and Source recovery history; sources moved to Trash by explicit opt-in can be restored after Activity or host-session recreation
- `Fixed` Source restore never overwrites an existing name or deletes a created archive; conflicts and partial or interrupted restores retain recovery evidence and report per-item outcomes
- `Improved` The host owns the bounded durable history and exposes only opaque metadata; recovery UI stays inside the plugin compression page and the ordinary file-manager layout remains unchanged

## v2.20.0

_2026/08/30_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v20 (version code 5276 or newer)
- `Added` Writable native archive pages can now create empty folders and add an explicit mixed selection of files and complete folder trees; empty folders are preserved and same-named directory roots are safely numbered
- `Fixed` The frozen input snapshot is revalidated before commit; cancellation, source changes, equivalent-name ambiguity inside a selected tree, or a direct-file conflict fail the whole addition and preserve the original archive
- `Improved` Explorer Action v20 exposes only bounded opaque nodes, metadata, and one-shot read-only descriptors for the selected inputs; it grants no paths, URIs, unselected siblings, or general storage access, and leaves the ordinary file-manager layout unchanged

## v2.19.0

_2026/08/29_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v19 (version code 5276 or newer)
- `Added` Files and folders in writable archives can now be renamed or deleted directly in the native AutoJs6 archive list, including multi-selection deletion; dialogs, progress, path navigation, and selection restoration reuse the host framework
- `Fixed` Delete and rename actions are now gated by both session and per-entry capabilities; unsafe paths, missing volumes, RAR, split archives, and other read-only variants never advertise unavailable mutations
- `Improved` Explorer Action v19 passes only bounded opaque entry IDs and a safe leaf name, then rebuilds through host-owned pending output with complete readback, atomic replacement, and in-place reindexing; the ordinary file-manager layout and visual style are unchanged

## v2.18.0

_2026/08/29_

- `Note` This release still requires the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Improved` Safe ordinary 7Z and every writable TAR-family format now share a format-neutral full-rewrite planner; each backend explicitly supplies capabilities and retained-entry validation, removing the former TAR naming and coupling without changing supported formats, output settings, or read-only boundaries

## v2.17.0

_2026/08/29_

- `Note` This release still requires the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` Ordinary single-volume, unencrypted, non-solid 7Z archives within the decoder budget can now add files or complete folder trees, create empty folders, rename, and delete from the management page
- `Fixed` The management action now includes `.7z`; encrypted, solid, split, unsafe, unsupported-method, and over-budget 7Z variants remain read-only after structural inspection
- `Improved` 7Z edits rebuild to bounded non-solid LZMA2 output and fully read it back before atomic replacement; cancellation, source changes, damaged pending output, and write failures preserve the original

## v2.16.0

_2026/08/29_

- `Note` This release continues to require the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` TAR.ZST and TZST archives containing only safe regular files and directories can now add files or complete folder trees, create empty folders, rename, and delete from the management page
- `Fixed` The management action now recognizes `.tar.zst` and `.tzst` precisely; every supported TAR wrapper now uses the same verified writable rebuild boundary
- `Improved` TAR.ZST changes use bounded single-threaded Zstandard level 3 with a 1 MiB window and frame checksum, and stream directly into host-owned pending output; cancellation, checksum integrity, real write failures, source changes, and complete readback remain inside the rollback boundary

## v2.15.0

_2026/08/29_

- `Note` This release continues to require the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` TAR.BZ2 and TBZ2 archives containing only safe regular files and directories can now add files or complete folder trees, create empty folders, rename, and delete from the management page
- `Fixed` The management action now recognizes `.tar.bz2` and `.tbz2` precisely; TAR.ZST/TZST is the only compressed TAR wrapper that remains read-only
- `Improved` TAR.BZ2 changes use bounded BZIP2 block-size preset 6 and stream directly into host-owned pending output; cancellation, trailer integrity, real write failures, source changes, and complete readback remain inside the rollback boundary

## v2.14.0

_2026/08/29_

- `Note` This release continues to require the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` TAR.XZ and TXZ archives containing only safe regular files and directories can now add files or complete folder trees, create empty folders, rename, and delete from the management page
- `Fixed` The management action now recognizes `.tar.xz` and `.txz` precisely while TAR.BZ2/TBZ2 and TAR.ZST/TZST remain read-only
- `Improved` TAR.XZ changes use fixed XZ preset 4 (4 MiB dictionary; 48,058 KiB library-reported encoder memory, below a 64 MiB budget) and stream directly into host-owned pending output; cancellation, container-trailer integrity, real write failures, source changes, and complete readback remain inside the rollback boundary

## v2.13.0

_2026/08/29_

- `Note` This release continues to require the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` TAR.GZ and TGZ archives containing only safe regular files and directories can now add files or complete folder trees, create empty folders, rename, and delete from the management page
- `Fixed` The management action now recognizes `.tar.gz` and `.tgz` precisely while continuing to reject JAR/AAR/WAR and the read-only TAR.XZ, TAR.BZ2, and TAR.ZST wrappers
- `Improved` TAR.GZ changes rebuild the compressed source stream directly into host-owned pending output without a private uncompressed TAR copy; cancellation, GZIP trailer integrity, write failures, and complete readback all remain inside the rollback boundary

## v2.12.0

_2026/08/28_

- `Note` This release continues to require the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` Ordinary uncompressed TAR archives containing only safe regular files and directories can now add files or folder trees, create empty folders, rename, and delete from the management page
- `Fixed` Archive information now describes format-specific extended metadata without incorrectly labeling TAR metadata as ZIP extra fields
- `Improved` TAR changes use one sequential source pass, show factual work before reserving output, preserve available modification times, emit POSIX/PAX names when needed, fully read back the replacement, and reuse the host's atomic replacement and recent-version restore flow

## v2.11.0

_2026/08/28_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` After a successful ZIP edit, Restore previous version is available from the success message and the management-page menu for one rollback during bounded host retention
- `Fixed` SAF output streams now request explicit truncation, preventing shorter overwrites from retaining bytes from the previous content on newer Android versions
- `Improved` The host keeps the previous archive in private durable storage, restores only while the target is still the exact committed replacement, and preserves interrupted recovery evidence instead of overwriting external changes

## v2.10.0

_2026/08/28_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v17 (version code 5276 or newer)
- `Added` A file whose name or extension is not recognized can now use Open as archive...; after structural detection, the native path bar labels the actual format when it differs from the name
- `Added` Explorer Action v17 adds a matcher-free read-only fallback tied to one normal primary archive action and returns bounded detected-format metadata from the existing archive session
- `Added` The management page adds archive information that brings the actual format, content totals, available edit operations, and exact read-only reason into one place
- `Fixed` TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST now use exact complete-name suffixes instead of generic `gz`, `xz`, `bz2`, or `zst` leaf extensions, so ordinary compressed streams do not receive the primary archive action
- `Improved` The host performs no background file scan while building menus; only an explicit user click runs one existing read-only archive-open call, with no new path, directory, or write authority
- `Improved` Add, import, create-folder, rename, and delete now review factual work, the full rebuild, and metadata effects before reserving host output; cancelling the review creates no pending output

## v2.9.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v16 (version code 5276 or newer)
- `Added` The compression form adds an off-by-default Move source items to Trash after compression option that runs only after every physical output is verified and committed
- `Added` Explorer Action v16 accepts only the exact ordered original selection and every committed output transaction, then lets the host revalidate source and output identities before using its Trash
- `Fixed` The host now syncs a recovery copy and persists its Trash record before removing a source; if a directory is removed only partly, its recoverable copy is retained instead of deleting the only recovery data
- `Improved` The Trash phase cannot be cancelled and reports committed, recovery-required, failed, and unknown outcomes separately; a lost Binder response queries the host terminal state instead of blindly retrying

## v2.8.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v15 (version code 5276 or newer)
- `Added` Standard split ZIP and Compress each item separately now finish writing and read-back verification for every output before one recoverable Explorer Action v15 batch publication
- `Fixed` Multi-output creation no longer leaves committed partial results on normal failure paths; Explorer refreshes and success is reported only after the complete batch commits
- `Fixed` Compression option switches now render correctly and remain tappable on Android 7 instead of appearing as plain labels
- `Improved` The host durably records the parent and each staged file identity before publication; failure or restart rolls back only matching members, while externally changed files are preserved and reported for manual recovery

## v2.7.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v14 (version code 5276 or newer)
- `Added` Complete numbered `.zip.001` and `.7z.001` sets can now be browsed, previewed, and extracted by opening their `.001` volume; numbered sets remain read-only
- `Added` Explorer Action v14 adds bounded compound filename-suffix matching and reuses the UID-bound v12 sibling-volume source without matching arbitrary `.001` files
- `Fixed` Android 7 combines host-authorized numbered ZIP volumes into one private local file before the Zip4j compatibility path, so valid sets are no longer reported as damaged
- `Improved` Companion numbers are bounded to `.002` through `.128` and every supplied volume must be contiguous; the reader reports the exact next missing volume, revalidates identity around materialization, and never advertises in-archive modification

## v2.6.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v13 (version code 5276 or newer)
- `Added` The native archive page can now select a ZIP filename encoding from the path bar without opening the management page; changing it preserves the current internal folder and available selected entries
- `Added` Explorer Action v13 rebuilds the staged source inside the same read-only session and uses stable entry IDs to restore the deepest available path and entries that still exist
- `Improved` A replacement index is published only after it is complete; invalid choices, scan failures, active previews, or active extraction keep the previous index, while any existing password remains only in clearable memory

## v2.5.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v12 (version code 5276 or newer)
- `Added` Complete standard `.z01 + .zip` sets and modern WinRAR `partN.rar` sets can now be browsed, previewed, and extracted from the native archive page; split archives remain read-only
- `Added` Explorer Action v12 gives the plugin only a bounded catalog of host-approved sibling volumes and opens each one by opaque ID as a read-only descriptor, without exposing a directory or filesystem path
- `Fixed` Split ZIP final-volume directory metadata is accepted correctly on Android 7 and newer, and entry data is read across every authorized volume without treating the final volume as damaged
- `Fixed` Split RAR segment CRC values are no longer compared with reconstructed entry data; missing volumes and volumes changed after opening now produce stable typed failures
- `Improved` Volume count, names, IDs, open requests, caller UID, file identity, and session lifetime are bounded and revalidated; interrupted staging removes every partial private-cache file

## v2.4.0

_2026/08/26_

- `Note` This release requires AutoJs6 6.8.0 with Explorer Action v11, version code 5276 or newer
- `Added` RAR4/RAR5 archives can now be browsed, previewed, and extracted, including content- and header-encrypted input; RAR remains deliberately read-only
- `Added` The native AutoJs6 archive page can request a password while first opening an archive or during extraction, then retry without losing the current path or selection
- `Fixed` Split RAR first volumes keep their readable metadata but no longer advertise extraction when sibling volumes are unavailable
- `Fixed` A wrong password clears the previous input and retries against an unchanged archive snapshot instead of leaving the native page
- `Improved` RAR uses direct reads from the host's seekable descriptor when available, adds no native ABI, and applies the same path, resource, and output safety checks as other formats
- `Dependency` Added Junrar 8.1.0 and SLF4J 2.0.17 for read-only RAR support under their bundled license terms

## v2.3.0

_2026/08/26_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v10 (version code 5276 or newer)
- `Added` The native archive page can now extract the current internal folder from the path bar or selected entries from the selection bar without opening a separate management page
- `Added` Native extraction writes through a host-owned output tree with progress, cancellation, safe conflict numbering, and automatic Explorer refresh
- `Fixed` Leaving an archive on Android 7 no longer crashes while the host cleans its preview cache
- `Fixed` Labels in the five-action file selection bar are centered below their icons on narrow screens
- `Improved` Archive selection mode now shows only Exit and Extract, hiding filesystem actions that do not apply inside an archive

## v2.2.0

_2026/08/26_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v9 (version code 5276 or newer)
- `Added` Extract to... now recommends the current folder and creates a same-name output folder through a host-owned directory transaction; an equivalent existing name is safely numbered and its content is never changed
- `Added` Ordinary single-volume ZIP management can import an entire folder through the Android system picker, including nested files and empty folders
- `Fixed` The extraction destination chooser remains fully usable on short and narrow screens and shows the exact default path
- `Fixed` Cancelled, failed, interrupted, or out-of-space same-directory extractions roll back unpublished output; the next session recovers interrupted host transactions without changing the source archive
- `Improved` Explorer Action v9 atomically publishes verified directory trees and refreshes the new output folder in AutoJs6 immediately after commit

## v2.1.0

_2026/08/25_

- `Note` Editing currently targets ordinary single-volume `.zip` files. JAR/AAR/WAR, split ZIP, 7Z, and TAR-family archives remain read-only; rebuilding normalizes archive comments, nonessential extra metadata, and Unix permission attributes
- `Added` Manage archive... opens a selected ZIP in the management page with Add files..., New folder..., Rename..., and Delete actions, including directory-subtree rename and deletion
- `Added` Explorer Action v8 rebuilds into host-owned pending output, fully reads the result back, atomically replaces the original only after verification, and refreshes the Explorer row automatically
- `Improved` Every change is prevalidated as an immutable plan for unsafe paths, duplicate or equivalent names, file/directory collisions, unsupported retained entries, and source changes before replacement output is committed
- `Improved` ZIP rebuilds retain stored or deflated entry content, usable timestamps, and supported ZipCrypto/AES encryption; cancellation or any validation failure aborts pending output and leaves the original archive unchanged

## v2.0.0

_2026/08/25_

- `Added` Renamed the product to Archive Manager and the file action to Open archive
- `Added` Explorer Action v5 browses archives in the native AutoJs6 file list with the existing path bar, theme, and Back navigation
- `Added` Explorer Action v6 opens supported archive entries with the host document, image, audio, and video viewers
- `Added` Extract to... shortcut for choosing a destination and extracting the entire archive
- `Added` Manage archive... opens the management page with whole-archive, current-internal-folder, and current-checkbox-selection extraction ranges while preserving the direct whole-archive shortcut
- `Added` Ask, skip, overwrite, and auto-rename extraction conflict policies with Apply to all, accurate completion counts, and preservation of existing output folders
- `Added` Explorer Action v4 adds Compress... to ordinary file and folder menus and to the five-action same-parent multi-selection bar
- `Added` ZIP creation with default naming, compression levels, progress, cancellation, and automatic conflict numbering
- `Added` Standard split ZIP creation, including AES-256, with common MiB presets or a custom whole-MiB size; one conflict-safe base name covers every volume, and the final `.zip` appears after the numbered parts
- `Added` Create one archive per item in a same-parent multi-selection with an output preview, automatic conflict numbering, and explicit preservation and reporting of completed outputs after a later failure or cancellation
- `Added` Non-solid 7Z creation with levels 0 through 9 and optional AES-256 content encryption; filenames remain visible and filename encryption is not misreported
- `Added` TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST creation with format-specific levels and complete compound-extension updates
- `Added` Browse and extract ZipCrypto/AES encrypted ZIP files, retry a wrong password in place, and optionally create AES-256 ZIP files whose names remain visible with matching password confirmation
- `Added` Browse, preview, and extract ordinary or solid 7Z archives with common compression/filter pipelines, AES content encryption, and header encryption; missing and wrong passwords receive explicit diagnostics
- `Added` Browse, preview, and extract uncompressed TAR files in the native host list with header-checksum validation; links, device nodes, and sparse entries remain list-only
- `Added` Browse, preview, and extract TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST through the same native host paths; format detection verifies both the compressor signature and inner TAR structure
- `Added` Compatible, Strict, and Custom extraction budgets; over-budget archives remain read-only browsable and show estimated output, exceeded dimensions, and one-time confirmation before writing
- `Added` Extraction progress now shows entries, bytes, the current item, transfer rate, and estimated time remaining, with reliable cancellation
- `Fixed` ZIP listings now use directory metadata and handle self-extracting-style preambles, legacy filename encodings, Windows separators, and more readable ZIP methods
- `Fixed` Standard `.z01 + .zip` split ZIP files now identify the required earlier volumes instead of reporting the final volume as damaged
- `Fixed` ZIP filename encoding can be overridden when automatic detection is wrong, and extraction reuses the selected encoding
- `Fixed` Unknown or imprecise sizes, valid DocumentsProvider URIs, and extra host write grants no longer reject a valid archive before parsing
- `Fixed` ZIP browsing and extraction now work on Android 7.x without calling runtime APIs that only exist on newer systems
- `Fixed` ZIP files with Unicode names now open correctly on Android 7, including archives whose UTF-8 filename flag is missing or handled inconsistently
- `Fixed` Wrong passwords now map consistently to PASSWORD/WRONG_PASSWORD, and AES v2 entries with a zero stored CRC are no longer misreported as damaged
- `Fixed` Default extraction folders for compound extensions such as TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST now remove the complete suffix instead of retaining `.tar`
- `Fixed` Archives containing parent traversal, absolute, drive-prefixed, or control-character names remain browsable; unsafe names move to a read-only isolation folder, remain previewable when their data is readable, and require explicit skipping before extraction
- `Fixed` Extraction no longer overwrites existing files or directories when a destination provider treats case or Unicode-equivalent names as identical; equivalent output-folder names are numbered automatically
- `Fixed` Cancellation and extraction failures roll back the newly created output root in a non-cancellable cleanup phase; if a provider refuses deletion, the possible residual name and URI are listed instead of only a generic cleanup error
- `Fixed` Encrypted 7Z creation now works on Android 7, and verification matches entries by path so valid backend ordering differences no longer cause false failures
- `Fixed` The management page now adapts to short portrait and landscape screens: archive and path context move into the toolbar, settings remain available in a compact horizontal row, and entries and actions stay visible at up to 2.0x font scale
- `Improved` Removed the fixed 4 GiB input cap and browse-time extraction-size/ratio gates while retaining path containment, integrity checks, and failure cleanup
- `Improved` Added a checkable Roadmap and rewrote README and CHANGELOG to separate current behavior from planned work
- `Improved` The transitional standalone screen now follows system day/night mode and Material dynamic colors
- `Improved` ZIP, 7Z, and TAR-family output streams through a UID-bound host session to same-directory temporary output and commits atomically without storage permission or overwriting existing files
- `Improved` Archive format and entry capabilities are checked consistently across preview, extraction, and creation, so unavailable options stay disabled
- `Improved` Archive failures identify the format, processing stage, stable code, and reason; debug builds can copy complete diagnostics
- `Improved` Ordinary archives now browse through independent positional channels over the host's read-only descriptor without a whole-file copy; incompatible inputs or readers (currently including encrypted ZIP) fall back to private cache, which is cleaned on close, failure, or expiry
- `Improved` Unified ZIP, 7Z, and TAR-family creation output transactions; unreadable sources and reserve, open, write, or commit failures now carry stable stages, while unconfirmed rollback closes the session, shows the intended path, and prevents an unsafe retry
- `Improved` Archive creation now scans sources before opening temporary output and shows separate scanning, compression, verification, and commit states with total files, bytes read, and unknown-size files
- `Improved` Created archives are fully read back before publication to verify their format, entries, sizes, CRC values, and content fingerprints; every pending split ZIP volume is also compared byte for byte
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
