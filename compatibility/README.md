# Archive compatibility corpus

This directory records reproducible, privacy-free archive samples produced by external tools. A sample is accepted only when its producer, exact command, contents and SHA-256 digest are recorded. Tests consume the committed files from `app/src/test/resources/archive-fixtures`.

Supported samples normally omit `expectedPluginCapability`. A sample that intentionally exercises a narrower behavior records the deviation explicitly. For example, the split RAR fixture uses `metadata-only` because Explorer Action v11 grants the selected volume descriptor but cannot yet grant its sibling volumes. An unregistered format may still use `unsupported`; merely committing a fixture never advertises a user-facing capability.

Manifest schema v2 retains `file` as the primary/final archive file and records any required split companions in `companionVolumes`, with an independent size, volume index and SHA-256 for every physical file.

RAR fixtures are reader regressions only. Archive Manager can browse, preview and extract supported single-volume RAR4/RAR5 input, including content- and header-encrypted RAR5, but deliberately does not create or modify RAR archives. Split RAR first volumes expose safe metadata without an extraction action until the host adds a bounded sibling-volume contract.

The corpus is intentionally incremental. A producer listed in the Roadmap is not considered covered until at least one independently generated sample and its relevant edge cases are committed and verified.

## Source material

All fixture inputs are synthetic text owned by this project. They contain no user paths, account names or device data. Input modification times are normalized before an archive is generated.

## Regeneration

Run the documented command from the repository root with the matching producer version. Then update the manifest digest and run `:app:testDebugUnitTest`.

Encrypted fixtures use the public test password recorded in `fixtures.json`. It exists only to make the corpus reproducible and must not be reused for personal data.

Generated archives are binary test data and must not be edited manually.

## Historical regressions

User-provided archives whose original producer and exact command are unknown are kept separately under `app/src/test/resources/regression-fixtures`. They are not counted as reproducible producer coverage. Each one is privacy-screened, renamed to a neutral fixture name, pinned by SHA-256 and exercised only as inert archive data. Archived scripts, registry files or other payloads are never executed by tests.
