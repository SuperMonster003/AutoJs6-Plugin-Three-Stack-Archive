# Third-party notices

This project includes third-party software in its Android application. The project license in [`LICENSE`](LICENSE) does not replace the licenses below.

## Apache Commons Compress 1.28.0

- Component: `org.apache.commons:commons-compress:1.28.0`
- Project: <https://commons.apache.org/proper/commons-compress/>
- Purpose here: ZIP directory metadata and uncompressed TAR structure detection, listing, header-checksum validation, and entry streams
- License: Apache License 2.0; the exact upstream [`LICENSE`](third_party/commons-compress/LICENSE) and [`NOTICE`](third_party/commons-compress/NOTICE) are retained in this repository
- Resolved runtime dependencies: Commons Codec 1.19.0, Commons IO 2.20.0, and Commons Lang 3.18.0
- Native code/ABI impact: none; these are Java libraries and add no native ABI

### Security review

Review date: 2026-08-22.

- Apache's security report lists the TAR parsing denial of service CVE-2023-42503 as affecting 1.22 before 1.24.0; 1.28.0 is outside that range.
- Apache's report lists CVE-2024-25710 and CVE-2024-26308 as fixed in 1.26.0; 1.28.0 includes those fixes.
- Commons Compress 1.28.0 resolves Commons Lang 3.18.0, whose release replaced the recursive `ClassUtils.getClass` path associated with CVE-2025-48924.
- Commons IO 2.20.0 is outside the before-2.14.0 range affected by CVE-2024-47554.
- Future upgrades must repeat the Apache security-report and NVD searches for Commons Compress and its resolved runtime dependencies, then rerun malformed TAR and Android runtime tests.

Archive Manager instantiates `TarArchiveInputStream` directly after its own signature check. It validates visible header checksums, never follows TAR links, does not materialize device or sparse entries, and keeps path validation, source-identity checks, declared/actual size checks, output isolation, and cleanup outside the library.

### Packaging impact

Commons Compress and its three runtime dependencies were already part of the application before the TAR backend. This phase adds no Maven artifact or native ABI. The resolved Commons Compress JAR is 1,117,221 bytes with SHA-256 `E1522945218456F3649A39BC4AFD70CE4BD466221519DBA7D378F2141A4642CA`; R8 can continue removing formats unused by the application.

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
