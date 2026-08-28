# Archive compatibility corpus

This directory records reproducible, privacy-free archive samples produced by external tools. A sample is accepted only when its producer, exact command, contents and SHA-256 digest are recorded. Tests consume the committed files from `app/src/test/resources/archive-fixtures`.

Supported samples normally omit `expectedPluginCapability`. A sample that intentionally exercises a narrower behavior records the deviation explicitly. For example, the split RAR fixture uses `metadata-only` because Explorer Action v11 grants the selected volume descriptor but cannot yet grant its sibling volumes. An unregistered format may still use `unsupported`; merely committing a fixture never advertises a user-facing capability.

Manifest schema v2 uses `file` for the volume selected to open. That file may be a terminal ZIP volume, a first RAR volume or a numbered `.zip.001` / `.7z.001` volume. Required siblings are recorded in `companionVolumes`, with an independent size, volume index and SHA-256 for every physical file.

RAR fixtures are reader regressions only. Archive Manager can browse, preview and extract supported single-volume or complete split RAR4/RAR5 input, including content- and header-encrypted RAR5, but deliberately does not create or modify RAR archives. A fixture that contains only a first volume remains metadata-only; complete sets are exercised separately through the bounded sibling-volume contract. Numbered ZIP and 7Z fixtures exercise ordered byte-volume concatenation through that same bounded contract and remain read-only.

The corpus is intentionally incremental. A producer listed in the Roadmap is not considered covered until at least one independently generated sample and its relevant edge cases are committed and verified.

## Automated format matrix

`ArchiveFormatCompatibilityMatrixTest` uses one committed production fixture for every readable format. Each row must pass with its canonical name, with no extension, and with a misleading extension that belongs to a different supported format. The detected format must always come from structure, while the display-name match remains false for the latter two cases. A separate signature-preserving damage case must report the detected format together with `INDEX/MALFORMED_ARCHIVE`.

| Format | Frozen detection fixture | Canonical name | No extension | Misleading supported extension | Typed damage | Standalone stream rejection |
| --- | --- | --- | --- | --- | --- | --- |
| ZIP | Windows Explorer 11 ZIP | Covered | Covered | Covered | Covered | Not applicable |
| 7Z | 7-Zip 22 solid LZMA2 | Covered | Covered | Covered | Covered | Not applicable |
| RAR | WinRAR 6 RAR5 | Covered | Covered | Covered | Covered | Not applicable |
| TAR | 7-Zip 22 USTAR | Covered | Covered | Covered | Covered | Not applicable |
| TAR.GZ | 7-Zip 22 GZIP-wrapped USTAR | Covered | Covered | Covered | Covered | Covered |
| TAR.XZ | 7-Zip 22 XZ-wrapped USTAR | Covered | Covered | Covered | Covered | Covered |
| TAR.BZ2 | 7-Zip 22 BZIP2-wrapped USTAR | Covered | Covered | Covered | Covered | Covered |
| TAR.ZST | bsdtar 3.8.4 Zstandard-wrapped USTAR | Covered | Covered | Covered | Covered | Covered |

The misleading-name column deliberately uses another registered archive suffix rather than a neutral `.bin` name. This proves that backend order and a plausible but wrong extension cannot override the detected structure. `CompressedTarArchiveBackendTest` independently generates valid GZIP, XZ, BZIP2 and Zstandard streams whose payload is not TAR; all four remain `FORMAT_DETECTION/INVALID_SIGNATURE`, even though their outer compressor signatures are valid. The plugin catalog and intent policy also omit the generic `gz`, `xz`, `bz2` and `zst` leaves.

## Source material

All fixture inputs are synthetic text owned by this project. They contain no user paths, account names or device data. Input modification times are normalized before an archive is generated. The numbered-volume payload is intentionally a frozen 2.6.0 README snapshot so its committed volumes remain reproducible as documentation evolves; its text is inert test data, not a statement of current capability.

## Regeneration

Run the documented command from the repository root with the matching producer version. Then update the manifest digest and run `:app:testDebugUnitTest`.

Encrypted fixtures use the public test password recorded in `fixtures.json`. It exists only to make the corpus reproducible and must not be reused for personal data.

Generated archives are binary test data and must not be edited manually.

## Reproducible Info-ZIP 3.0 sample

