# Archive Manager release checklist

## 1. Scope and source

- [x] Every advertised capability is already implemented and appears in `docs/FORMAT_CAPABILITIES.md`.
- [x] Unchecked Roadmap work is not described as current behavior.
- [x] Ordinary AutoJs6 file-manager layout and established action styling have no unrelated changes.
- [x] Plugin and paired host working trees are clean before the release build.
- [x] The version is `x.y.z` with no suffix and versionCode is strictly greater than the previous release.
- [x] The release date and version match all ten Changelog JSON sources.
- [x] No signing file, keystore, token, password, private archive, or local path entered Git.
- [x] Added text uses three ASCII periods when an ellipsis is needed.

## 2. Plugin Center contract

- [x] `plugin_requires_host_version` is a positive decimal value and matches service `requiresHostVersion` metadata.
- [x] `plugin_runtime_component` names the declared Explorer Action service exactly.
- [x] `plugin_supported_abis` matches the native libraries packaged in the final APK.
- [x] All localized `plugin_description` values accurately cover ZIP, 7Z, RAR, and TAR without implying RAR creation or mutation.
- [x] All localized `plugin_instruction.md` files describe the same supported and read-only boundaries.
- [x] The official index generator is allowed to omit `protocolApiMin` and `protocolApiMax`; Explorer Action v21 is not misrepresented as a `major.minor` protocol.

## 3. Documentation

- [x] Parse all ten `.readme/lang_*.json` and ten `.changelog/lang_*.json` files.
- [x] Run `scripts/sync-localized-docs.ps1`.
- [x] Run the documentation generator a second time and confirm the working diff is unchanged.
- [x] Check README, installation guide, format matrix, Security Policy, issue forms, and user release notes.
- [x] Confirm the latest README compatibility text names Explorer Action v21 and no completed recovery work remains listed as pending.
- [x] Inspect every screenshot at original resolution and confirm it contains no private path, notification, account, device identifier, password, or unrelated app content.

## 4. Automated verification

- [x] Run the full JVM suite with the committed fixture manifest.
- [x] Run the external `archive-test-2026` corpus.
- [x] Run both external macOS ZIP regression samples.
- [x] Run Android instrumentation on API 24, API 25, and a current Android release.
- [x] Exercise the real paired host/plugin flow for open, extract, create, source-to-Trash, source restore, archive mutation, and previous-version restore as applicable.
- [x] Run `:app:lintDebug` and attribute warnings to changed or pre-existing files.
- [x] Run `:app:assembleRelease` from a clean source state.

## 5. Artifact verification

- [x] Confirm application ID, version name, versionCode, minSdk, targetSdk, and the absence of `debuggable` and `testOnly`.
- [x] Confirm exactly one expected signer and record its SHA-256 certificate digest.
- [x] Confirm the expected APK signing schemes.
- [x] Run `zipalign -c -P 16 -v 4`.
- [x] Enumerate native ABIs and confirm they match `plugin_supported_abis`.
- [x] Inspect native ELF load alignment and RELRO when native libraries changed.
- [x] Record final APK size, SHA-256, and MD5.
- [x] Copy the APK to the ignored local `releases` directory and prove it is byte-identical to the build output.
- [x] Install the final Release APK on each release-gate device and verify the installed package is not debuggable.

## 6. Device cleanup

- [x] Remove temporary host, plugin, test packages, fixtures, screenshots, and UI dumps from test-only devices.
- [x] Restore any device that had a pre-test plugin or host version and verify the installed APK against its frozen digest. No final-gate device had a pre-test host or Archive Manager installation, so no restoration was required.
- [x] Record devices deliberately not touched.

## 7. Local release receipt

- [x] Commit the complete source and documentation change locally.
- [x] Confirm the post-commit working tree is clean.
- [x] Record the source commit and final artifact facts in the Roadmap.
- [x] Keep the local APK ignored; do not add it to Git.

## 2.22.0 local verification receipt

| Gate | Verified result |
| --- | --- |
| Source baseline | `c7f00d37803254122f2ed685412c5eacec26e618` |
| JVM | 32 suites, 229 tests, 0 failures, 0 errors, 0 skipped |
| External compatibility | 24 logical fixtures/31 physical files, `archive-test-2026`, and both external macOS ZIP samples executed |
| Android instrumentation | API 24: 178/178; API 25: 178/178; API 36: 178/178; total 534/534 |
| Lint | 0 fatal, 0 errors, 62 warnings; warnings classified as the existing dependency/resource baseline |
| Final signed install | Exact APK installed and read back byte-identically on API 24, API 25, and API 36; all three rejected `run-as` as not debuggable |
| APK | 4,573,880 bytes; SHA-256 `FED769348CE5A96233F8A53CBBCF710854E752973C1E2BFC66C80CED4ACBB90F`; MD5 `E6DE4FEFF75CA2BE03A47634B8E4A22F` |
| Signer | One v2 signer; certificate SHA-256 `31A681FCFFFB3E428420CAE280DED89292B12A3B0F59E19B7A73E32A8AE4C213`, identical to the paired AutoJs6 host |
| Native packaging | Exactly `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`; packaged libraries match stripped outputs; each ELF has three `LOAD` segments aligned to `0x4000` and includes `GNU_RELRO` |
| Device cleanup | API 24/API 25 returned to no host/plugin/test packages; the read-only API 36 gate was stopped; the existing API 37 AVD and all physical devices were not touched by the final installation gate |
| Remote state | No push, tag, GitHub Release, or official-index mutation performed |

## 8. Remote publication, only after separate authorization

- [ ] Push the exact source commit.
- [ ] Create signed or otherwise approved tag `v2.22.0`.
- [ ] Upload only the verified APK and user-facing release notes.
- [ ] Confirm the GitHub release asset size and digest match the local receipt.
- [ ] Add the exact final admission manifest to `AutoJs6-Official-Plugins-Index`.
- [ ] Regenerate and review the official index.
- [ ] Verify installation and update behavior from AutoJs6 Plugin Center.
