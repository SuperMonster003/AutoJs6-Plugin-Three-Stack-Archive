3-Stack Archive adds archive support to the AutoJs6 file manager: browse ZIP, 7Z, RAR and TAR family archives like folders, extract all or part of them, and compress files and folders into archives. The plugin's home screen can also open archives for browsing and extraction on its own.

### Getting started

1. Enable 3-Stack Archive in "Plugin Center".
2. In the AutoJs6 file manager, open the menu of an archive and choose "View archive"; its contents appear in the same file list.
3. Open folders, search, or tap an entry to open it in an AutoJs6 previewer; Back first returns to the parent folder inside the archive.
4. To get files out, choose "Extract to..." from the archive's menu to extract all of it, or long-press entries to select them and extract only those.

### Main features

- **Browse and preview**: Browse ZIP, JAR, 7Z, RAR and TAR family archives in the AutoJs6 file list, search them and jump with the path bar; documents, images, audio and video open in a previewer without extracting first.
- **Open as archive**: When a file name is not recognized, choose "Open as archive..." from the file menu; the format is detected from the content and shown in the path bar.
- **Extraction**: Extract the whole archive, the current folder (the button at the right of the path bar) or selected entries, with progress and cancel; files with the same name can be skipped, overwritten or numbered automatically.
- **Passwords and name encoding**: Encrypted ZIP, 7Z and RAR archives ask for a password when opened or extracted, and a wrong password can be retried at once; garbled ZIP file names are fixed by switching the encoding in the path bar.
- **Split archives**: Complete `.z01` + `.zip`, `partN.rar`, `.zip.001` and `.7z.001` volume sets can be browsed, previewed and extracted, but not modified.
- **Compression**: Choose "Compress..." for a file, a folder or several items selected in one folder to create ZIP, 7Z, TAR, TAR.GZ and other archives; ZIP and 7Z can take a password, and ZIP can be split by size.
- **One archive per item**: With several items selected, turn on "Compress each item separately" to create one archive for each item; name conflicts are numbered automatically.
- **Editing archives**: Plain single-volume ZIP, unencrypted non-solid 7Z and TAR family archives accept added files and folders, new empty folders, renamed and deleted entries; after a change, "Restore previous version" undoes it once.
- **Moving sources to Trash**: With "Move source items to Trash after compression" turned on (off by default), the sources go to AutoJs6 Trash only after every output is verified; "Source recovery history" in the menu of the compression page restores them.
- **Home screen**: Open the plugin from the launcher and tap "Open an archive" to browse and extract archives; "Settings" adjusts the language, dark mode, theme color and launcher icon.

### Using it in scripts

The plugin tools `archive_list`, `archive_read` and `archive_extract` list the contents of an archive, read a text file inside it and extract it from a script:

```js
let list = plugins.tools.call('archive_list', { archive: './demo.zip' });
list.result.entries.forEach(e => console.log(e.path, e.size));

let readme = plugins.tools.call('archive_read', { archive: './demo.zip', path: 'README.md' });
console.log(readme.result.text);

let extracted = plugins.tools.call('archive_extract', { archive: './demo.zip' }, {
    outputs: { files: './out/demo' },
});
console.log(extracted.result.filesExtracted, extracted.result.kept);
```

- Relative paths are resolved against the AutoJs6 working directory; the target folder of an extraction may already exist, and its existing files stay as they are and are counted in `kept`.
- `archive_read` returns UTF-8 text, at most 64 KiB per call; a binary file returns no text and `binary` is `true`.
- Pass the password of an encrypted archive in `password`; the plugin tools do not handle split archives.

### Requirements

- Android 7.0 or later.
- AutoJs6 6.8.0 or later, with this plugin enabled in "Plugin Center"; the plugin's home screen also works without AutoJs6.
- The plugin requests no storage or network permission and only handles files you choose in AutoJs6 or in the system file picker.

### FAQ

**When the plugin's actions are missing from the menu**

Make sure the plugin is enabled in "Plugin Center". Some AutoJs6 versions place plugin actions in a submenu named after the plugin, "3-Stack Archive"; for an archive whose name is not recognized, use "Open as archive...".

**Why an archive can only be viewed, not modified**

RAR, split archives, JAR/AAR/WAR, and encrypted or solid 7Z archives can only be browsed and extracted. "Manage archive..." shows the format of the current archive and why it is read-only.

**File names in encrypted archives**

ZIP and 7Z encryption protects the file contents, while the file names inside the archive stay visible, so keep sensitive information out of file names.

**When a split archive does not open**

Open a standard split ZIP from its final `.zip`, a WinRAR set from its first `partN.rar` and a numbered set from its `.001` file; all volumes must be in the same folder.

All parameters and results of the plugin tools are described in [AutoJs6 docs > Plugins](https://docs.autojs6.com/#/plugins); more information is on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive).
