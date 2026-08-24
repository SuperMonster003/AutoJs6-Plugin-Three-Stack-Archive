<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>An AutoJs6 file-manager plugin for browsing, extracting, and creating ZIP, 7Z, and TAR-family archives</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### Languages

The README is available in these languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### About

Archive Manager brings ZIP, 7Z, and TAR-family browsing, extraction, and creation into the AutoJs6 file manager. The current build browses archives in the native host list with external and internal paths, previews supported entries, extracts the entire archive, current internal folder, or current checkbox selection from the management page, and creates a supported format from one item or a same-parent selection. Per-entry extraction inside the native host page and in-archive editing remain staged in the Roadmap.

### Available now

- Open ZIP-, 7Z-, and TAR-family archives directly in the native AutoJs6 file list, using the host theme, dark mode, and dynamic colors.
- Show the external directory, archive name, and internal directory in the path bar; jump by tapping a level and use Back to move up before leaving the archive.
- Preview supported document, image, audio, and video entries with the host's existing viewers.
- Use the Extract to... shortcut to extract an entire archive without first opening the archive view.
- Choose Selective extraction... to open the management page and extract the entire archive, current internal folder, or current checkbox selection; Extract to... remains the whole-archive shortcut.
- Choose Ask each time, Skip, Overwrite, or Auto rename for equivalent output names; Apply to all handles compatible remaining conflicts, while existing output folders are always numbered and preserved.
- Browse directories, search, and sort archive content.
- Build the listing from directory metadata without decompressing every entry first.
- Browse ordinary archives directly through the host's seekable read-only descriptor with independent positional channels and no whole-file copy; pipes, writable or non-seekable sources, Android 7, and readers that require a process-readable local file (currently encrypted ZIP) fall back to private cache, which is removed on close.
- Choose Compatible, Strict, or Custom extraction budgets; an archive that exceeds entry, path, output-size, or compression-ratio thresholds stays browsable and shows its estimated space and risks for one-time confirmation before writing.
- Show extraction entry and byte progress, the current item, transfer rate, and estimated time remaining; cancellation or failure rolls back the newly created output root, and a provider-refused residual is listed by name and URI.
- Put parent-traversal, absolute, drive-prefixed, or control-character names in a path-bar-visible Unsafe paths folder; readable data remains previewable, while whole-archive extraction requires explicitly skipping those entries and leaves normal entries unaffected.
- Browse, preview, and extract uncompressed TAR files; symbolic links, hard links, device nodes, and sparse entries are listed but never written as ordinary files.
- Browse, preview, and extract TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST archives with the same internal paths, special-entry isolation, and integrity checks.
- Browse, preview, and extract ordinary or solid 7Z archives, including common compression/filter pipelines plus content- and header-encrypted inputs; missing and wrong passwords receive explicit diagnostics.
- Verify the actual ZIP/7Z/TAR structure and apply one capability model to preview, extraction, and creation, keeping unavailable options disabled.
- Handle Zip64, self-extracting-style preambles, legacy filename encodings, and Windows path separators.
- Recognize the standard `.z01 + .zip` split ZIP structure and name the required earlier volumes instead of reporting the final volume as damaged; create standard split ZIPs with preset or custom MiB sizes, while reading existing split ZIPs still awaits a host API for sibling volumes.
- Browse and extract ZIP files protected with ZipCrypto or AES, retry a wrong password in place, and optionally create AES-256 encrypted ZIP files whose names remain visible; encrypted creation requires matching password confirmation.
- Override the ZIP filename encoding when automatic detection is wrong; browsing and extraction reuse the same selection.
- Show archive failures with the format, processing stage, stable code, and a clear reason; debug builds can copy detailed diagnostics.
- Offer Compress... for ordinary files, folders, and same-parent multi-selections.
- Create one archive per item in a same-parent multi-selection; the form previews the output count and derived names, while existing or repeated names are numbered without overwriting. Each output commits independently; cancellation or failure keeps and reports completed outputs while blocking an ambiguous whole-batch retry.
- Create ordinary or standard split ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST with a configurable name and only the compression levels and password options supported by the selected format.
- Write to a same-directory temporary file and commit atomically; choose automatic numbering or try the exact name and decide before a numbered retry, without overwriting existing files. After name reservation, the sources are scanned into a bounded snapshot before temporary output is opened; the form distinguishes scanning, compression, verification, and commit while showing total files, bytes read, and unknown-size files. Verification fully reads the still-hidden output and checks its format, entries, sizes, CRC values, and content fingerprints before publication. Failed creation or verification aborts the transaction; if the host cannot confirm temporary-output cleanup, the form shows the intended path and prevents another attempt.

### Current formats

The current release recognizes these browsable and extractable extensions:

