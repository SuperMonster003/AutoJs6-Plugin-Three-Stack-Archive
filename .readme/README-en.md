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
- Correct ZIP filename encoding directly from the host path bar; the same read-only session rebuilds its index in place and preserves the current internal path and available selection where possible.
- Browse, preview, and extract complete standard `.z01 + .zip`, modern WinRAR `partN.rar`, numbered `.zip.001`, and numbered `.7z.001` sets through bounded host-authorized sibling-volume descriptors; missing or changed volumes fail explicitly.
- Create ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST from one item or a same-parent selection; ZIP also supports AES-256 and standard split output, and a selection can create one archive per item.
- Standard split ZIP and Compress each item separately publish all verified physical outputs through one recoverable batch; failure or host restart is never presented as a successful partial result.
- Edit ordinary single-volume ZIP files through a verified rebuild: add files or complete folder trees, create empty folders, rename, and delete, then atomically replace the source only after read-back verification.
- Keep unsafe archive names read-only and isolated, apply structural and resource limits before writing, and prefer direct reads from the host's seekable descriptor over whole-file copies.
- Optionally move the complete source selection to the host Trash only after every physical output is verified and committed; this option is off by default, and changed sources or incomplete output proof stop before source data is removed.

### Current formats

The current release recognizes these browsable and extractable extensions:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

The current release can create these formats:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> Native integration requires the paired AutoJs6 6.8.0 build with Explorer Action v16 (version code 5276 or newer). RAR and split archives are deliberately read-only; editing is limited to ordinary single-volume `.zip` files. Open a standard split ZIP through its final `.zip`, a modern WinRAR set through its first `partN.rar` volume, and a numbered ZIP or 7Z set through its `.001` volume, with every required sibling in the same directory. Filename-encrypted creation and in-archive editing for 7Z, RAR, and TAR-family formats are not current capabilities.

### Usage

1. Install Archive Manager and enable it in the AutoJs6 Plugin Center.
2. Tap the primary archive action or choose Open archive for a supported file. Browse it like a normal directory with the host path bar.
3. Use the path-bar extraction action for the current internal folder or the filename-encoding action to correct ZIP names, long-press entries to extract a selection, or choose Extract to... from the archive's file menu for the whole archive. Password prompts appear when required.
4. Choose Compress... for a file or folder, or select several items in one directory and use Compress... in the bottom action bar. To clean up sources after success, explicitly enable the off-by-default Move source items to Trash after compression option.
5. Choose Manage archive... only for an ordinary single-volume ZIP when you need to add, rename, or delete content.

### Permissions and data

Archive Manager requests neither storage nor network permission. The host supplies short-lived read-only descriptors and UID-pinned output transactions, so the plugin cannot choose arbitrary filesystem paths. Explorer Action v11 carries a password only in a bounded synchronous retry request; both sides immediately remove and clear retained buffers and never persist it. Explorer Action v12 adds only a bounded, session-scoped catalog of host-approved sibling volumes: the plugin receives opaque IDs instead of paths, and caller UID, file identity, size, modification time, and lifecycle are revalidated before use. Explorer Action v13 only reindexes the same staged source and retains the old state until a complete replacement index is ready. Explorer Action v14 matches only bounded compound suffixes such as `.zip.001` and `.7z.001`, never arbitrary `.001` files, and reuses the v12 catalog without granting directory or path access. Android and Java libraries can still create unavoidable short-lived runtime copies, so password cleanup is best-effort memory hygiene rather than an absolute claim. Path traversal and unsafe names remain isolated, output is verified before publication, and resource-budget confirmation never disables structural safety checks.

Explorer Action v15 groups only verified new-file outputs from one session into a recoverable batch of at most 128 members. Explorer Action v16 lets the host revalidate sources and outputs and move sources to Trash only after the plugin supplies the exact ordered original selection and every committed output transaction. The host syncs a recovery copy and persists its record before removing source data; the plugin receives no arbitrary-path or direct-delete capability. A lost Binder response is resolved by querying the same idempotent terminal result, not by retrying the move.

### Roadmap

The remaining work is tracked as checkable items: writable rebuilds beyond ordinary ZIP, grouped Trash undo and history, accessibility review, the rest of the device and producer matrix, and first-public-release material.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.9.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v16 (version code 5276 or newer)
- `Added` The compression form adds an off-by-default Move source items to Trash after compression option that runs only after every physical output is verified and committed
- `Added` Explorer Action v16 accepts only the exact ordered original selection and every committed output transaction, then lets the host revalidate source and output identities before using its Trash
- `Fixed` The host now syncs a recovery copy and persists its Trash record before removing a source; if a directory is removed only partly, its recoverable copy is retained instead of deleting the only recovery data
- `Improved` The Trash phase cannot be cancelled and reports committed, recovery-required, failed, and unknown outcomes separately; a lost Binder response queries the host terminal state instead of blindly retrying

#### v2.8.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v15 (version code 5276 or newer)
- `Added` Standard split ZIP and Compress each item separately now finish writing and read-back verification for every output before one recoverable Explorer Action v15 batch publication
- `Fixed` Multi-output creation no longer leaves committed partial results on normal failure paths; Explorer refreshes and success is reported only after the complete batch commits
- `Fixed` Compression option switches now render correctly and remain tappable on Android 7 instead of appearing as plain labels
- `Improved` The host durably records the parent and each staged file identity before publication; failure or restart rolls back only matching members, while externally changed files are preserved and reported for manual recovery

#### v2.7.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v14 (version code 5276 or newer)
- `Added` Complete numbered `.zip.001` and `.7z.001` sets can now be browsed, previewed, and extracted by opening their `.001` volume; numbered sets remain read-only
- `Added` Explorer Action v14 adds bounded compound filename-suffix matching and reuses the UID-bound v12 sibling-volume source without matching arbitrary `.001` files
- `Fixed` Android 7 combines host-authorized numbered ZIP volumes into one private local file before the Zip4j compatibility path, so valid sets are no longer reported as damaged
- `Improved` Companion numbers are bounded to `.002` through `.128` and every supplied volume must be contiguous; the reader reports the exact next missing volume, revalidates identity around materialization, and never advertises in-archive modification

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
