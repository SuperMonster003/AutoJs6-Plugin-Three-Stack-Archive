<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>Read-only ZIP-family archive browsing and selective SAF extraction for AutoJs6 Explorer</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### Introduction

******

The AutoJs6 Archive Browser Plugin adds read-only archive browsing to AutoJs6 Explorer. It opens ZIP-based containers in a dedicated hierarchy viewer and extracts only the entries selected by the user to an output folder selected through Android Storage Access Framework.

******

### Features

******

- Registers a single-file read-only Explorer overflow action through the shared `org.autojs.plugin.EXPLORER_ACTION` protocol.
- Browses archive folders and displays unpacked size, compressed size, CRC, and modification metadata.
- Searches normalized entry paths and supports selecting individual files, folders, or all visible entries.
- Extracts selected entries to a user-selected SAF tree with progress reporting and cancellation.
- Accepts ZIP, JAR, AAR, and WAR containers that use supported ZIP compression methods.
- Stages the read-only input in private cache and removes temporary data when the viewer closes.

******

### Supported Formats

******

Version 1 recognizes these ZIP-family filename extensions:

```text
zip, jar, aar, war
```

******

### Plugin Interface

******

AutoJs6 discovers and executes the plugin with the following identities:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

Version 1 is limited to a single-file read-only overflow action in the main AutoJs6 Explorer surface.

******

### Security

******

The plugin requests neither storage nor network permissions. The host grants temporary read-only access to the input content URI, while output access is limited to the SAF directory explicitly selected by the user. Absolute paths, parent traversal, drive prefixes, backslashes, unsafe Unicode, duplicate paths, file-directory conflicts, unsupported compression methods, size mismatches, and CRC mismatches are rejected.

******

### Safety Limits

******

- Maximum staged input size: `4 GiB`.
- Maximum archive path nodes, including implicit directories: `20,000`.
- Maximum ZIP central directory size: `64 MiB`.
- Maximum normalized path length: `1,024` characters.
- Maximum path depth: `64` segments.
- Maximum unpacked size for one entry: `512 MiB`.
- Maximum total unpacked size: `2 GiB`.
- Maximum compression ratio: `1000:1`.

******

### Release History

******

# v1.0.0

###### 2026/08/02

* `Feature` Archive Browser plugin with plugin ID `archive-browser`, engine `explorer-action`, and variant `default`
* `Feature` Single-file read-only Explorer action for ZIP, JAR, AAR, and WAR containers with hierarchical browsing, path search, and entry selection
* `Feature` Selective extraction to a user-selected SAF tree with progress and cancellation, temporary read-only input access, and no storage or network permission
* `Feature` Safety limits of 4 GiB input, 20,000 archive path nodes including implicit directories, 64 MiB ZIP central directory, 512 MiB per unpacked entry, 2 GiB total unpacked data, and a compression ratio of 1000:1
* `Feature` Validation for unsafe paths, duplicate and conflicting entries, unsupported compression methods, changed sources, size mismatches, and CRC mismatches
* `Feature` Localized metadata, interface text, usage instructions, README files, and changelogs in Spanish, French, Russian, Arabic, Japanese, Korean, English, Simplified Chinese, Hong Kong Traditional Chinese, and Taiwan Traditional Chinese

##### For more release history

* [CHANGELOG-en.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release build:

```powershell
.\gradlew.bat :app:assembleRelease
```

Build parameters come from `version.properties`. The current minimum SDK is 24 and the target SDK is 36.

******

### Resource Layout

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localizes plugin metadata and fixed browser text, while `plurals.xml` localizes quantity-aware browser text. `plugin_instruction.md` provides host-visible usage instructions. README and changelog files are generated from JSON sources by `.python/generate_markdown.py`.

******

### Links

******

- AutoJs6 documentation: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
