Use Archive Manager from the main file manager:

1. Install and enable the `Archive Manager` plugin.
2. Open the overflow menu for a supported archive file.
3. Select `Open archive`.
4. Browse folders, search, use the path bar, or open supported entries with the host previewers.
5. Use Back to move to the previous internal folder before returning to the ordinary file list.

To extract the entire archive immediately, select `Extract to...` from its file menu and choose the output folder.

To create an archive, select `Compress...` from an ordinary file or folder menu. You can also select multiple items in the same folder and use `Compress...` in the bottom action bar. Choose the file name, format, and compression level; ZIP and 7Z can also use an optional password.

The current build reads and extracts ZIP-family, ordinary or solid 7Z, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST archives. It creates ordinary or standard split ZIP, non-solid 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST; optional passwords are available for ZIP and 7Z, and encrypted output filenames remain visible. TAR links, device nodes, and sparse entries are list-only. Standard `.z01 + .zip` split ZIP files identify the required earlier volumes when the final volume is opened alone, and creation offers preset or custom MiB sizes. Output larger than the selected size is split; smaller output remains one `.zip`. Additional formats, split-volume reading, per-entry extraction, and in-archive editing remain in the project Roadmap.

The plugin requests no broad storage or network permission. Ordinary archives browse through independent positional channels over a host-provided read-only descriptor, without a whole-file copy; incompatible inputs or readers (currently including encrypted ZIP) fall back to private cache, which is cleaned on close and after expiry. Archive creation reads through a short-lived host session bound to the plugin and can write only a transactional output in the current parent folder. Path-traversal, source-size, and output-boundary checks remain enforced.
