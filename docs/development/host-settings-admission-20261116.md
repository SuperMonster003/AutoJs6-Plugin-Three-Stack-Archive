# Follow AutoJs6 settings admission fix

Investigation started on 2026-11-16; device verification completed on 2026-11-17.

The Maple investigation identified the same missing `org.autojs.plugin.INFO`
service in Stack. The host settings provider rejects an application without an
enabled, exported INFO service protected by `org.autojs.permission.PLUGIN`, even
when its Explorer Action service works and its signature permission is granted.
The client treats that denial as an unavailable appearance contract.

`PluginInfoService` now implements the common `IPluginInfoProvider` Binder and
reuses `archiveManagerPluginInfo`. It preserves installed-package ABI discovery,
identity and capabilities. Existing Explorer Action and tool services are unchanged.

## Verification

- Workspace source audit: 59 Android plugin applications, 29 host appearance
  consumers, 2 missing INFO entries (Maple and Stack). Both are corrected.
- Python manifest regression: failed before the fix, passed afterward. All 22
  root Python tests, 4 icon tests and generated Markdown checks pass.
- Gradle Debug assembly, unit tests, Android test APK assembly, Debug lint and
  signed release collection pass with two workers and version auto-increment disabled.
- JVM: 252 tests, 244 passed, 8 skipped, no failures. The skipped tests require
  external archive/macOS sample corpora and are unrelated to host settings.
- Debug lint: 0 errors, 105 warnings; broader lint cleanup is outside this fix.
- Both `HostSettingsContractInstrumentationTest` cases pass against the signed
  Debug APK on BH900ASK9E with `-e requireHostSettings true`.
- BH900ASK9E (Sony G8441, Android 9 / API 28) already had Stack 3.1.0 (127) and
  AutoJs6 6.8.1 (6641). Before: INFO count 0 and the real settings call threw
  `SecurityException: Official plugin settings access denied: PLUGIN_ENTRY_UNAVAILABLE`.
- After installing the signed release 3.1.3 (134), a framework-only probe under
  Stack's UID passed INFO discovery, permission, Binder descriptor and metadata
  parity with Explorer Action, and the real host appearance call: 2/2 passed.
- Host result: protocol 1, host 6.8.1 (6641), resolved language `en`, night policy
  `FOLLOW_SYSTEM`, active `false`, theme primary/accent `#FFDEAD`.
- Old/new APK signer certificate SHA-256 match. `adb install -r` preserved the
  data directory inode and original installation time. Final installed APK is release.
- All five signed ABI/universal APKs were collected and alignment-checked. Device
  testing here covers arm64 only, not every ABI or a physical 16 KB environment.

No archive operation or settings-dialog visual pass is claimed for this task.
QV770340J7 did not have Stack installed, so verification used an existing Stack
installation on BH900ASK9E.

Release APKs are under `releases/v3.1.3/`. `.bak/host-settings-20261116/` retains
the original package/test APKs, package states, build log and actual before/after
instrumentation results. These local artifacts are ignored by Git.
