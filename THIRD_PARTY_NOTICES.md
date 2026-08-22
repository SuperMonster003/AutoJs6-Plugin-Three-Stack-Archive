# Third-party notices

This project includes third-party software in its Android application. The project license in [`LICENSE`](LICENSE) does not replace the licenses below.

## Apache Commons Compress 1.28.0

- Component: `org.apache.commons:commons-compress:1.28.0`
- Project: <https://commons.apache.org/proper/commons-compress/>
- Purpose here: ZIP directory metadata; TAR structure detection, listing, header-checksum validation, and entry streams; and GZIP stream decoding for TAR.GZ/TGZ
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

## XZ for Java 1.12

- Component: `org.tukaani:xz:1.12`
- Project: <https://tukaani.org/xz/java.html>
- Purpose here: streamed XZ decoding for TAR.XZ/TXZ archives through Commons Compress
- License: BSD Zero Clause License (0BSD); the exact upstream [`COPYING`](third_party/xz-java/COPYING) is retained in this repository and packaged with the application
- Transitive dependencies: none in `releaseRuntimeClasspath`
- Native code/ABI impact: none; XZ for Java is a pure Java library and the application still packages no native libraries

### Version and security review

Review date: 2026-08-23.

- Upstream released 1.12 on 2026-03-01 and identifies it as the current release.
- Upstream states that 1.12 fixes a significant bug present in 1.10 and 1.11. The version catalog therefore moves from 1.10 to 1.12 for both build logic and the application instead of introducing the older version into production.
- Upstream currently reports no known XZ for Java security issues and warns that scanners may incorrectly apply XZ Utils CVEs because the XZ Utils CPE resembles the Maven coordinate. Findings must be checked against the Java package and version rather than dismissed or accepted by name alone.
- XZ for Java 1.10 and newer use 0BSD. The main sources are Java 8 compatible, so 1.12 remains compatible with the application's Android toolchain.
- Future upgrades must review the upstream security and release pages, verify the Maven artifact hash, and rerun malformed-stream, external 7-Zip corpus, API 24 runtime, and host-session tests.

Archive Manager verifies both the six-byte XZ container signature and the decompressed TAR structure before indexing. It enables concatenated-stream decoding, consumes the container footer so XZ integrity checks run, and caps XZ decoder memory at 262,144 KiB so an archive header cannot request unbounded dictionary memory. TAR entry paths, types, declared and actual sizes, source identity, output isolation, and cleanup remain enforced by the format-neutral application layer.

### Artifact and APK measurement

The resolved XZ for Java JAR is 168,792 bytes with SHA-256 `3E158A87BD73D8AFB4B6E8239C013B7D049C48563F45860CE99CD2E448CF4A6B`, matching the checksum published by upstream. APKs were measured on Windows 11 with JDK 21, Android Gradle Plugin 9.2.1, Gradle 9.5.0, and the same local signing configuration. Baseline commit: `6ab8680`.

| Variant | Plain-TAR baseline | With TAR.GZ and TAR.XZ | Difference |
| --- | ---: | ---: | ---: |
| Debug APK | 10,769,982 bytes | 11,318,244 bytes | +548,262 bytes |
| R8/resource-shrunk Release APK | 1,826,951 bytes | 1,848,415 bytes | +21,464 bytes |

The difference covers the complete compressed-TAR phase: the XZ decoder, GZIP/XZ adapters, format registration, tests excluded from production, and packaged 0BSD text. Release is the distribution-relevant figure because R8 removes unused encoder and platform-specific code.

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
