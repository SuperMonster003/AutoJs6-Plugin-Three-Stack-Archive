# Third-party notices

3-Stack Archive continues the Archive Manager implementation; names in dated measurements describe the historical releases. The maintainer supplied the new artwork; its original files and Icon Studio recipe, when present, are retained.

This project includes third-party software in its Android application. The project license in [`LICENSE`](LICENSE) does not replace the licenses below.

## Apache Commons Compress 1.28.0

- Component: `org.apache.commons:commons-compress:1.28.0`
- Project: <https://commons.apache.org/proper/commons-compress/>
- Purpose here: ZIP directory metadata; seekable 7Z structure detection, listing, entry streams, common compression/filter pipelines, AES-256-SHA256 reading and content-encrypted writing; TAR structure detection, listing, header-checksum validation, and entry streams; and GZIP/BZIP2 stream decoding for TAR.GZ/TGZ and TAR.BZ2/TBZ2
- License: Apache License 2.0; the exact upstream [`LICENSE`](third_party/commons-compress/LICENSE) and [`NOTICE`](third_party/commons-compress/NOTICE) are retained in this repository
- Resolved runtime dependencies: Commons Codec 1.19.0, Commons IO 2.20.0, and Commons Lang 3.18.0
- Native code/ABI impact: none; these are Java libraries and add no native ABI

### Security review

Review date: 2026-08-22.

- Apache's security report lists the TAR parsing denial of service CVE-2023-42503 as affecting 1.22 before 1.24.0; 1.28.0 is outside that range.
- Apache's report lists CVE-2024-25710 and CVE-2024-26308 as fixed in 1.26.0; 1.28.0 includes those fixes.
- Commons Compress 1.28.0 resolves Commons Lang 3.18.0, whose release replaced the recursive `ClassUtils.getClass` path associated with CVE-2025-48924.
- Commons IO 2.20.0 is outside the before-2.14.0 range affected by CVE-2024-47554.
- Future upgrades must repeat the Apache security-report and NVD searches for Commons Compress and its resolved runtime dependencies, then rerun malformed 7Z/TAR, password, memory-limit, writer, and Android runtime tests.

Archive Manager instantiates `TarArchiveInputStream` directly after its own signature check. It validates visible header checksums, never follows TAR links, does not materialize device or sparse entries, and keeps path validation, source-identity checks, declared/actual size checks, output isolation, and cleanup outside the library.

Archive Manager also builds `SevenZFile` and `SevenZOutputFile` on seekable channels backed by host-provided file descriptors. The reader caps Commons Compress decoder memory at 262,144 KiB, maps absent and wrong passwords to stable diagnostics, and still applies the application's source-identity, path, declared/actual size, CRC, and output-isolation checks. The writer creates non-solid output, maps level 0 to Copy and levels 1 through 9 to LZMA2, and can add AES-256 content encryption. Commons Compress writes filenames in the unencrypted 7Z header, so Archive Manager keeps filename-encryption creation disabled and explains that encrypted output names remain visible. Split volumes and in-archive mutation are not exposed.

### Packaging impact

Commons Compress and its three runtime dependencies were already part of the application before the TAR and 7Z backends. TAR.BZ2 and 7Z support add no Maven artifact or native ABI. The resolved Commons Compress JAR is 1,117,221 bytes with SHA-256 `E1522945218456F3649A39BC4AFD70CE4BD466221519DBA7D378F2141A4642CA`; R8 can continue removing formats unused by the application. The only 7Z packaging increase comes from application code, localized resources, and the upstream license text that was already packaged, not from another dependency or ABI.

## Junrar 8.1.0

- Component: `com.github.junrar:junrar:8.1.0`; the production build packages a locally generated reader-only variant with the password-hygiene changes described below
- Project: <https://github.com/junrar/junrar>
- Purpose here: read-only structure detection, listing, preview, and extraction for single-volume RAR4/RAR5 archives, including RAR5 content and header encryption
- License: UnRAR license; the exact upstream [`LICENSE`](third_party/junrar/LICENSE) and the local modification [`NOTICE`](third_party/junrar/NOTICE) are retained in this repository and packaged with the application
- License restriction: the bundled or modified UnRAR-derived source may handle RAR archives but must not be used to develop a RAR (WinRAR) compatible archiver. Archive Manager does not register a RAR writer and does not create or modify RAR archives.
- Runtime support: the patched reader uses the already resolved Commons IO 2.20.0 plus SLF4J 2.0.17 API/NOP; it adds no native code or ABI

