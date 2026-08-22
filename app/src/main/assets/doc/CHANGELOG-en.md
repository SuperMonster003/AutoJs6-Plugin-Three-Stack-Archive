# Release notes

## Unreleased

_Unreleased_

- `Added` Renamed the product to Archive Manager and the file action to Open archive
- `Added` Explorer Action v5 browses archives in the native AutoJs6 file list with the existing path bar, theme, and Back navigation
- `Added` Explorer Action v6 opens supported archive entries with the host document, image, audio, and video viewers
- `Added` Extract to... shortcut for choosing a destination and extracting the entire archive
- `Added` Explorer Action v4 adds Compress... to ordinary file and folder menus and to the five-action same-parent multi-selection bar
- `Added` ZIP creation with default naming, compression levels, progress, cancellation, and automatic conflict numbering
- `Added` Browse and extract ZipCrypto/AES encrypted ZIP files, retry a wrong password in place, and optionally create AES-256 ZIP files whose names remain visible with matching password confirmation
- `Fixed` ZIP listings now use directory metadata and handle self-extracting-style preambles, legacy filename encodings, Windows separators, and more readable ZIP methods
- `Fixed` ZIP filename encoding can be overridden when automatic detection is wrong, and extraction reuses the selected encoding
- `Fixed` Unknown or imprecise sizes, valid DocumentsProvider URIs, and extra host write grants no longer reject a valid archive before parsing
- `Fixed` ZIP browsing and extraction now work on Android 7.x without calling runtime APIs that only exist on newer systems
- `Fixed` Wrong passwords now map consistently to PASSWORD/WRONG_PASSWORD, and AES v2 entries with a zero stored CRC are no longer misreported as damaged
- `Improved` Removed the fixed 4 GiB input cap and browse-time extraction-size/ratio gates while retaining path containment, integrity checks, and failure cleanup
- `Improved` Added a checkable Roadmap and rewrote README and CHANGELOG to separate current behavior from planned work
- `Improved` The transitional standalone screen now follows system day/night mode and Material dynamic colors
- `Improved` ZIP output streams through a UID-bound host session to same-directory temporary output and commits atomically without storage permission or overwriting existing files
- `Improved` Archive format and entry capabilities are checked consistently across preview, extraction, and creation, so unavailable options stay disabled
- `Improved` Archive failures identify the format, processing stage, stable code, and reason; debug builds can copy complete diagnostics
- `Dependency` Added Apache License 2.0 licensed Zip4j 2.11.5 for encrypted ZIP streams, AES-256 creation, and the Android 7.x compatibility path

## v1.0.1

_2026/08/08_

- `Fixed` Empty service binding when enabling the plugin in Plugin Center
- `Improved` Simplified the plugin name, description, and usage text

## v1.0.0

_2026/08/02_

- `Added` Initial release for browsing ZIP, JAR, AAR, and WAR files and extracting selected files or folders
- `Added` Added search, selection, progress, cancellation, temporary-input cleanup, and localized UI