```text
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

The current release can create these formats:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> The complete integration uses Explorer Action v7 for pre-commit output verification, v6 for native browsing and entry preview, and v4 file sessions for compression; it requires AutoJs6 version code 5276 or newer. Per-entry extraction inside the native host page, reading existing split volumes, filename encryption during creation, source deletion, and in-archive add/delete operations are not released capabilities yet. Use the Roadmap checkboxes as the source of truth.

### Usage

1. Install the plugin and enable it in the AutoJs6 Plugin Center.
2. Open the file menu for a ZIP, JAR, AAR, WAR, 7Z, or TAR-family archive.
3. Choose Open archive, then enter directories, search, or jump with the path bar in the host file list.
4. To extract the entire archive, choose Extract to... from its file menu and select an output directory with the Android system picker.
5. For a specific range, choose Selective extraction..., browse within the management page or tick entries, tap Extract to..., choose the range, and then select the output directory.
6. On the management page, choose how equivalent output names are handled before extraction. Ask each time can apply one skip, overwrite, or auto-rename decision to all compatible remaining conflicts.
7. To create an archive, choose Compress... from an ordinary file or folder menu, or select multiple items in one directory and use Compress... in the bottom action bar. To create one archive per item, enable Compress each item separately, review the output preview, and create; this mode always resolves name conflicts with safe automatic numbering. For ZIP, choose No split, a common MiB preset, or a custom whole number from 1 to 4096 MiB; when output exceeds that size, it consists of `.z01`, `.z02`, ... numbered volumes and a final `.zip`, while smaller output remains one `.zip`.

### Permissions and data

The plugin requests neither storage nor network permission. Native browsing first leases the host's seekable read-only descriptor and gives ordinary archives independent positional channels. Pipes, writable or non-seekable sources, Android 7, and readers that require a process-readable local file (currently encrypted ZIP) fall back to private cache. The descriptor lease or cache is cleaned when the page closes, unbinds, fails, or expires. Extraction uses only the input URI temporarily granted by the host. Archive creation uses a host file session pinned to the plugin UID, reads targets page by page, and can create transactional output only in the current parent directory. Passwords stay only in clearable memory buffers, are never written to Bundles, preferences, logs, or diagnostics, and are cleared when replaced, after a task, or when the page is destroyed. The fixed 4 GiB input cap and browse-time extraction-size/ratio gates have been removed; path containment, destination isolation, source-size checks, output transactions, and failure cleanup remain.

Resource budgets decide when to warn or request confirmation; they never relax structural safety. After confirmation, live byte and ratio bounds expand only to the selected entries' declared values for that extraction. Undeclared growth, source changes, and size or CRC mismatches still stop the operation and clean its output.

Split ZIP creation assembles and verifies the complete volume set in private cache, copies each volume to a hidden host output, and compares every pending volume byte for byte. Only then is the private staging copy removed and the numbered volumes published before the final `.zip`; existing names are never overwritten. Because the current host has no atomic group commit, any partial commit result is reported explicitly instead of being presented as a complete archive.

Unsafe names are exposed only as read-only display text behind opaque IDs; they never become output paths.

### Roadmap

The implementation tasks and acceptance criteria for more formats, split-volume reading, per-entry extraction inside the native host page, archive editing, and the full device matrix live in the Roadmap. Unchecked work is not a current feature.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.0.0

_2026/08/25_

- `Added` Renamed the product to Archive Manager and the file action to Open archive
- `Added` Explorer Action v5 browses archives in the native AutoJs6 file list with the existing path bar, theme, and Back navigation
- `Added` Explorer Action v6 opens supported archive entries with the host document, image, audio, and video viewers
- `Added` Extract to... shortcut for choosing a destination and extracting the entire archive
- `Added` Selective extraction... opens the management page with whole-archive, current-internal-folder, and current-checkbox-selection ranges while preserving the direct whole-archive shortcut
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

#### v1.0.1

_2026/08/08_

- `Fixed` Empty service binding when enabling the plugin in Plugin Center
- `Improved` Simplified the plugin name, description, and usage text

#### v1.0.0

_2026/08/02_

- `Added` Initial release for browsing ZIP, JAR, AAR, and WAR files and extracting selected files or folders
- `Added` Added search, selection, progress, cancellation, temporary-input cleanup, and localized UI

##### Full history

* [CHANGELOG-en.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

### Build

```powershell
.\gradlew.bat :app:assembleDebug
```

Release build:

```powershell
.\gradlew.bat :app:assembleRelease
```

Run the Gradle Wrapper from the repository root; use `version.properties` as the source of truth for SDK and JDK requirements.

### Links

- AutoJs6 documentation: https://docs.autojs6.com
- Third-party notices: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
