# Archive workspace and Explorer integration

The September 2026 UI update is implemented across Archive Manager and the local AutoJs6 host. Install matching builds of both projects to use all of the new interactions.

## File manager

- Regular archives use an eye icon to view their contents and an information icon to open the existing archive management Activity.
- Context menu labels show the action without a plugin-name prefix. Archive viewing is also available in the context menu.
- APK, APKS, XAPK, APKM, APKZ and AAB are inspected as ZIP containers. They use the explicit **Open as archive...** menu and **Extract to...** action, preserving the existing Inspector/install/information buttons, including on older hosts. The information dialog's upper-right menu exposes the same archive actions.
- Archive browsing has no action icons on the right of the path bar. Long-press entries to select them, then choose **Extract** and select a destination folder. Cancelling the picker creates no output.
- Clicking a readable file materializes that file in a private temporary directory. Scripts use the host's normal interactive execution path; other files use the corresponding viewer/open action. Valid filenames, including runtime suffixes such as `.node.js` and `.bun.ts`, are preserved. Opened temporary files remain available after leaving the archive and are eligible for stale-cache cleanup after 24 hours.
- The selected extraction directory stays inside the host. The plugin receives a bounded output session, with no source-trash, replacement or destination-reading permission.

## Independent Activities

Both management and creation use a coordinated blue accent with separate light and dark surface, text, outline and error colors. The app follows system night mode. System bars also have an Android 7/8 compatible fallback.

The management page groups the archive summary into a card, keeps extraction options in a compact strip and gives the file list the remaining space. A fixed footer keeps extraction visible at narrow widths; capability-dependent editing actions are collected in **Actions**. File and folder rows use vector icons.

The creation form separates selection, destination, compression parameters, password/encryption and post-compression behavior into cards. Existing password, split-volume, conflict and source-recovery behavior is retained.

Package formats are available for viewing and extraction. Archive modification remains limited to eligible ZIP, 7Z and TAR-family files; JAR/AAR/WAR and Android packages do not gain editing capabilities.

## Verification

Run `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleRelease` and the focused Android instrumentation tests. The host additionally has regression tests for menu placement, runtime suffix retention and extraction into a separate directory with restricted source access.

The device regression tests can save actual Activity screenshots by passing `-e archiveUiScreenshots true` to the instrumentation runner. Artifacts are written to the plugin's external files directory under `ui-screenshots`.

The final local verification on 2026-09-08 completed with:

- Plugin unit tests: 230 total, 222 passed and 8 environment-dependent tests skipped.
- Plugin Debug/Release APK builds and Debug Lint: passed.
- Plugin Activity, accessibility and contract device tests: all 30 passed on API 36.
- Host focused unit tests: all 13 passed in an isolated worktree.
- Paired host/plugin device tests: selected-directory output isolation, all six package aliases and execution of a materialized script after archive closure passed.

Actual UI captures are available in [the screenshot index](images/screenshots/README.md).
