# Archive management entry regression

## Contract

The archive workspace introduced in `6230618` exposes the management page for inspection and
extraction of readable archives, including JAR, AAR, WAR and RAR. Opening that page does not grant
editing capabilities. `ThreeStackArchivePlugin.canModifyFileName` and the management status checks
still restrict modification to eligible ZIP, 7Z and TAR files. Android package aliases use the
separate read-only archive entry point.

The old `manageArchiveRejectsZipContainerAliases` assertion contradicted this contract. Its
replacement checks successful management entry and the continued prohibition on modification.
The adjacent RAR test also used `Intent.setType`, which cleared the data URI and caused rejection
before the format check. It now builds a complete envelope and checks the read-only contract.
The Android package test checks rejection by management and acceptance by the read-only entry
point using the same otherwise valid envelope.

## Verification

- Sony XQ-DQ72, Android 13 / API 33: reproduced the original failure, 13 passed / 1 failed.
- The corrected `ArchiveIntentPolicyInstrumentationTest`: 15 passed, no skips.
- JVM suite: 225 passed, 8 existing optional external-corpus tests skipped because their sample
  environment variables were not supplied.
- Debug APK, instrumentation APK, `lintDebug`, and Markdown generation check passed.
- Runtime behavior and public APIs are unchanged; this is a test correction.

Run after `:app:assembleDebug :app:assembleDebugAndroidTest` and installation:

```powershell
adb -s <serial> shell am instrument -w -r `
  -e class io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveIntentPolicyInstrumentationTest `
  io.github.supermonster003.autojs6.plugin.three.stack.archive.test/androidx.test.runner.AndroidJUnitRunner
```

Local build and device logs are under `build/verification/plugin-test-repair/` and remain ignored.
