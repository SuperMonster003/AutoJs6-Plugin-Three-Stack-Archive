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

Archive Manager works inside the AutoJs6 file manager instead of replacing it. Supported archives use the host list, path bar, theme, viewers, selection mode, progress UI, and directory refresh. A separate management page remains for detailed format information and settings that need a richer form.

### Screenshots

Authentic Android captures show the host menu integration, native archive browsing, archive creation, and detailed management. Only synthetic public data is shown.

<table>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/explorer-actions.png?raw=true" alt="Archive actions in AutoJs6 Explorer" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/native-archive-browsing.png?raw=true" alt="Native archive browsing" width="280" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/create-archive-form.png?raw=true" alt="Archive creation form" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/archive-management.png?raw=true" alt="Archive management page" width="280" /></td>
  </tr>
</table>

- Capture notes and full screenshot set: [docs/images/screenshots/README.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/README.md)

### Available now

- Open ZIP-, 7Z-, and TAR-family archives directly in the native AutoJs6 file list, using the host theme, dark mode, and dynamic colors.
- Show the external directory, archive name, and internal directory in the path bar; jump by tapping a level and use Back to move up before leaving the archive.
- Extract the current internal folder from the path bar, or enter selection mode and extract selected files and folders, without leaving the native host page; progress is shown, the task can be cancelled, and the parent directory refreshes on completion.
- Preview supported document, image, audio, and video entries with the host's existing viewers.
- Use Extract to... to extract an entire archive into the recommended same-directory folder, or choose another folder with the Android system picker; equivalent existing folder names are safely numbered.
- Choose Manage archive... to open the management page, extract the entire archive, current internal folder, or current checkbox selection, or edit an ordinary single-volume ZIP or eligible TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, or TAR.ZST/TZST archive with Add files..., Add folder..., New folder..., Rename..., and Delete; Extract to... remains the whole-archive shortcut.
- ZIP and TAR changes are planned before writing, rebuilt into host-owned pending output, fully read back, and atomically replace the original only after verification. Cancellation or failure leaves the source unchanged, and a successful commit refreshes Explorer automatically.
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

> Native integration requires the paired AutoJs6 6.8.0 build with Explorer Action v21 (version code 5276 or newer). RAR and split archives are deliberately read-only. Editing is available for ordinary single-volume ZIP, unencrypted non-solid single-volume 7Z within the decoder budget, and TAR-family archives that contain only safe regular files and directories. Encrypted, solid, split, unsafe, unsupported-method, or over-budget 7Z variants remain read-only. Open a standard split ZIP through its final `.zip`, a modern WinRAR set through its first `partN.rar` volume, and a numbered ZIP or 7Z set through its `.001` volume, with every required sibling in the same directory. Filename-encrypted creation and in-archive editing for JAR/AAR/WAR and RAR are unavailable.

### Usage

1. Install the plugin and enable it in the AutoJs6 Plugin Center.
2. Open the file menu for a ZIP, JAR, AAR, WAR, 7Z, or TAR-family archive.
3. Choose Open archive, then enter directories, search, or jump with the path bar in the host file list.
4. To extract the entire archive, choose Extract to... from its file menu. Use the recommended current folder or choose another folder with the Android system picker, then confirm the exact output path.
5. To extract the current internal folder, use the extraction button at the right of the path bar. To extract specific entries, touch and hold an entry, select files or folders, and tap Extract in the bottom bar. Use Manage archive... or Extract to... when a password, encoding correction, unsafe-path confirmation, conflict policy, or another destination is required.
6. To edit an ordinary single-volume ZIP or eligible TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, or TAR.ZST/TZST archive, choose Manage archive... and use Add files..., Add folder... to import a complete folder tree, New folder... to create an empty folder, Rename..., or Delete. Wait for rebuilding, verification, and the success message before leaving the page.
7. On the management page, choose how equivalent output names are handled before extraction. Ask each time can apply one skip, overwrite, or auto-rename decision to all compatible remaining conflicts.
8. To create an archive, choose Compress... from an ordinary file or folder menu, or select multiple items in one directory and use Compress... in the bottom action bar. To create one archive per item, enable Compress each item separately, review the output preview, and create; this mode always resolves name conflicts with safe automatic numbering. For ZIP, choose No split, a common MiB preset, or a custom whole number from 1 to 4096 MiB; when output exceeds that size, it consists of `.z01`, `.z02`, ... numbered volumes and a final `.zip`, while smaller output remains one `.zip`.

### Permissions and data

