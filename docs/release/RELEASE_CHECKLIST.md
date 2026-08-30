# Archive Manager release checklist

## 1. Scope and source

- [ ] Every advertised capability is already implemented and appears in `docs/FORMAT_CAPABILITIES.md`.
- [ ] Unchecked Roadmap work is not described as current behavior.
- [ ] Ordinary AutoJs6 file-manager layout and established action styling have no unrelated changes.
- [ ] Plugin and paired host working trees are clean before the release build.
- [ ] The version is `x.y.z` with no suffix and versionCode is strictly greater than the previous release.
- [ ] The release date and version match all ten Changelog JSON sources.
- [ ] No signing file, keystore, token, password, private archive, or local path entered Git.
- [ ] Added text uses three ASCII periods when an ellipsis is needed.

## 2. Plugin Center contract

- [ ] `plugin_requires_host_version` is a positive decimal value and matches service `requiresHostVersion` metadata.
- [ ] `plugin_runtime_component` names the declared Explorer Action service exactly.
- [ ] `plugin_supported_abis` matches the native libraries packaged in the final APK.
- [ ] All localized `plugin_description` values accurately cover ZIP, 7Z, RAR, and TAR without implying RAR creation or mutation.
- [ ] All localized `plugin_instruction.md` files describe the same supported and read-only boundaries.
- [ ] The official index generator is allowed to omit `protocolApiMin` and `protocolApiMax`; Explorer Action v21 is not misrepresented as a `major.minor` protocol.

## 3. Documentation

- [ ] Parse all ten `.readme/lang_*.json` and ten `.changelog/lang_*.json` files.
- [ ] Run `scripts/sync-localized-docs.ps1`.
- [ ] Run the documentation generator a second time and confirm the working diff is unchanged.
- [ ] Check README, installation guide, format matrix, Security Policy, issue forms, and user release notes.
- [ ] Confirm the latest README compatibility text names Explorer Action v21 and no completed recovery work remains listed as pending.
- [ ] Inspect every screenshot at original resolution and confirm it contains no private path, notification, account, device identifier, password, or unrelated app content.

## 4. Automated verification

- [ ] Run the full JVM suite with the committed fixture manifest.
- [ ] Run the external `archive-test-2026` corpus.
- [ ] Run both external macOS ZIP regression samples.
- [ ] Run Android instrumentation on API 24, API 25, and a current Android release.
- [ ] Exercise the real paired host/plugin flow for open, extract, create, source-to-Trash, source restore, archive mutation, and previous-version restore as applicable.
- [ ] Run `:app:lintDebug` and attribute warnings to changed or pre-existing files.
- [ ] Run `:app:assembleRelease` from a clean source state.

## 5. Artifact verification

- [ ] Confirm application ID, version name, versionCode, minSdk, targetSdk, and the absence of `debuggable` and `testOnly`.
- [ ] Confirm exactly one expected signer and record its SHA-256 certificate digest.
- [ ] Confirm the expected APK signing schemes.
- [ ] Run `zipalign -c -P 16 -v 4`.
- [ ] Enumerate native ABIs and confirm they match `plugin_supported_abis`.
- [ ] Inspect native ELF load alignment and RELRO when native libraries changed.
- [ ] Record final APK size, SHA-256, and MD5.
- [ ] Copy the APK to the ignored local `releases` directory and prove it is byte-identical to the build output.
- [ ] Install the final Release APK on each release-gate device and verify the installed package is not debuggable.

## 6. Device cleanup

- [ ] Remove temporary host, plugin, test packages, fixtures, screenshots, and UI dumps from test-only devices.
- [ ] Restore any device that had a pre-test plugin or host version and verify the installed APK against its frozen digest.
- [ ] Record devices deliberately not touched.

## 7. Local release receipt

- [ ] Commit the complete source and documentation change locally.
- [ ] Confirm the post-commit working tree is clean.
- [ ] Record the source commit and final artifact facts in the Roadmap.
- [ ] Keep the local APK ignored; do not add it to Git.

## 8. Remote publication, only after separate authorization

- [ ] Push the exact source commit.
- [ ] Create signed or otherwise approved tag `v2.22.0`.
- [ ] Upload only the verified APK and user-facing release notes.
- [ ] Confirm the GitHub release asset size and digest match the local receipt.
- [ ] Add the exact final admission manifest to `AutoJs6-Official-Plugins-Index`.
- [ ] Regenerate and review the official index.
- [ ] Verify installation and update behavior from AutoJs6 Plugin Center.
