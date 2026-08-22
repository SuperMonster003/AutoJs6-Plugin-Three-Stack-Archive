# Third-party notices

This project includes third-party software in its Android application. The project license in [`LICENSE`](LICENSE) does not replace the licenses below.

## Zip4j 2.11.5

- Component: `net.lingala.zip4j:zip4j:2.11.5`
- Project: <https://github.com/srikanth-lingala/zip4j>
- Purpose here: ZIP AES/ZipCrypto input streams, encrypted ZIP metadata on Android 7.x, and AES-256 ZIP output streams
- License: Apache License 2.0; the exact upstream [`LICENSE`](third_party/zip4j/LICENSE) and [`NOTICE`](third_party/zip4j/NOTICE) are retained in this repository
- Transitive dependencies: none in `debugRuntimeClasspath`
- Native code/ABI impact: none; Zip4j is a Java library and adds no native ABI

### Security review

Review date: 2026-08-22.

- NVD CVE-2022-24615 affects Zip4j versions earlier than 2.10.0.
- NVD CVE-2023-22899 affects Zip4j through 2.11.2.
- The selected 2.11.5 version is outside both recorded affected ranges.
- Future dependency updates must repeat the NVD and GitHub Advisory Database search for the Maven coordinate, review upstream release notes, and rerun the encrypted compatibility corpus.

The application does not call Zip4j's filesystem extraction methods. Entry data stays behind Archive Manager's existing path validation, output isolation, source-identity, size, and checksum checks.

### APK measurement

Measured on Windows 11 with JDK 21, Android Gradle Plugin 9.2.1, Gradle 9.5.0, and the same local signing configuration. Baseline commit: `7ca4410`.

| Variant | Baseline | With this encrypted-ZIP phase | Difference |
| --- | ---: | ---: | ---: |
| Debug APK | 9,399,958 bytes | 9,523,129 bytes | +123,171 bytes |
| R8/resource-shrunk Release APK | 1,746,619 bytes | 1,803,734 bytes | +57,115 bytes |

The measured difference covers the complete encrypted-ZIP phase, including adapter code, password UI, localized resources, Zip4j, and the packaged upstream license and notice. The source Zip4j JAR is 210,027 bytes. Release is the distribution-relevant figure because R8 removes unused library code.
