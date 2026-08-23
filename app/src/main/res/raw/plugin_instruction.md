Use Archive Manager from the main file manager:

1. Install and enable the `Archive Manager` plugin.
2. Open the overflow menu for a supported archive file.
3. Select `Open archive`.
4. Browse folders or search entry paths, then select files or folders.
5. Select `Extract selected` and choose an output folder with the Android system picker.

To extract the entire archive immediately, select `Extract to...` from its file menu and choose the output folder.

To create an archive, select `Compress...` from an ordinary file or folder menu. You can also select multiple items in the same folder and use `Compress...` in the bottom action bar. Choose the file name, format, and compression level; ZIP can also use an optional password.

The current build reads and extracts ZIP-family, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST archives. It creates ZIP, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST; optional passwords are available for ZIP only. TAR links, device nodes, and sparse entries are list-only. Additional formats, split volumes, and in-archive editing remain in the project Roadmap.

The plugin requests no broad storage or network permission. Browsing stages the input in private cache according to available space. Archive creation reads through a short-lived host session bound to the plugin and can write only a transactional output in the current parent folder. Path-traversal, source-size, and output-boundary checks remain enforced.
