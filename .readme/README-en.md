<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>An AutoJs6 file-manager plugin for browsing and extracting ZIP/TAR archives and creating ZIP archives</p>

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

Archive Manager brings ZIP/TAR browsing and extraction plus ZIP creation into the AutoJs6 file manager. The current build browses archives in the native host list with external and internal paths, previews supported entries, and can compress one item or a same-parent selection. More formats, per-entry extraction, and in-archive editing remain staged in the Roadmap.

### Available now

- Open ZIP- and TAR-family archives directly in the native AutoJs6 file list, using the host theme, dark mode, and dynamic colors.
- Show the external directory, archive name, and internal directory in the path bar; jump by tapping a level and use Back to move up before leaving the archive.
- Preview supported document, image, audio, and video entries with the host's existing viewers.
- Use the Extract to... shortcut to extract an entire archive without first opening the archive view.
- Browse directories, search, and sort archive content.
- Build the listing from directory metadata without decompressing every entry first.
- Browse, preview, and extract uncompressed TAR files; symbolic links, hard links, device nodes, and sparse entries are listed but never written as ordinary files.
- Browse, preview, and extract TAR.GZ/TGZ and TAR.XZ/TXZ archives with the same internal paths, special-entry isolation, and integrity checks.
- Verify the actual ZIP/TAR structure and apply one capability model to preview, extraction, and creation, keeping unavailable options disabled.
- Handle Zip64, self-extracting-style preambles, legacy filename encodings, and Windows path separators.
- Browse and extract ZIP files protected with ZipCrypto or AES, retry a wrong password in place, and optionally create AES-256 encrypted ZIP files whose names remain visible; encrypted creation requires matching password confirmation.
- Override the ZIP filename encoding when automatic detection is wrong; browsing and extraction reuse the same selection.
- Show archive failures with the format, processing stage, stable code, and a clear reason; debug builds can copy detailed diagnostics.
- Offer Compress... for ordinary files, folders, and same-parent multi-selections.
- Create ZIP files with a configurable name and compression level; default to the target name for one item and the parent-folder name for multiple items.
- Write to a same-directory temporary file and commit atomically; automatically number conflicts without overwriting existing files.

### Current formats

The current release recognizes these browsable and extractable extensions:

```text
zip, jar, aar, war, tar, tar.gz, tgz, tar.xz, txz
```

The current release can create these formats:

```text
zip
```

> Explorer Action v6 native browsing and entry preview, plus v4 compression, require AutoJs6 version code 5276 or newer. Per-entry extraction inside the native host page, 7z, split volumes, filename encryption, separate archives, source deletion, and in-archive add/delete operations are not released capabilities yet. Use the Roadmap checkboxes as the source of truth.

### Usage

1. Install the plugin and enable it in the AutoJs6 Plugin Center.
2. Open the file menu for a ZIP, JAR, AAR, WAR, or TAR file.
3. Choose Open archive, then enter directories, search, or jump with the path bar in the host file list.
4. To extract the entire archive, choose Extract to... from its file menu and select an output directory with the Android system picker.
5. To create a ZIP, choose Compress... from an ordinary file or folder menu, or select multiple items in one directory and use Compress... in the bottom action bar.

### Permissions and data

The plugin requests neither storage nor network permission. Native browsing uses a short-lived read-only archive session pinned to the host UID and removes staged input when the page closes or unbinds; extraction uses only the input URI temporarily granted by the host. ZIP creation uses a host file session pinned to the plugin UID, reads targets page by page, and can create transactional output only in the current parent directory. Passwords stay only in clearable memory buffers, are never written to Bundles, preferences, logs, or diagnostics, and are cleared when replaced, after a task, or when the page is destroyed. The fixed 4 GiB input cap and browse-time extraction-size/ratio gates have been removed; path containment, destination isolation, integrity checks, and failure cleanup remain.

### Roadmap

The implementation tasks and acceptance criteria for more formats, split volumes, per-entry extraction, archive editing, and the full device matrix live in the Roadmap. Unchecked work is not a current feature.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### Unreleased

_Unreleased_

- `Added` Renamed the product to Archive Manager and the file action to Open archive
- `Added` Explorer Action v5 browses archives in the native AutoJs6 file list with the existing path bar, theme, and Back navigation
- `Added` Explorer Action v6 opens supported archive entries with the host document, image, audio, and video viewers
- `Added` Extract to... shortcut for choosing a destination and extracting the entire archive
- `Added` Explorer Action v4 adds Compress... to ordinary file and folder menus and to the five-action same-parent multi-selection bar
- `Added` ZIP creation with default naming, compression levels, progress, cancellation, and automatic conflict numbering
- `Added` Browse and extract ZipCrypto/AES encrypted ZIP files, retry a wrong password in place, and optionally create AES-256 ZIP files whose names remain visible with matching password confirmation
- `Added` Browse, preview, and extract uncompressed TAR files in the native host list with header-checksum validation; links, device nodes, and sparse entries remain list-only
- `Added` Browse, preview, and extract TAR.GZ/TGZ and TAR.XZ/TXZ through the same native host paths; format detection verifies both the compressor signature and inner TAR structure
- `Fixed` ZIP listings now use directory metadata and handle self-extracting-style preambles, legacy filename encodings, Windows separators, and more readable ZIP methods
- `Fixed` ZIP filename encoding can be overridden when automatic detection is wrong, and extraction reuses the selected encoding
- `Fixed` Unknown or imprecise sizes, valid DocumentsProvider URIs, and extra host write grants no longer reject a valid archive before parsing
- `Fixed` ZIP browsing and extraction now work on Android 7.x without calling runtime APIs that only exist on newer systems
- `Fixed` Wrong passwords now map consistently to PASSWORD/WRONG_PASSWORD, and AES v2 entries with a zero stored CRC are no longer misreported as damaged
- `Improved` Removed the fixed 4 GiB input cap and browse-time extraction-size/ratio gates while retaining path containment, integrity checks, and failure cleanup
- `Improved` Added a checkable Roadmap and rewrote README and CHANGELOG to separate current behavior from planned work
- `Improved` The transitional standalone screen now follows system day/night mode and Material dynamic colors
- `Improved` ZIP output streams through a UID-bound host session to same-directory temporary output and commits atomically without storage permission or overwriting existing files
- `Improved` Archive format and entry capabilities are checked consistently across preview, extraction, and creation, so unavailable options stay disabled
- `Improved` Archive failures identify the format, processing stage, stable code, and reason; debug builds can copy complete diagnostics
- `Dependency` Added Apache License 2.0 licensed Zip4j 2.11.5 for encrypted ZIP streams, AES-256 creation, and the Android 7.x compatibility path
- `Dependency` Added 0BSD-licensed XZ for Java 1.12 for pure-Java TAR.XZ/TXZ decoding without native ABIs

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
