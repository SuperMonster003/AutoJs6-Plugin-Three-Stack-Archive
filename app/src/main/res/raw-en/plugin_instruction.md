Use Archive Manager from the main file manager:

1. Install and enable the `Archive Manager` plugin.
2. Open the overflow menu for one ZIP, JAR, AAR, WAR, or TAR file.
3. Select `Open archive`.
4. Browse folders or search entry paths, then select files or folders.
5. Select `Extract selected` and choose an output folder with the Android system picker.

To extract the entire archive immediately, select `Extract to...` from its file menu and choose the output folder.

To create a ZIP, select `Compress...` from an ordinary file or folder menu. You can also select multiple items in the same folder and use `Compress...` in the bottom action bar. Confirm the file name and compression level, then create the archive in the current folder.

The current build reads and extracts ZIP-family archives and uncompressed TAR files, and creates ZIP files with an optional password. TAR links, device nodes, and sparse entries are list-only. Compressed TAR variants, split volumes, and in-archive editing remain in the project Roadmap.

The plugin requests no broad storage or network permission. Browsing stages the input in private cache according to available space. ZIP creation reads through a short-lived host session bound to the plugin and can write only a transactional output in the current parent folder. Path-traversal and output-boundary checks remain enforced.
