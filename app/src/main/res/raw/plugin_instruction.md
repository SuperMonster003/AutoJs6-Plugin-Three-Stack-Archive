Use Archive Browser from the main file manager:

1. Install and enable the `Archive Browser` plugin.
2. Open the overflow menu for one ZIP, JAR, AAR, or WAR file.
3. Select `Browse archive`.
4. Browse folders or search entry paths, then select files or folders.
5. Select `Extract selected` and choose an output folder with the Android system picker.

The plugin receives temporary read-only access to the input content URI. It requests no storage or network permission, and it writes only to the output tree that you select through the Storage Access Framework.

The input is staged in private cache and is limited to 4 GiB. Each archive is limited to 20,000 entries, 512 MiB for one unpacked entry, 2 GiB total unpacked data, and a compression ratio of 1000:1. Unsafe paths and unsupported compression methods are blocked.
