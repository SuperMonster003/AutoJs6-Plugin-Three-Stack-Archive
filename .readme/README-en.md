<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>A native AutoJs6 archive manager for browsing, extracting, creating, and safely editing supported archives</p>

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

Archive Manager works inside the AutoJs6 file manager instead of replacing it. Supported archives use the host list, path bar, theme, viewers, selection mode, progress UI, and directory refresh. A separate management page remains only for settings and operations that need a richer form.

### Available now

- Browse ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5, and TAR-family archives in the native AutoJs6 file list, including internal path-bar navigation, search, sorting, and Back behavior.
- Preview readable documents, images, audio, and video with the host's existing viewers, without extracting the whole archive first.
- Extract the whole archive, the current internal folder, or selected entries with progress, cancellation, safe conflict naming, output verification, and rollback before publication.
- Handle encrypted ZIP, 7Z, and RAR input with host-native password prompts for initial opening or extraction; a wrong password can be corrected without losing the current archive path.
- Create ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST from one item or a same-parent selection; ZIP also supports AES-256 and standard split output, and a selection can create one archive per item.
- Edit ordinary single-volume ZIP files through a verified rebuild: add files or complete folder trees, create empty folders, rename, and delete, then atomically replace the source only after read-back verification.
- Keep unsafe archive names read-only and isolated, apply structural and resource limits before writing, and prefer direct reads from the host's seekable descriptor over whole-file copies.

### Current formats

The current release recognizes these browsable and extractable extensions:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

The current release can create these formats:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> Native integration requires AutoJs6 6.8.0, version code 5276 or newer, with Explorer Action v11. RAR is deliberately read-only. Editing is limited to ordinary single-volume `.zip` files. Existing split ZIP and RAR sets cannot yet be read as a complete set because the host currently grants only the selected file descriptor; a RAR first volume may show metadata, but extraction remains unavailable. Filename-encrypted creation, source deletion after compression, and in-archive editing for 7Z, RAR, and TAR-family formats are not current capabilities.

### Usage

1. Install Archive Manager and enable it in the AutoJs6 Plugin Center.
2. Tap the primary archive action or choose Open archive for a supported file. Browse it like a normal directory with the host path bar.
3. Use the path-bar extraction action for the current internal folder, long-press entries to extract a selection, or choose Extract to... from the archive's file menu for the whole archive. Password prompts appear when required.
4. Choose Compress... for a file or folder, or select several items in one directory and use Compress... in the bottom action bar.
5. Choose Manage archive... only for an ordinary single-volume ZIP when you need to add, rename, or delete content.

### Permissions and data

Archive Manager requests neither storage nor network permission. The host supplies a short-lived read-only descriptor and UID-pinned output transactions, so the plugin cannot choose arbitrary filesystem paths. Explorer Action v11 carries a password only in a bounded synchronous retry request; the host and plugin remove and clear their retained buffers immediately, and neither side persists it in saved state, preferences, logs, or diagnostics. Android and Java libraries can still create unavoidable short-lived runtime copies, so this is best-effort memory hygiene rather than an absolute claim. Path traversal and unsafe names remain isolated, output is verified before publication, and resource-budget confirmation never disables structural safety checks.

### Roadmap

The remaining work is tracked as checkable items: sibling-volume input for split ZIP/RAR sets, native filename-encoding correction, writable rebuilds beyond ordinary ZIP, undo or source-deletion transactions, accessibility review, and the rest of the device and producer matrix.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.4.0

_2026/08/26_

- `Note` This release requires AutoJs6 6.8.0 with Explorer Action v11, version code 5276 or newer
- `Added` RAR4/RAR5 archives can now be browsed, previewed, and extracted, including content- and header-encrypted input; RAR remains deliberately read-only
- `Added` The native AutoJs6 archive page can request a password while first opening an archive or during extraction, then retry without losing the current path or selection
- `Fixed` Split RAR first volumes keep their readable metadata but no longer advertise extraction when sibling volumes are unavailable
- `Fixed` A wrong password clears the previous input and retries against an unchanged archive snapshot instead of leaving the native page
- `Improved` RAR uses direct reads from the host's seekable descriptor when available, adds no native ABI, and applies the same path, resource, and output safety checks as other formats
- `Dependency` Added Junrar 8.1.0 and SLF4J 2.0.17 for read-only RAR support under their bundled license terms

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
