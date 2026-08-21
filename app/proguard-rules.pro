-keep class io.github.supermonster003.autojs6.plugin.archivemanager.ExplorerActionService { *; }
-keep class io.github.supermonster003.autojs6.plugin.archivemanager.ArchiveManagerApplication { *; }
-keep class io.github.supermonster003.autojs6.plugin.archivemanager.ArchiveManagerActivity { *; }
-keep class io.github.supermonster003.autojs6.plugin.archivemanager.WakeActivity { *; }
-keep class org.autojs.plugin.common.api.PluginInfo { *; }
-keep class org.autojs.plugin.explorer.api.** { *; }

# Commons Compress treats XZ and Zstandard as optional codecs. This build only
# enables codecs present on the runtime classpath; absent optional codecs are
# reported per entry through ZipFile.canReadEntryData instead of being invoked.
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
