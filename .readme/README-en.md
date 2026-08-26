<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>An AutoJs6 file-manager plugin for browsing, extracting, and creating supported archives, with transactional editing for ordinary ZIP files</p>

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

Archive Manager brings archive browsing, extraction, creation, and management into the AutoJs6 file manager. The current build uses the native host list and path bar for ZIP, 7Z, and TAR-family browsing, previews supported entries, extracts selected ranges, creates supported formats from one item or a same-parent selection, and transactionally edits ordinary single-volume ZIP files from the management page.

### Available now

- Open ZIP-, 7Z-, and TAR-family archives directly in the native AutoJs6 file list, using the host theme, dark mode, and dynamic colors.
- Show the external directory, archive name, and internal directory in the path bar; jump by tapping a level and use Back to move up before leaving the archive.
- Extract the current internal folder from the path bar, or enter selection mode and extract selected files and folders, without leaving the native host page; progress is shown, the task can be cancelled, and the parent directory refreshes on completion.
- Preview supported document, image, audio, and video entries with the host's existing viewers.
- Use Extract to... to extract an entire archive into the recommended same-directory folder, or choose another folder with the Android system picker; equivalent existing folder names are safely numbered.
- Choose Manage archive... to open the management page, extract the entire archive, current internal folder, or current checkbox selection, or edit an ordinary single-volume ZIP with Add files..., Add folder..., New folder..., Rename..., and Delete; Extract to... remains the whole-archive shortcut.
- ZIP changes are planned before writing, rebuilt into host-owned pending output, fully read back, and atomically replace the original only after verification. Cancellation or failure leaves the source unchanged, and a successful commit refreshes Explorer automatically.
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

> The complete integration uses Explorer Action v10 for native current-folder and selected-entry extraction, v9 for verified directory output and recovery, v8 for verified target replacement, v7 for pre-commit output verification, v6 for native browsing and entry preview, and v4 file sessions for compression; it requires AutoJs6 version code 5276 or newer. Editing currently applies only to ordinary single-volume `.zip` files. Reading existing split volumes, native password and filename-encoding prompts, filename encryption during creation, source deletion, and editing JAR/AAR/WAR, 7Z, or TAR-family archives are not released capabilities yet. Use the Roadmap checkboxes as the source of truth.

### Usage

1. Install the plugin and enable it in the AutoJs6 Plugin Center.
2. Open the file menu for a ZIP, JAR, AAR, WAR, 7Z, or TAR-family archive.
3. Choose Open archive, then enter directories, search, or jump with the path bar in the host file list.
4. To extract the entire archive, choose Extract to... from its file menu. Use the recommended current folder or choose another folder with the Android system picker, then confirm the exact output path.
5. To extract the current internal folder, use the extraction button at the right of the path bar. To extract specific entries, touch and hold an entry, select files or folders, and tap Extract in the bottom bar. Use Manage archive... or Extract to... when a password, encoding correction, unsafe-path confirmation, conflict policy, or another destination is required.
6. To edit an ordinary single-volume ZIP, choose Manage archive... and use Add files..., Add folder... to import a complete folder tree, New folder... to create an empty folder, Rename..., or Delete. Wait for rebuilding, verification, and the success message before leaving the page.
7. On the management page, choose how equivalent output names are handled before extraction. Ask each time can apply one skip, overwrite, or auto-rename decision to all compatible remaining conflicts.
8. To create an archive, choose Compress... from an ordinary file or folder menu, or select multiple items in one directory and use Compress... in the bottom action bar. To create one archive per item, enable Compress each item separately, review the output preview, and create; this mode always resolves name conflicts with safe automatic numbering. For ZIP, choose No split, a common MiB preset, or a custom whole number from 1 to 4096 MiB; when output exceeds that size, it consists of `.z01`, `.z02`, ... numbered volumes and a final `.zip`, while smaller output remains one `.zip`.

### Permissions and data

