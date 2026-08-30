# Archive Manager 2.22.0

Archive Manager 2.22.0 is the first-publication preparation release. Archive processing remains compatible with 2.21.0, with one asynchronous mutation-lifecycle race fixed; this release also makes its installation contract and supported format boundary precise enough for AutoJs6 Plugin Center and public issue reporting.

## Highlights

- AutoJs6 Plugin Center can derive the minimum host version, exact Explorer Action service component, and four packaged ABIs directly from the release tag.
- Plugin descriptions and instructions now cover ZIP, 7Z, RAR, and TAR-family behavior without implying that read-only formats are writable.
- A complete format matrix distinguishes browsing, preview, extraction, creation, conditional mutation, encryption, and volume handling.
- The installation guide explains the paired AutoJs6 6.8.0 (5276) plus Explorer Action v21 requirement, signature permission, upgrades, downgrades, and recovery precautions.
- Bug, archive-compatibility, and feature-request forms collect reproducible facts while warning against public disclosure of private archives or passwords.
- The release checklist binds source, documentation, tests, screenshots, APK signing, alignment, ABIs, checksums, device cleanup, and the later official-index admission step.
- README compatibility text no longer names v20 as the current protocol or lists delivered source-recovery history as unfinished work.
- Archive-add input grants and active-operation state are now released before the terminal callback, so an immediate retry after completion or failure is not incorrectly rejected as another operation already running.

## Current archive capabilities

- Browse, preview, and extract ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST.
- Read supported encrypted ZIP, 7Z, and RAR input with in-place password retry.
- Read supported standard split ZIP, modern WinRAR volume sets, numbered ZIP, and numbered 7Z through bounded host-approved sibling descriptors.
- Create ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST. ZIP supports AES-256 and standard split output; 7Z supports optional content encryption.
- Safely rebuild eligible ordinary ZIP, 7Z, and TAR-family archives to add content, create empty directories, rename, or delete entries.
- Optionally move source items to the host Trash after every created output is committed. The default remains off, and Explorer Action v21 provides durable source-recovery history without removing the created archive.

See `docs/FORMAT_CAPABILITIES.md` for the exact conditional and read-only boundaries.

## Compatibility

- Android 7.0 or newer.
- AutoJs6 6.8.0, versionCode 5276 or newer, with Explorer Action v21.
- `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` in one universal APK.
- Official AutoJs6 signing certificate for the signature-level plugin permission.

This is still a local release-preparation stage. No GitHub tag, GitHub Release, official-index update, or remote push is implied.
