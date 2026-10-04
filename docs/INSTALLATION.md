# Installation and compatibility

## Requirements

Archive Manager 2.22.2 requires all of the following:

- Android 7.0 or newer (`minSdk 24`).
- AutoJs6 6.8.0 with version code 5276 or newer.
- An AutoJs6 build that includes Explorer Action protocol v21.
- A plugin APK signed with a certificate trusted by the installed AutoJs6 host.
- One of the packaged Android ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, or `x86_64`.

The official Plugin Center metadata declares minimum host version code 5276. During local co-development, version code alone is not proof that a particular AutoJs6 6.8.0 build contains Explorer Action v21. The plugin also negotiates the Explorer Action protocol at runtime and does not call v21 transactions when the host does not advertise them. The compatible local host baseline includes AutoJs6 commit `d7bc884d6749369a9045fc545d3a3f3a7710a55f`.

## Recommended installation

1. Install or update the compatible AutoJs6 host first.
2. Open **Plugin Center** in AutoJs6.
3. Install Archive Manager from the official index, or install the verified local APK when testing before publication.
4. Enable **Archive Manager** in Plugin Center.
5. Open the AutoJs6 file manager and check a supported archive's file menu. **Open archive**, **Extract to...**, and **Manage archive...** appear only when their matcher and protocol requirements are satisfied.
6. Check an ordinary file or directory menu for **Compress...**. The same action is available for a same-parent multi-selection.

Release downloads include one APK per ABI and a universal APK containing all four listed ABIs. Do not choose an APK only by the Android device's marketing name; verify the release filename, version, size, SHA-256 digest, and signer certificate from the release receipt.

## Sideloading the local release

Build and validate the release with `./gradlew.bat :app:appendDigestToReleasedFiles`, then select the filename recorded in `releases/v2.22.2/release-manifest.json`. Use an explicitly selected device when more than one Android device is connected:

```powershell
adb -s <serial> install -r .\releases\v2.22.2\autojs6-plugin-three-stack-archive-v2.22.2-universal-<CRC32>.apk
```

Archive Manager requests the host's signature-level `org.autojs.permission.PLUGIN` permission. A locally rebuilt plugin signed with an unrelated certificate may install, but the compatible host will not grant the plugin contract. Use the AutoJs6 signing configuration for paired local builds.

## Upgrades and downgrades

- Install a newer build in place when it uses the same package name and signer.
- Finish or inspect any pending archive replacement and source-recovery work before uninstalling the plugin.
- Uninstalling the plugin removes its private cache and preferences. Host-owned Trash data remains under host policy, but opaque recovery history is scoped to the plugin UID and exact parent-directory identity; do not rely on uninstall and reinstall as a recovery workflow.
- Downgrading can require uninstalling the newer package on Android. That loses plugin-private state, so use the host Trash and the visible recovery controls first.
- Never overwrite an older APK file and assume its checksum still identifies the same release. Treat version, size, SHA-256, signer, and source commit as one receipt.

## Permission model

Archive Manager does not request broad storage access or network access.

- Browsing uses a short-lived host-provided read-only descriptor or a bounded private compatibility cache.
- Same-directory extraction and creation write through host-owned transactions bound to the plugin UID.
- Extraction to another directory uses only the directory selected through the Android system picker.
- Multi-volume reads use bounded host-approved sibling descriptors and opaque IDs, not directory paths.
- Passwords are not persisted in settings, logs, diagnostics, or durable protocol records.

## Troubleshooting

### The plugin is installed but does not appear

- Confirm it is enabled in AutoJs6 Plugin Center.
- Confirm the package is `io.github.supermonster003.autojs6.plugin.three.stack.archive`.
- Confirm the APK signer is trusted by the host.
- Confirm the device ABI is one of the four packaged ABIs.

### Archive actions are missing

- Confirm the host advertises Explorer Action protocol v21.
- Confirm the filename has a registered extension or compound suffix.
- For an unrecognized name, use **Open as archive...** from the file menu and let structural detection decide.
- Generic `.gz`, `.xz`, `.bz2`, and `.zst` streams are intentionally not registered unless they contain TAR and use a supported TAR compound name.

### An archive opens read-only

This can be the correct result. RAR, split archives, numbered volumes, JAR/AAR/WAR, encrypted ZIP, and encrypted or solid 7Z are outside the writable boundary. A normally writable format also becomes read-only when it contains unsafe names, unsupported methods, special TAR entries, exceeds a mutation decoder budget, or was opened without a host replacement session.

### A password is rejected

- Retry in the same host password prompt; the archive path and session are retained where possible.
- Confirm whether the archive encrypts only content or also filenames/headers.
- The public compatibility corpus uses documented test passwords only. Never infer a real archive password from fixture documentation.

### A source cannot be restored after compression

- Open the creation page's **Source recovery history** and refresh the matching parent directory session.
- Restoration never overwrites an occupied original name. Move or rename the conflicting item first if it is safe to do so, then re-query the batch.
- Restoring sources deliberately keeps every created archive.
- If plugin history is no longer available, inspect the ordinary AutoJs6 Trash UI. Protocol history expiration does not itself delete the underlying Trash item.

### Reporting a compatibility problem

Use the repository's **Archive compatibility report** issue form. Include the Archive Manager version, AutoJs6 version and version code, Android version, device ABI, archive format, producer and version, encryption/volume details, and the stable error stage/code. Attach a sample only when it contains no private data and you are authorized to redistribute it.
