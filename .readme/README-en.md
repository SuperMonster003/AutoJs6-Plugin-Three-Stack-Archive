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
- Browse, preview, and extract complete standard `.z01 + .zip` sets through bounded host-authorized sibling descriptors; create standard split ZIPs with preset or custom MiB sizes, and report missing or changed volumes explicitly.
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
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

The current release can create these formats:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> Native integration requires the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer). RAR and split archives are deliberately read-only; editing is limited to ordinary single-volume `.zip` files. Open a standard split ZIP through its final `.zip`, a modern WinRAR set through its first `partN.rar` volume, and a numbered ZIP or 7Z set through its `.001` volume, with every required sibling in the same directory. Filename-encrypted creation and in-archive editing for 7Z, RAR, and TAR-family formats are not current capabilities.

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

Archive Manager requests neither storage nor network permission. The host supplies short-lived read-only descriptors and UID-pinned output transactions, so the plugin cannot choose arbitrary filesystem paths. Explorer Action v11 carries a password only in a bounded synchronous retry request; both sides immediately remove and clear retained buffers and never persist it. Explorer Action v12 adds only a bounded, session-scoped catalog of host-approved sibling volumes: the plugin receives opaque IDs instead of paths, and caller UID, file identity, size, modification time, and lifecycle are revalidated before use. Explorer Action v13 only reindexes the same staged source and retains the old state until a complete replacement index is ready. Explorer Action v14 matches only bounded compound suffixes such as `.zip.001` and `.7z.001`, never arbitrary `.001` files, and reuses the v12 catalog without granting directory or path access. Explorer Action v17 adds only a matcher-free read-only overflow fallback when the normal primary action does not match; it runs one existing archive session after a user click and grants no new path, directory, or write access. Android and Java libraries can still create unavoidable short-lived runtime copies, so password cleanup is best-effort memory hygiene rather than an absolute claim. Path traversal and unsafe names remain isolated, output is verified before publication, and resource-budget confirmation never disables structural safety checks.

Explorer Action v15 groups only verified new-file outputs from one session into a recoverable batch of at most 128 members. Explorer Action v16 lets the host revalidate sources and outputs and move sources to Trash only after the plugin supplies the exact ordered original selection and every committed output transaction. The host syncs a recovery copy and persists its record before removing source data; the plugin receives no arbitrary-path or direct-delete capability. A lost Binder response is resolved by querying the same idempotent terminal result, not by retrying the move.

Explorer Action v18 keeps the previous archive only in host-private durable storage and returns an opaque history ID, never a backup path. One restore is allowed only while the parent and target still match the exact committed replacement. External changes make the history stale; interrupted recovery evidence is retained and blocks another replacement of that target until the host can resolve it. Normal history is bounded by age, count, total bytes, and free-space reserve, while v8-v17 sessions create no replacement backup.

### Roadmap

The remaining work is tracked as checkable items: writable rebuilds beyond ordinary ZIP, grouped Trash undo and history, the rest of the device and producer matrix, and first-public-release material.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.11.0

_2026/08/28_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v18 (version code 5276 or newer)
- `Added` After a successful ZIP edit, Restore previous version is available from the success message and the management-page menu for one rollback during bounded host retention
- `Improved` The host keeps the previous archive in private durable storage, restores only while the target is still the exact committed replacement, and preserves interrupted recovery evidence instead of overwriting external changes

#### v2.10.0

_2026/08/28_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v17 (version code 5276 or newer)
- `Added` A file whose name or extension is not recognized can now use Open as archive...; after structural detection, the native path bar labels the actual format when it differs from the name
- `Added` Explorer Action v17 adds a matcher-free read-only fallback tied to one normal primary archive action and returns bounded detected-format metadata from the existing archive session
- `Added` The management page adds archive information that brings the actual format, content totals, available edit operations, and exact read-only reason into one place
- `Fixed` TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST now use exact complete-name suffixes instead of generic `gz`, `xz`, `bz2`, or `zst` leaf extensions, so ordinary compressed streams do not receive the primary archive action
- `Improved` The host performs no background file scan while building menus; only an explicit user click runs one existing read-only archive-open call, with no new path, directory, or write authority
- `Improved` Add, import, create-folder, rename, and delete now review factual work, the full rebuild, and metadata effects before reserving host output; cancelling the review creates no pending output

#### v2.9.0

_2026/08/27_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v16 (version code 5276 or newer)
- `Added` The compression form adds an off-by-default Move source items to Trash after compression option that runs only after every physical output is verified and committed
- `Added` Explorer Action v16 accepts only the exact ordered original selection and every committed output transaction, then lets the host revalidate source and output identities before using its Trash
- `Fixed` The host now syncs a recovery copy and persists its Trash record before removing a source; if a directory is removed only partly, its recoverable copy is retained instead of deleting the only recovery data
- `Improved` The Trash phase cannot be cancelled and reports committed, recovery-required, failed, and unknown outcomes separately; a lost Binder response queries the host terminal state instead of blindly retrying

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
