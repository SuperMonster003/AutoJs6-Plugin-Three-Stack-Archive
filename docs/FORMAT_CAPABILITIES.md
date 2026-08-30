# Archive format capabilities

This document describes the user-visible format boundary of Archive Manager 2.22.0. It is a release contract, not a list of every format a dependency might parse internally.

## Legend

- **Yes** means the operation is available for the format when the archive itself is structurally valid and its entry methods are supported.
- **Conditional** means the operation is offered only after the current archive has been inspected and found to satisfy every condition listed below.
- **No** means Archive Manager deliberately does not advertise the operation for that format.
- Preview support also depends on the selected entry type and an installed AutoJs6 viewer. A format can be browsable even when one particular entry method cannot be opened.

## Format matrix

| Format and recognized names | Browse, preview, extract | Create | Add, new folder, rename, delete | Password and encryption | Volume handling |
| --- | --- | --- | --- | --- | --- |
| ZIP (`.zip`) | Yes, including Zip64, ZipCrypto, AES, common legacy filename encodings, and self-extracting-style preambles | Yes; stored or deflated levels 0-9; optional AES-256 password | Conditional: ordinary unencrypted single-volume ZIP with safe paths and supported entry methods | Reads ZipCrypto and AES; creates AES-256 content encryption; output filenames remain visible | Reads complete standard `.z01 + .zip` and numbered `.zip.001` sets; creates standard split ZIP with preset or custom MiB sizes; split input remains read-only |
| JAR, AAR, WAR | Yes through the ZIP reader | No | No | Reader behavior follows the underlying ZIP structure | No creation or mutation contract |
| 7Z (`.7z`) | Yes, including ordinary and solid archives, common filters, content encryption, and header encryption | Yes; non-solid Copy or LZMA2 output; optional AES-256 content password | Conditional: unencrypted, non-solid, single-volume archive using supported methods within the decoder budget | Reads content- and header-encrypted input; creates content encryption only; output filenames remain visible | Reads complete numbered `.7z.001` sets; numbered input is read-only; split creation is not available |
| RAR4 and RAR5 (`.rar`) | Yes for supported methods, including content-encrypted and header-encrypted input | No | No | Reads supported encrypted input with an in-place password retry | Reads complete modern `partN.rar` sets; all RAR input remains read-only |
| TAR (`.tar`) | Yes | Yes, uncompressed | Conditional: only safe regular files and directories | Not applicable | No split creation or mutation |
| TAR.GZ and TGZ | Yes | Yes, GZIP levels 0-9 | Conditional: only safe regular files and directories | Not applicable | No split creation or mutation |
| TAR.XZ and TXZ | Yes | Yes, bounded XZ preset | Conditional: only safe regular files and directories | Not applicable | No split creation or mutation |
| TAR.BZ2 and TBZ2 | Yes | Yes, bounded BZIP2 preset | Conditional: only safe regular files and directories | Not applicable | No split creation or mutation |
| TAR.ZST and TZST | Yes | Yes, bounded Zstandard preset | Conditional: only safe regular files and directories | Not applicable | No split creation or mutation |

Generic `.gz`, `.xz`, `.bz2`, and `.zst` files are not registered as archives. The outer compression stream must contain a structurally valid TAR payload. A supported-looking filename never overrides structural detection.

## Native AutoJs6 integration

Supported archives use the existing AutoJs6 file list rather than a second file-manager layout. The native page provides:

- external and internal path navigation through the host path bar;
- search, sorting, selection mode, and Back navigation;
- preview through existing document, image, audio, and video viewers;
- extraction of the whole archive, current internal directory, or selected entries;
- password retry without losing the current internal path;
- ZIP filename-encoding correction in the active read-only session;
- progress, cancellation, output conflict handling, directory refresh, and typed failures.

A file whose name is not registered can use **Open as archive...**. The plugin probes its structure only after that explicit action. If the detected format differs from the filename, the host path bar labels the actual format.

## Creation boundary

Archive creation accepts one file or directory, or a same-parent multi-selection. It can also create one archive per selected item. Output is limited to the current parent directory and uses a host-owned transaction.

The creation flow:

1. freezes and validates the complete source selection;
2. scans a bounded source snapshot;
3. writes hidden output without replacing an existing name;
4. completely reads the output back and verifies format, entries, sizes, CRC values, and content fingerprints;
5. publishes only verified physical outputs;
6. refreshes the host directory after the complete result is known.

Existing output names are preserved. The user can request safe automatic numbering or ask before retrying with a numbered name. Standard split ZIP and per-item creation publish their verified physical outputs as a recoverable batch.

The optional **Move source items to Trash after compression** setting is off by default. It runs only after every physical output has been committed and revalidated by the host. Explorer Action v21 keeps a bounded durable source-recovery history. Restoring sources never removes or replaces an archive that was already created.

## Mutation boundary

Writable archives are never edited in place. Every add, empty-folder creation, rename, or delete operation uses a verified full rebuild:

1. the plugin explains the planned work before reserving output;
2. retained and new entries are streamed to host-owned pending output;
3. the complete replacement is opened and verified;
4. the host atomically replaces the source only while the original identity still matches;
5. one previous-version restore remains available during bounded host retention.

An archive remains read-only when any of these conditions applies:

- the host granted only a read session;
- the format has no mutation backend;
- the archive is encrypted, solid, or multi-volume when that variant is outside the writable boundary;
- an entry has an unsafe output path, unsupported method, unsupported special type, or excessive decoder requirement;
- the source, companion volumes, or replacement identity changed;
- the host cannot provide its transactional replacement contract.

RAR is intentionally read-only. Its reader is distributed under the UnRAR license, whose restrictions are incompatible with implementing a RAR-compatible writer.

## Safety boundaries that remain enforced

- Parent traversal, absolute paths, drive-prefixed paths, control characters, and bidirectional-control names never become output paths.
- Unsafe names are exposed behind opaque IDs in a read-only virtual area. Whole-archive extraction requires an explicit decision to skip them.
- Symbolic links, hard links, device nodes, and sparse TAR entries may be listed as metadata but are never materialized as ordinary files.
- Resource-budget confirmation can expand a user-selected extraction bound, but cannot disable path containment, source identity, size, CRC, or structural checks.
- Passwords are not persisted and are cleared from mutable buffers on replacement, task completion, or page destruction on a best-effort basis.
- The plugin requests neither broad storage permission nor network permission.

## Deliberately unavailable in 2.22.0

- RAR creation or mutation.
- Creating 7Z archives with encrypted filenames or solid output.
- Creating split 7Z, split RAR, or cross-format volume sets.
- Mutating JAR, AAR, WAR, encrypted ZIP, split ZIP, solid or encrypted 7Z, or any numbered volume set.
- Preserving symbolic links or other special filesystem objects during creation or extraction.
- Creating archives directly in an arbitrary filesystem directory. Creation stays in the selected sources' current parent; whole-archive extraction may use a directory chosen through the Android system picker.
- Replacing an unrelated existing output as a name-conflict shortcut.

## Compatibility evidence

The committed compatibility corpus records producer versions, commands, physical volume membership, and SHA-256 digests. It includes fixtures produced by Windows Explorer, Bandizip, Info-ZIP, 7-Zip, WinRAR, bsdtar, Zip4j, and project-owned adversarial generators. External regression runs also cover the supplied `archive-test-2026` corpus and two macOS-origin ZIP samples.

See [compatibility/README.md](../compatibility/README.md) for the reproducible fixture contract and [ROADMAP.md](../ROADMAP.md) for completed validation evidence and future work that is not part of the current release.
