# Product screenshots

These screenshots are direct Android screen captures of the signed, paired builds below:

- AutoJs6 6.8.0, version code 5276, Explorer Action v21, host commit `d7bc884d6749369a9045fc545d3a3f3a7710a55f`
- Archive Manager 2.22.0, version code 28
- Android 13 / API 33 x86_64 temporary AVD at 1080 x 2340 px
- System day mode, default font scale, and the AVD's active dynamic color scheme

Only the synthetic `Archive Manager Demo` fixture appears in the captures. The images were captured with Android `screencap` and were not cropped, composited, recolored, or generated. No user archive, device identifier, account, notification, or private path is present.

| File | What it demonstrates |
| --- | --- |
| `explorer-actions.png` | Archive Manager actions inside the existing AutoJs6 file menu. |
| `native-archive-browsing.png` | ZIP contents in the host list with the host path bar and archive-only actions. |
| `create-archive-form.png` | Destination, file name, conflict policy, format, compression level, password, and split-volume settings. |
| `creation-safety-options.png` | Format-aware disabled states, source-to-Trash opt-in, and output verification guidance. |
| `archive-management.png` | Detailed archive management with budgets, conflict handling, encoding, entry selection, and add actions. |
| `source-recovery.png` | A verified creation whose source was moved to AutoJs6 Trash and can be restored while the created archive remains. |

The source-recovery capture was followed by a real `Restore sources` action. The source returned without overwriting another name, and the newly created `Welcome.txt.zip` remained in place.