Archive Manager requests neither storage nor network permission. The host supplies short-lived read-only descriptors and UID-pinned output transactions, so the plugin cannot choose arbitrary filesystem paths. Explorer Action v11 carries a password only in a bounded synchronous retry request; both sides immediately remove and clear retained buffers and never persist it. Explorer Action v12 adds only a bounded, session-scoped catalog of host-approved sibling volumes: the plugin receives opaque IDs instead of paths, and caller UID, file identity, size, modification time, and lifecycle are revalidated before use. Explorer Action v13 only reindexes the same staged source and retains the old state until a complete replacement index is ready. Explorer Action v14 matches only bounded compound suffixes such as `.zip.001` and `.7z.001`, never arbitrary `.001` files, and reuses the v12 catalog without granting directory or path access. Explorer Action v17 adds only a matcher-free read-only overflow fallback when the normal primary action does not match; it runs one existing archive session after a user click and grants no new path, directory, or write access. Android and Java libraries can still create unavoidable short-lived runtime copies, so password cleanup is best-effort memory hygiene rather than an absolute claim. Path traversal and unsafe names remain isolated, output is verified before publication, and resource-budget confirmation never disables structural safety checks.

Explorer Action v15 groups only verified new-file outputs from one session into a recoverable batch of at most 128 members. Explorer Action v16 lets the host revalidate sources and outputs and move sources to Trash only after the plugin supplies the exact ordered original selection and every committed output transaction. The host syncs a recovery copy and persists its record before removing source data; the plugin receives no arbitrary-path or direct-delete capability. A lost Binder response is resolved by querying the same idempotent terminal result, not by retrying the move.

Explorer Action v18 keeps the previous archive only in host-private durable storage and returns an opaque history ID, never a backup path. One restore is allowed only while the parent and target still match the exact committed replacement. External changes make the history stale; interrupted recovery evidence is retained and blocks another replacement of that target until the host can resolve it. Normal history is bounded by age, count, total bytes, and free-space reserve, while v8-v17 sessions create no replacement backup. Explorer Action v19 passes only opaque IDs and a safe leaf name for existing entries whose capabilities allow delete or rename. Explorer Action v20 adds a frozen grant for only the explicitly selected input roots: the plugin receives bounded opaque nodes, metadata, and one-shot read-only descriptors, never source paths, URIs, unselected siblings, or general storage access. The host revalidates the complete snapshot before commit, and any changed or failed input aborts the whole archive replacement. Explorer Action v21 keeps bounded host-owned source-recovery batches across Activity and host-session recreation; source restore never overwrites an occupied name or removes the created archive.

### Roadmap

Native browsing, extraction, creation, archive mutation, previous-version restore, and durable source recovery are complete without changing the ordinary file-manager layout. Unchecked Roadmap items are future optional protocol, backend, or edge-case enhancements and are not current features.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Release notes

#### v2.22.1

_2026/09/11_

- `Improved` Build verification of 16 KB page alignment for 64-bit native libraries, including manifest contract checks and JSON reports

#### v2.22.0

_2026/08/30_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v21 (version code 5276 or newer)
- `Added` Plugin Center can now identify the required host version, runtime service, and packaged device architectures directly from the plugin
- `Fixed` Public descriptions and instructions now distinguish browsing, extraction, creation, editing, encryption, and volume support without overstating writable formats
- `Fixed` Archive input grants and active-operation state now close before the terminal callback, so an immediate retry is no longer rejected as another operation already running
- `Improved` A format capability matrix and installation guide now make compatibility, read-only boundaries, recovery behavior, and troubleshooting easier to check before installation

#### v2.21.0

_2026/08/30_

- `Note` This release requires the paired AutoJs6 6.8.0 build with Explorer Action v21 (version code 5276 or newer)
- `Added` Archive creation now keeps a completion page and Source recovery history; sources moved to Trash by explicit opt-in can be restored after Activity or host-session recreation
- `Fixed` Source restore never overwrites an existing name or deletes a created archive; conflicts and partial or interrupted restores retain recovery evidence and report per-item outcomes
- `Improved` The host owns the bounded durable history and exposes only opaque metadata; recovery UI stays inside the plugin compression page and the ordinary file-manager layout remains unchanged

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

- Installation guide: [docs/INSTALLATION.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/INSTALLATION.md)
- Format capability matrix: [docs/FORMAT_CAPABILITIES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/FORMAT_CAPABILITIES.md)
- Security policy: [SECURITY.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/SECURITY.md)
- AutoJs6 documentation: https://docs.autojs6.com
- Third-party notices: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/16kb.md)