`infozip-3.0-deflate-unicode.zip` is generated from the official Info-ZIP Zip 3.0 source release. The source supplied for this fixture contains 362 files and matches the [SourceForge `zip30.zip` release](https://sourceforge.net/projects/infozip/files/Zip%203.x%20%28latest%29/3.0/zip30.zip/download) byte for byte after extraction. The downloaded source package has SHA-256 `7061ceac0407682b6dc54bb480347205f680f4e56cf34fe1423df2309f18968a`; the canonical extracted-tree digest recorded by the generator is `78d68057069fa76f05af685b339b299344a36da8ff42224f4ef082c73cb7f345`. Its license is the Info-ZIP license dated 2007-Mar-4.

The generator requires PowerShell 7, the frozen source tree, and a MinGW directory containing `gcc.exe`, `mingw32-make.exe` and `windres.exe`:

```powershell
pwsh -NoProfile -File scripts/generate-infozip-fixture.ps1 `
    -SourceDirectory '<official-zip30-source-directory>' `
    -ToolchainDirectory '<MinGW-bin-directory>'
```

The frozen fixture was built with MinGW GCC 13.1.0 targeting `x86_64-w64-mingw32` and GNU Make 4.4 on Windows 11 with the zh-CN OEM code page 936. The script copies the source to an isolated temporary directory, verifies the complete source-tree digest, disables the obsolete 32-bit assembly path with `-DNO_ASM`, and pre-includes `windows.h` to avoid the modern MinGW `CR` field collision. It does not patch or write to the supplied source directory. The generated `zip.exe` reports `This is Zip 3.0 (July 5th 2008)`; neither that executable nor the source tree is shipped with the plugin.

The producer command is `zip -X <output> .\ascii.txt .\目录\文件.txt` with both input timestamps normalized to `2024-01-02 03:04:06 +08:00`. Its non-ASCII standard name uses OEM code page 936 without ZIP bit 11, while Info-ZIP's `0x7075` Unicode Path extra field carries the canonical `目录/文件.txt` spelling. This specifically verifies that the production reader honors the Unicode extra field even when its legacy fallback charset is IBM437. The script checks the final 451-byte file against SHA-256 `cce11cb11fda466a73f547aa01b412253534210b4748d16e31be6ce5d8d72eef` and refuses to replace an existing fixture.

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

## Opt-in supplied observation corpus

The locally supplied `archive-test-2026` directory is not redistributed. `ExternalArchiveCompatibilitySampleTest` requires all 14 original files, pins every SHA-256 independently, and accounts for each file in exactly one behavior group. It covers ordinary Bandizip, WinRAR and MT Manager archives; encrypted WinRAR RAR5 and MT Manager 7Z; a complete two-volume WinRAR set; standalone MT Manager GZIP/XZ streams; and the historical ZIP regression. The production reader lists each supported archive and streams at least one complete entry. Encrypted samples exercise missing, wrong and correct passwords; the volume set reconstructs and streams an entry; the two standalone streams must remain unrecognized as TAR.

| Source group | Files | Automated behavior |
| --- | ---: | --- |
| Bandizip | 1 | ZIP list and verified entry stream |
| WinRAR | 6 | ZIP, RAR4, RAR5, encrypted RAR5 and complete two-volume RAR |
| MT Manager | 6 | ZIP, 7Z, encrypted 7Z, TAR and standalone GZIP/XZ rejection |
| Historical regression | 1 | Legacy Chinese filename recovery and verified ZIP entry stream |

The six Android samples were produced by MT Manager v2.26.8. That exact application version is now pinned, but the original UI selections were not embedded in the files, so they remain observation samples rather than reproducible committed fixtures.

Run this corpus together with the two external macOS samples:

```powershell
$env:ARCHIVE_EXTERNAL_CORPUS_DIRECTORY = 'E:\tmp\archive-test-2026'
$env:ARCHIVE_MAC_HELLO_DOLLY_SAMPLE = 'E:\Downloads\Firefox\hello-dolly.zip'
$env:ARCHIVE_MAC_ZIPKIREI_SAMPLE = 'T:\Downloads\Firefox\test.zip'
.\gradlew.bat :app:testDebugUnitTest `
    --tests ExternalArchiveCompatibilitySampleTest `
    --tests MacArchiveCompatibilitySampleTest
```

The supplied password is the corpus directory name and exists only for this local regression. Tests never execute payloads or modify the originals. Exact creation settings were not embedded in these files, so this observation corpus complements but does not replace reproducible manifest fixtures.

Windows Explorer coverage is reproducible through `scripts/generate-windows-explorer-fixture.ps1`, which invokes the real `Shell.Application` ZIP namespace and records the exact `zipfldr.dll` version. Info-ZIP coverage is reproducible through `scripts/generate-infozip-fixture.ps1`, including the official source identity, compiler target, locale, command and frozen output. All producer categories named by the Roadmap now have behavioral samples. The external macOS archives and MT Manager v2.26.8 corpus retain pinned identities and automated production-reader coverage, but still need exact original creation settings before they can be promoted to the committed reproducible corpus.

## Historical regressions

User-provided archives whose original producer and exact command are unknown are kept separately under `app/src/test/resources/regression-fixtures`. They are not counted as reproducible producer coverage. Each one is privacy-screened, renamed to a neutral fixture name, pinned by SHA-256 and exercised only as inert archive data. Archived scripts, registry files or other payloads are never executed by tests.