### Security and password-hygiene review

Review date: 2026-08-26.

- GitHub Advisory Database records path traversal advisory GHSA-j273-m5qq-6825 as affecting Junrar versions before 7.5.8. The selected 8.1.0 version is outside that affected range.
- Archive Manager does not call Junrar's filesystem extraction API. It supplies a host-descriptor-backed read-only seekable channel and keeps path validation, unsafe-name isolation, source identity, declared/actual size, available CRC, resource-budget, output-transaction, cancellation, and cleanup checks in the format-neutral application layer.
- Junrar's maximum RAR dictionary size is capped at 256 MiB. A larger request is rejected with a typed unsupported/resource diagnostic instead of attempting an unbounded allocation.
- The build replaces `ArchiveOptions` with a source-derived variant that clears builder password arrays after construction and retained option arrays on close. It also replaces `Rar5Crypt` with a source-derived variant that disables Junrar's JVM-global RAR5 key-derivation cache. Both replacements are guarded by exact upstream source matches so an upgrade fails closed until the patch is reviewed again.
- These changes reduce how long password and derived-key material remains reachable, but Java, Binder, and cryptographic implementations can still create short-lived copies that the application cannot reliably overwrite. The project therefore promises best-effort clearing, not absolute absence of runtime copies.
- Future upgrades must review the upstream release and advisory history, verify the original Maven artifact and source hashes, rebase both source-derived password patches, and rerun malformed input, RAR4/RAR5, encrypted header/content, split-volume, dictionary-limit, API 24, host-session, and real-device tests.

Split archives remain bounded by Explorer Action v11: the host grants only the selected volume descriptor. Archive Manager may display safe metadata from a first RAR volume, but marks its entries `MISSING_VOLUME` and exposes no preview or extraction action until a host-owned sibling-volume contract exists. Junrar is never allowed to discover sibling files by local path.

### Artifact measurement

The original Junrar JAR is 243,519 bytes with SHA-256 `53C23CC8A11C932B7336D8109257C14B1D8981B789B0792FC3F78AC567B645E7`. The build derives the production JAR from that artifact and the matching source artifact, replacing only `ArchiveOptions` and `Rar5Crypt` classes and removing signature metadata. This generated JAR contains no native library.

## SLF4J 2.0.17 API and NOP provider

- Components: `org.slf4j:slf4j-api:2.0.17` and `org.slf4j:slf4j-nop:2.0.17`
- Project: <https://www.slf4j.org/>
- Purpose here: satisfy Junrar's logging API with an intentionally silent provider; Archive Manager does not route Junrar messages into user or diagnostic logs
- License: MIT; the exact upstream [`LICENSE`](third_party/slf4j/LICENSE) is retained in this repository and packaged with the application
- Transitive dependencies: `slf4j-nop` resolves only the matching `slf4j-api`
- Native code/ABI impact: none; both components are Java libraries

The SLF4J API JAR is 69,908 bytes with SHA-256 `7B751D952061954D5ABFED7181C1F645D336091B679891591D63329C622EB832`. The NOP provider JAR is 4,982 bytes with SHA-256 `3716F83649EC66161A2EDEFD4F49DF34D1DD1C51CDCF941996C6987260F0A829`. Release R8 may remove unused logging surface, so dependency JAR sizes are not treated as packaged cost. The final signed and resource-shrunk Archive Manager 2.4.0 APK is 4,285,257 bytes, compared with the 4,212,527-byte 2.3.0 baseline: a net increase of 72,730 bytes, or approximately 1.73%. This delta also includes Explorer Action v11 application code, localized resources, documentation, and the RAR integration itself; it is not an isolated SLF4J or Junrar measurement. The APK retains the same four zstd-jni ABIs and adds no native library.

