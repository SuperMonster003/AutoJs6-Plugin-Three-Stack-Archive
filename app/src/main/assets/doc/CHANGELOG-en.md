******

### Release History

******

# v1.0.1

###### 2026/08/08

* `Fix` Null service binding when enabling the plugin in Plugin Center
* `Improvement` Clearer and more concise plugin name, description, and user documentation

# v1.0.0

###### 2026/08/02

* `Feature` Archive Browser plugin with plugin ID `archive-browser`, engine `explorer-action`, and variant `default`
* `Feature` Single-file read-only Explorer action for ZIP, JAR, AAR, and WAR containers with hierarchical browsing, path search, and entry selection
* `Feature` Selective extraction to a user-selected SAF tree with progress and cancellation, temporary read-only input access, and no storage or network permission
* `Feature` Safety limits of 4 GiB input, 20,000 archive path nodes including implicit directories, 64 MiB ZIP central directory, 512 MiB per unpacked entry, 2 GiB total unpacked data, and a compression ratio of 1000:1
* `Feature` Validation for unsafe paths, duplicate and conflicting entries, unsupported compression methods, changed sources, size mismatches, and CRC mismatches
* `Feature` Localized metadata, interface text, usage instructions, README files, and changelogs in Spanish, French, Russian, Arabic, Japanese, Korean, English, Simplified Chinese, Hong Kong Traditional Chinese, and Taiwan Traditional Chinese