The plugin requests neither storage nor network permission. Native browsing first leases the host's seekable read-only descriptor and gives ordinary archives independent positional channels. Pipes, writable or non-seekable sources, Android 7, and readers that require a process-readable local file (currently encrypted ZIP) fall back to private cache. The descriptor lease or cache is cleaned when the page closes, unbinds, fails, or expires. Same-directory extraction writes only through a host-owned directory transaction pinned to the plugin UID; another folder uses only the tree grant chosen in the Android system picker. Archive creation uses a host file session pinned to the plugin UID, reads targets page by page, and can create transactional output only in the current parent directory. Passwords stay only in clearable memory buffers, are never written to Bundles, preferences, logs, or diagnostics, and are cleared when replaced, after a task, or when the page is destroyed. The fixed 4 GiB input cap and browse-time extraction-size/ratio gates have been removed; path containment, destination isolation, source-size checks, output transactions, and failure cleanup remain.

Resource budgets decide when to warn or request confirmation; they never relax structural safety. After confirmation, live byte and ratio bounds expand only to the selected entries' declared values for that extraction. Undeclared growth, source changes, and size or CRC mismatches still stop the operation and clean its output.

Split ZIP creation assembles and verifies the complete volume set in private cache, copies each volume to a hidden host output, and compares every pending volume byte for byte. Only then is the private staging copy removed and the numbered volumes published before the final `.zip`; existing names are never overwritten. Because the current host has no atomic group commit, any partial commit result is reported explicitly instead of being presented as a complete archive.

Unsafe names are exposed only as read-only display text behind opaque IDs; they never become output paths.

### Roadmap

The implementation tasks and acceptance criteria for writable formats beyond ordinary ZIP, split-volume reading, native password and filename-encoding prompts, filename encryption during creation, source deletion, undo, and remaining failure recovery live in the Roadmap. Unchecked work is not a current feature.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.3.0

_2026/08/26_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v10 (version code 5276 or newer)
- `Added` The native archive page can now extract the current internal folder from the path bar or selected entries from the selection bar without opening a separate management page
- `Added` Native extraction writes through a host-owned output tree with progress, cancellation, safe conflict numbering, and automatic Explorer refresh
- `Fixed` Leaving an archive on Android 7 no longer crashes while the host cleans its preview cache
- `Fixed` Labels in the five-action file selection bar are centered below their icons on narrow screens
- `Improved` Archive selection mode now shows only Exit and Extract, hiding filesystem actions that do not apply inside an archive

#### v2.2.0

_2026/08/26_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v9 (version code 5276 or newer)
- `Added` Extract to... now recommends the current folder and creates a same-name output folder through a host-owned directory transaction; an equivalent existing name is safely numbered and its content is never changed
- `Added` Ordinary single-volume ZIP management can import an entire folder through the Android system picker, including nested files and empty folders
- `Fixed` The extraction destination chooser remains fully usable on short and narrow screens and shows the exact default path
- `Fixed` Cancelled, failed, interrupted, or out-of-space same-directory extractions roll back unpublished output; the next session recovers interrupted host transactions without changing the source archive
- `Improved` Explorer Action v9 atomically publishes verified directory trees and refreshes the new output folder in AutoJs6 immediately after commit

#### v2.1.0

_2026/08/25_

- `Note` Editing currently targets ordinary single-volume `.zip` files. JAR/AAR/WAR, split ZIP, 7Z, and TAR-family archives remain read-only; rebuilding normalizes archive comments, nonessential extra metadata, and Unix permission attributes
- `Added` Manage archive... opens a selected ZIP in the management page with Add files..., New folder..., Rename..., and Delete actions, including directory-subtree rename and deletion
- `Added` Explorer Action v8 rebuilds into host-owned pending output, fully reads the result back, atomically replaces the original only after verification, and refreshes the Explorer row automatically
- `Improved` Every change is prevalidated as an immutable plan for unsafe paths, duplicate or equivalent names, file/directory collisions, unsupported retained entries, and source changes before replacement output is committed
- `Improved` ZIP rebuilds retain stored or deflated entry content, usable timestamps, and supported ZipCrypto/AES encryption; cancellation or any validation failure aborts pending output and leaves the original archive unchanged

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
