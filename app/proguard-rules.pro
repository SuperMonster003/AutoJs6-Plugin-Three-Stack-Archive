-keep class io.github.supermonster003.autojs6.plugin.three.stack.archive.ExplorerActionService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.stack.archive.ThreeStackArchiveApplication { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveManagerActivity { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.stack.archive.WakeActivity { *; }
-keep class org.autojs.plugin.common.api.PluginInfo { *; }
-keep class org.autojs.plugin.explorer.api.** { *; }

# zstd-jni resolves JNI entry points by their original Java class and member names.
-keep class com.github.luben.zstd.** { *; }

# Commons Compress treats XZ and Zstandard as optional codecs. Both codecs are
# present in this build; these rules also cover optional ZIP methods that R8
# analyzes without proving their runtime class path.
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