## XZ for Java 1.12

- Component: `org.tukaani:xz:1.12`
- Project: <https://tukaani.org/xz/java.html>
- Purpose here: streamed XZ encoding and decoding for TAR.XZ/TXZ archives through Commons Compress
- License: BSD Zero Clause License (0BSD); the exact upstream [`COPYING`](third_party/xz-java/COPYING) is retained in this repository and packaged with the application
- Transitive dependencies: none in `releaseRuntimeClasspath`
- Native code/ABI impact: none from XZ for Java; it is a pure Java library. The application now packages native libraries only for the separately documented Zstandard codec.

### Version and security review

Review date: 2026-08-23.

- Upstream released 1.12 on 2026-03-01 and identifies it as the current release.
- Upstream states that 1.12 fixes a significant bug present in 1.10 and 1.11. The version catalog therefore moves from 1.10 to 1.12 for both build logic and the application instead of introducing the older version into production.
- Upstream currently reports no known XZ for Java security issues and warns that scanners may incorrectly apply XZ Utils CVEs because the XZ Utils CPE resembles the Maven coordinate. Findings must be checked against the Java package and version rather than dismissed or accepted by name alone.
- XZ for Java 1.10 and newer use 0BSD. The main sources are Java 8 compatible, so 1.12 remains compatible with the application's Android toolchain.
- Future upgrades must review the upstream security and release pages, verify the Maven artifact hash, and rerun malformed-stream, external 7-Zip corpus, API 24 runtime, and host-session tests.

Archive Manager verifies both the six-byte XZ container signature and the decompressed TAR structure before indexing. It enables concatenated-stream decoding, consumes the container footer so XZ integrity checks run, and caps XZ decoder memory at 262,144 KiB so an archive header cannot request unbounded dictionary memory. Creation maps the selected XZ preset to the encoder and writes through the same host transaction as the other formats. TAR entry paths, types, declared and actual sizes, source identity, output isolation, and cleanup remain enforced by the format-neutral application layer.

### Artifact and APK measurement

The resolved XZ for Java JAR is 168,792 bytes with SHA-256 `3E158A87BD73D8AFB4B6E8239C013B7D049C48563F45860CE99CD2E448CF4A6B`, matching the checksum published by upstream. APKs were measured on Windows 11 with JDK 21, Android Gradle Plugin 9.2.1, Gradle 9.5.0, and the same local signing configuration. Baseline commit: `6ab8680`.

| Variant | Plain-TAR baseline | With TAR.GZ and TAR.XZ | Difference |
| --- | ---: | ---: | ---: |
| Debug APK | 10,769,982 bytes | 11,318,244 bytes | +548,262 bytes |
| R8/resource-shrunk Release APK | 1,826,951 bytes | 1,848,415 bytes | +21,464 bytes |

The difference was measured when only reading was enabled and covers that compressed-TAR phase: the XZ decoder, GZIP/XZ adapters, format registration, tests excluded from production, and packaged 0BSD text. Release is the distribution-relevant figure. The later writer reuses the same dependency, so it adds no Maven artifact or ABI; future size baselines should be remeasured from a release commit that includes both directions.

## zstd-jni 1.5.7-15

- Component: `com.github.luben:zstd-jni:1.5.7-15`
- Project: <https://github.com/luben/zstd-jni/tree/v1.5.7-15>
- Purpose here: streamed Zstandard encoding and decoding for TAR.ZST/TZST archives, including concatenated and skippable input frames and checksummed output frames
- Licenses: the Java/JNI bindings use BSD-2-Clause and the embedded Zstandard native library is dual-licensed under BSD-3-Clause or GPL-2.0; this project uses the BSD terms. The exact upstream binding [`LICENSE`](third_party/zstd-jni/LICENSE) and native-library [`LICENSE`](third_party/zstd-jni/LICENSE.zstd) are retained in this repository and packaged with the application.
- Resolved runtime dependencies: none in `releaseRuntimeClasspath`
- Native code/ABI impact: the Android AAR contributes `libzstd-jni-1.5.7-15.so` for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. `PluginInfo.supportedAbis` advertises the same complete set.

### Version and security review

