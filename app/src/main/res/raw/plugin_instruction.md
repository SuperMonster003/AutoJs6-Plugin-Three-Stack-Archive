Use Archive Manager from the main file manager:

Archive Manager 2.22.0 requires Android 7 or newer and a paired AutoJs6 6.8.0 build (versionCode 5276 or newer) that advertises Explorer Action v21.

1. Install and enable the `Archive Manager` plugin.
2. Open the overflow menu for a supported archive file.
3. Select `Open archive`.
4. Browse folders, search, use the path bar, or open supported entries with the host previewers.
5. Use Back to move to the previous internal folder before returning to the ordinary file list.

To extract the entire archive immediately, select `Extract to...` from its file menu and choose the output folder.

To create an archive, select `Compress...` from an ordinary file or folder menu. You can also select multiple items in the same folder and use `Compress...` in the bottom action bar. Choose the file name, format, and compression level; ZIP and 7Z can also use an optional password.

The current build reads and extracts ZIP-family, ordinary or solid 7Z, RAR4/RAR5, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2, and TAR.ZST/TZST archives. It creates ordinary or standard split ZIP, non-solid 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, and TAR.ZST; optional passwords are available for ZIP and 7Z, and encrypted output filenames remain visible. Ordinary single-volume ZIP, unencrypted non-solid single-volume 7Z within the decoder budget, and eligible TAR-family archives support adding, renaming, and deleting content through a verified full rebuild. RAR, encrypted, solid, split, unsafe, unsupported-method, or over-budget 7Z variants and TAR special entries remain read-only. Complete standard `.z01 + .zip`, modern `partN.rar`, numbered `.zip.001`, and numbered `.7z.001` sets can be browsed and extracted through bounded host-approved sibling descriptors; every volume set remains read-only. Split ZIP creation offers preset or custom MiB sizes. Output larger than the selected size is split; smaller output remains one `.zip`.

After every physical output is verified and committed, an off-by-default option can move the complete source selection to the host Trash. Explorer Action v21 keeps bounded durable recovery history; restoring sources never removes the created archive.

The plugin requests no broad storage or network permission. Ordinary archives browse through independent positional channels over a host-provided read-only descriptor, without a whole-file copy; incompatible inputs or readers (currently including encrypted ZIP) fall back to private cache, which is cleaned on close and after expiry. Archive creation reads through a short-lived host session bound to the plugin and can write only a transactional output in the current parent folder. Path-traversal, source-size, and output-boundary checks remain enforced.
