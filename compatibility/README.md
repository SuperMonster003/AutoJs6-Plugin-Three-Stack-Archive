# Archive compatibility corpus

This directory records reproducible, privacy-free archive samples produced by external tools. A sample is accepted only when its producer, exact command, contents and SHA-256 digest are recorded. Tests consume the committed files from `app/src/test/resources/archive-fixtures`.

Supported samples normally omit `expectedPluginCapability`. A sample that intentionally exercises a narrower behavior records the deviation explicitly. For example, the split RAR fixture uses `metadata-only` because Explorer Action v11 grants the selected volume descriptor but cannot yet grant its sibling volumes. An unregistered format may still use `unsupported`; merely committing a fixture never advertises a user-facing capability.

Manifest schema v2 uses `file` for the volume selected to open. That file may be a terminal ZIP volume, a first RAR volume or a numbered `.zip.001` / `.7z.001` volume. Required siblings are recorded in `companionVolumes`, with an independent size, volume index and SHA-256 for every physical file.

RAR fixtures are reader regressions only. Archive Manager can browse, preview and extract supported single-volume or complete split RAR4/RAR5 input, including content- and header-encrypted RAR5, but deliberately does not create or modify RAR archives. A fixture that contains only a first volume remains metadata-only; complete sets are exercised separately through the bounded sibling-volume contract. Numbered ZIP and 7Z fixtures exercise ordered byte-volume concatenation through that same bounded contract and remain read-only.

The corpus is intentionally incremental. A producer listed in the Roadmap is not considered covered until at least one independently generated sample and its relevant edge cases are committed and verified.

## Source material

All fixture inputs are synthetic text owned by this project. They contain no user paths, account names or device data. Input modification times are normalized before an archive is generated. The numbered-volume payload is intentionally a frozen 2.6.0 README snapshot so its committed volumes remain reproducible as documentation evolves; its text is inert test data, not a statement of current capability.

## Regeneration

Run the documented command from the repository root with the matching producer version. Then update the manifest digest and run `:app:testDebugUnitTest`.

Encrypted fixtures use the public test password recorded in `fixtures.json`. It exists only to make the corpus reproducible and must not be reused for personal data.

Generated archives are binary test data and must not be edited manually.

## Opt-in external macOS samples

The following upstream samples are not redistributed. Their tests run only when the corresponding environment variable points to a local, unmodified copy; the pinned SHA-256 prevents a different download from being accepted silently.

| Upstream sample | Environment variable | SHA-256 | Regression covered |
| --- | --- | --- | --- |
| [libzip issue 341 `hello-dolly.zip`](https://github.com/nih-at/libzip/issues/341) | `ARCHIVE_MAC_HELLO_DOLLY_SAMPLE` | `df3cfbc607582bcdd3e529800ac5559c90d9ca0c4c24179a1178f69a5d6a9dc4` | macOS Archive Utility local/central header variance, six-entry listing, and entry streaming |
| [zipkirei `test.zip`](https://github.com/yuk7/zipkirei/blob/main/tests/files/test.zip) | `ARCHIVE_MAC_ZIPKIREI_SAMPLE` | `676c57107bdc3c70c0e93eaadad5ccdfff4e1829aff23b7045f9c1c7d7b8f9e5` | valid UTF-8 names without bit 11, emoji, Chinese/Japanese/Korean paths, macOS NFD spelling, and entry streaming |

Run them together with the normal scanner tests:

```powershell
$env:ARCHIVE_MAC_HELLO_DOLLY_SAMPLE = '<path-to-hello-dolly.zip>'
$env:ARCHIVE_MAC_ZIPKIREI_SAMPLE = '<path-to-test.zip>'
.\gradlew.bat :app:testDebugUnitTest --tests MacArchiveCompatibilitySampleTest --tests ArchiveScannerTest
```

## Historical regressions

User-provided archives whose original producer and exact command are unknown are kept separately under `app/src/test/resources/regression-fixtures`. They are not counted as reproducible producer coverage. Each one is privacy-screened, renamed to a neutral fixture name, pinned by SHA-256 and exercised only as inert archive data. Archived scripts, registry files or other payloads are never executed by tests.