Review date: 2026-08-23.

- Upstream published tag 1.5.7-15 on 2026-08-16. Its Android AAR is documented for Android 5.0 and newer; the application remains at `minSdk 24`.
- The embedded native Zstandard version is 1.5.7. GitHub Advisory Database records CVE-2022-4899 as affecting Zstandard versions before 1.5.4, so the selected native version is outside that affected range.
- Historical zstd-jni reports identified missing RELRO in 1.5.7-9 and an NDK r19 build that did not meet Android's 16 KiB page-size requirement. Both reports are closed. This project independently inspected every native library in 1.5.7-15: each was built with NDK r29, every `LOAD` segment has `p_align = 0x4000`, and every ELF contains `GNU_RELRO`.
- The AAR metadata requires `compileSdk 37`; only the compile SDK was raised. `targetSdk 36` and `minSdk 24` are unchanged.
- zstd-jni resolves JNI entry points through original Java class and member names. The Release shrinker therefore keeps `com.github.luben.zstd.**` names and members instead of relying on a build that happens to work without the upstream-required rule.
- Future upgrades must review upstream releases and advisories, verify Maven hashes, inspect every packaged ABI for NDK provenance, 16 KiB `LOAD` alignment and RELRO, run `zipalign -P 16` on final APKs, and rerun malformed-stream, external-corpus, API 24 runtime, and host-session tests.

Archive Manager verifies a standard or skippable Zstandard frame signature and the decompressed TAR structure before indexing. It enables continuous-frame decoding, consumes the stream footer so checksum and truncation errors surface, and caps the permitted frame window at `2^28` bytes (256 MiB). Creation maps the selected level to `ZstdOutputStream` and enables a frame checksum before the TAR stream is closed and transactionally committed. TAR entry paths, types, declared and actual sizes, source identity, output isolation, and cleanup remain enforced by the format-neutral application layer. A magic-only or otherwise undersized frame is reported as a recognized but malformed archive rather than an unknown format.

### Artifact, ABI, and APK measurement

The Android AAR is 947,264 bytes with SHA-256 `CD722A3E928E610D57184BBC8F48A055067C308DBF6C7BACAAC999F9366E5264`. The desktop-native test JAR is 6,451,268 bytes with SHA-256 `88AF05C50951C2C8D0FE10C7B3451736204FCC389EAAD01E767E7ACC64E75FE2`; it is confined to JVM tests and is not packaged in Android builds. Both downloads match their Maven Central SHA-1 sidecars.

| AAR ABI | Native library size | ELF `LOAD` alignment | RELRO | NDK |
| --- | ---: | ---: | --- | --- |
| `arm64-v8a` | 476,288 bytes | `0x4000` | `GNU_RELRO` | r29 (14206865) |
| `armeabi-v7a` | 363,260 bytes | `0x4000` | `GNU_RELRO` | r29 (14206865) |
| `x86` | 552,248 bytes | `0x4000` | `GNU_RELRO` | r29 (14206865) |
| `x86_64` | 547,360 bytes | `0x4000` | `GNU_RELRO` | r29 (14206865) |

APK sizes were measured on Windows 11 with JDK 21, Android Gradle Plugin 9.2.1, Gradle 9.5.0, and the same local signing configuration. Baseline commit: `4e990a8`. Both resulting APKs pass Android Build Tools 37.0.0 `zipalign -v -c -P 16 4`, and inspection of the libraries extracted from the Release APK reproduces the four ABI results above.

| Variant | TAR.GZ/TAR.XZ baseline | With TAR.BZ2 and TAR.ZST | Difference |
| --- | ---: | ---: | ---: |
| Debug APK | 9,618,862 bytes | 11,645,934 bytes | +2,027,072 bytes |
| R8/resource-shrunk Release APK | 1,848,543 bytes | 3,863,367 bytes | +2,014,824 bytes |

The difference covers this complete phase: the four native Zstandard ABIs, BZIP2/Zstandard adapters, format registration, R8 JNI rules, tests excluded from production, and packaged BSD license texts. The Release increase is mostly the four compressed native libraries; the desktop-native test JAR does not contribute to it.

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
