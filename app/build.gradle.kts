import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val codeNamespace = "io.github.supermonster003.autojs6.plugin.archivemanager"

val buildTypeDebug = "debug"
val buildTypeRelease = "release"
val junrarVersion = "8.1.0"

val originalCommonsCompress by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val originalCommonsCompressSources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val commonsCompressPatchClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val originalJunrar by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val originalJunrarSources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val junrarPatchClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    originalCommonsCompress(libs.apache.commons.compress)
    originalCommonsCompressSources("org.apache.commons:commons-compress:1.28.0:sources")
    commonsCompressPatchClasspath(libs.apache.commons.compress)
    originalJunrar("com.github.junrar:junrar:$junrarVersion")
    originalJunrarSources("com.github.junrar:junrar:$junrarVersion:sources")
    junrarPatchClasspath("com.github.junrar:junrar:$junrarVersion")
}

val generatedSevenZFileSource = layout.buildDirectory.file(
    "generated/sources/commons-compress-android-patch/" +
        "org/apache/commons/compress/archivers/sevenz/SevenZFile.java",
)

val generateCommonsCompressAndroidPatch by tasks.registering {
    description = "Patches Commons Compress 7Z entry ordering and solid detection"
    inputs.files(originalCommonsCompressSources)
    outputs.file(generatedSevenZFileSource)

    doLast {
        val sourcePath = "org/apache/commons/compress/archivers/sevenz/SevenZFile.java"
        val sourceFile = zipTree(originalCommonsCompressSources.singleFile)
            .matching { include(sourcePath) }
            .singleFile
        val streamBasedAssembly =
            "archive.files = fileMap.values().stream().filter(Objects::nonNull)" +
                ".toArray(SevenZArchiveEntry[]::new);"
        val stableAssembly = """
            final List<SevenZArchiveEntry> entries = new ArrayList<>();
            for (final SevenZArchiveEntry entry : fileMap.values()) {
                if (entry != null) {
                    entries.add(entry);
                }
            }
            archive.files = entries.toArray(SevenZArchiveEntry.EMPTY_SEVEN_Z_ARCHIVE_ENTRY_ARRAY);
        """.trimIndent()
        val entriesAccessor = """
            public Iterable<SevenZArchiveEntry> getEntries() {
                return new ArrayList<>(Arrays.asList(archive.files));
            }
        """.trimIndent().prependIndent("    ")
        val entriesAccessorWithSolidDetection = """
            public Iterable<SevenZArchiveEntry> getEntries() {
                return new ArrayList<>(Arrays.asList(archive.files));
            }

            /**
             * Returns whether at least two non-empty files share one 7z folder.
             *
             * <p>This is the format-level definition needed by Archive Manager before it offers
             * a full rewrite. Commons Compress exposes random entry streams but doesn't otherwise
             * expose the folder map through its public API.</p>
             *
             * @return whether this archive contains a solid compression block
             */
            public boolean hasSolidCompression() {
                if (archive.streamMap == null || archive.folders.length == 0) {
                    return false;
                }
                final int[] nonEmptyFilesPerFolder = new int[archive.folders.length];
                for (int i = 0; i < archive.files.length; i++) {
                    if (!archive.files[i].hasStream()) {
                        continue;
                    }
                    final int folderIndex = archive.streamMap.fileFolderIndex[i];
                    if (folderIndex >= 0 && ++nonEmptyFilesPerFolder[folderIndex] > 1) {
                        return true;
                    }
                }
                return false;
            }
        """.trimIndent().prependIndent("    ")
        val source = sourceFile.readText(Charsets.UTF_8)
        check(source.indexOf(streamBasedAssembly) >= 0) {
            "Commons Compress SevenZFile entry assembly no longer matches the Android patch"
        }
        check(source.indexOf(streamBasedAssembly) == source.lastIndexOf(streamBasedAssembly)) {
            "Commons Compress SevenZFile entry assembly matched more than once"
        }
        check(source.indexOf(entriesAccessor) >= 0) {
            "Commons Compress SevenZFile entries accessor no longer matches the solid patch"
        }
        check(source.indexOf(entriesAccessor) == source.lastIndexOf(entriesAccessor)) {
            "Commons Compress SevenZFile entries accessor matched more than once"
        }
        val output = generatedSevenZFileSource.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            source
                .replace(streamBasedAssembly, stableAssembly)
                .replace(entriesAccessor, entriesAccessorWithSolidDetection),
            Charsets.UTF_8,
        )
    }
}

val compileCommonsCompressAndroidPatch by tasks.registering(JavaCompile::class) {
    dependsOn(generateCommonsCompressAndroidPatch)
    source(generatedSevenZFileSource)
    classpath = commonsCompressPatchClasspath
    destinationDirectory = layout.buildDirectory.dir("intermediates/commons-compress-android-patch/classes")
    options.release = 8
    options.encoding = "UTF-8"
}

val patchedCommonsCompressJar by tasks.registering(Jar::class) {
    dependsOn(compileCommonsCompressAndroidPatch)
    archiveFileName = "commons-compress-1.28.0-android-patched.jar"
    destinationDirectory = layout.buildDirectory.dir("intermediates/commons-compress-android-patch")
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from({ zipTree(originalCommonsCompress.singleFile) }) {
        exclude("org/apache/commons/compress/archivers/sevenz/SevenZFile*.class")
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }
    from(compileCommonsCompressAndroidPatch.flatMap(JavaCompile::getDestinationDirectory))
}

val patchedCommonsCompress = files(patchedCommonsCompressJar.flatMap(Jar::getArchiveFile)).apply {
    builtBy(patchedCommonsCompressJar)
}

val generatedArchiveOptionsSource = layout.buildDirectory.file(
    "generated/sources/junrar-password-hygiene-patch/com/github/junrar/ArchiveOptions.java",
)

val generatedRar5CryptSource = layout.buildDirectory.file(
    "generated/sources/junrar-password-hygiene-patch/com/github/junrar/crypt/Rar5Crypt.java",
)

val generateJunrarPasswordHygienePatch by tasks.registering {
    description = "Adds explicit password wiping to Junrar construction options"
    inputs.files(originalJunrarSources)
    outputs.files(generatedArchiveOptionsSource, generatedRar5CryptSource)

    doLast {
        val sourcePath = "com/github/junrar/ArchiveOptions.java"
        val sourceFile = zipTree(originalJunrarSources.singleFile)
            .matching { include(sourcePath) }
            .singleFile
        val classDeclaration = "public final class ArchiveOptions {"
        val wipeableClassDeclaration =
            "public final class ArchiveOptions implements AutoCloseable {"
        val passwordAccessor = """
            public char[] getPassword() {
                return this.password == null ? null : this.password.clone();
            }
        """.trimIndent().prependIndent("    ")
        val wipeablePasswordAccessor = """
            public char[] getPassword() {
                return this.password == null ? null : this.password.clone();
            }

            /**
             * Local reader-only modification: clears the retained password copy.
             * This code may not be used to develop a RAR (WinRAR) compatible archiver.
             */
            @Override
            public void close() {
                if (this.password != null) {
                    java.util.Arrays.fill(this.password, '\0');
                }
            }
        """.trimIndent().prependIndent("    ")
        val buildMethod = """
                    public ArchiveOptions build() {
                        if (this.maxDictionarySize <= 0) {
                            throw new IllegalArgumentException(
                                    "maxDictionarySize must be > 0, was " + this.maxDictionarySize);
                        }
                        return new ArchiveOptions(this);
                    }
        """.trimIndent().prependIndent("        ")
        val wipeableBuildMethod = """
                    public ArchiveOptions build() {
                        if (this.maxDictionarySize <= 0) {
                            throw new IllegalArgumentException(
                                    "maxDictionarySize must be > 0, was " + this.maxDictionarySize);
                        }
                        try {
                            return new ArchiveOptions(this);
                        } finally {
                            if (this.password != null) {
                                java.util.Arrays.fill(this.password, '\0');
                                this.password = null;
                            }
                        }
                    }
        """.trimIndent().prependIndent("        ")
        val source = sourceFile.readText(Charsets.UTF_8)
        listOf(classDeclaration, passwordAccessor, buildMethod).forEach { expected ->
            check(source.indexOf(expected) >= 0) {
                "Junrar ArchiveOptions source no longer matches the password hygiene patch"
            }
            check(source.indexOf(expected) == source.lastIndexOf(expected)) {
                "Junrar ArchiveOptions password hygiene patch matched more than once"
            }
        }
        val output = generatedArchiveOptionsSource.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            source
                .replace(classDeclaration, wipeableClassDeclaration)
                .replace(passwordAccessor, wipeablePasswordAccessor)
                .replace(buildMethod, wipeableBuildMethod),
            Charsets.UTF_8,
        )

        val cryptSourcePath = "com/github/junrar/crypt/Rar5Crypt.java"
        val cryptSourceFile = zipTree(originalJunrarSources.singleFile)
            .matching { include(cryptSourcePath) }
            .singleFile
        val cacheFields = """
            private static final CacheItem[] CACHE = new CacheItem[4];
            private static int cachePos = 0;
        """.trimIndent().prependIndent("    ")
        val cacheLookup = """
            for (final CacheItem item : CACHE) {
                if (item != null
                        && item.lg2Count == lg2Count
                        && Arrays.equals(item.pwd, pwdUtf8)
                        && Arrays.equals(item.salt, salt16)) {
                    return item.kdf;
                }
            }
        """.trimIndent().prependIndent("        ")
        val cacheStore = """
            CACHE[cachePos++ % CACHE.length] =
                    new CacheItem(pwdUtf8.clone(), salt16.clone(), lg2Count, kdf);
        """.trimIndent().prependIndent("        ")
        val cacheDocumentation =
            "(unrar {@code SetKey50}, {@code d861246:crypt5.cpp:131-190}). Result-cached across calls."
        val localDocumentation =
            "(unrar {@code SetKey50}, {@code d861246:crypt5.cpp:131-190}). " +
                "This reader-only build deliberately does not retain a JVM-global password cache."
        val cryptSource = cryptSourceFile.readText(Charsets.UTF_8)
        listOf(cacheFields, cacheLookup, cacheStore, cacheDocumentation).forEach { expected ->
            check(cryptSource.indexOf(expected) >= 0) {
                "Junrar Rar5Crypt source no longer matches the password hygiene patch"
            }
            check(cryptSource.indexOf(expected) == cryptSource.lastIndexOf(expected)) {
                "Junrar Rar5Crypt password hygiene patch matched more than once"
            }
        }
        val cryptOutput = generatedRar5CryptSource.get().asFile
        cryptOutput.parentFile.mkdirs()
        cryptOutput.writeText(
            cryptSource
                .replace(
                    cacheFields,
                    "    // Local reader-only build: do not retain password-derived material globally.",
                )
                .replace(cacheLookup, "")
                .replace(cacheStore, "")
                .replace(cacheDocumentation, localDocumentation),
            Charsets.UTF_8,
        )
    }
}

val compileJunrarPasswordHygienePatch by tasks.registering(JavaCompile::class) {
    dependsOn(generateJunrarPasswordHygienePatch)
    source(generatedArchiveOptionsSource, generatedRar5CryptSource)
    classpath = junrarPatchClasspath
    destinationDirectory = layout.buildDirectory.dir(
        "intermediates/junrar-password-hygiene-patch/classes",
    )
    options.release = 8
    options.encoding = "UTF-8"
}

val patchedJunrarJar by tasks.registering(Jar::class) {
    dependsOn(compileJunrarPasswordHygienePatch)
    archiveFileName = "junrar-$junrarVersion-password-hygiene-patched.jar"
    destinationDirectory = layout.buildDirectory.dir("intermediates/junrar-password-hygiene-patch")
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from({ zipTree(originalJunrar.singleFile) }) {
        exclude("com/github/junrar/ArchiveOptions*.class")
        exclude("com/github/junrar/crypt/Rar5Crypt*.class")
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }
    from(compileJunrarPasswordHygienePatch.flatMap(JavaCompile::getDestinationDirectory))
}

val patchedJunrar = files(patchedJunrarJar.flatMap(Jar::getArchiveFile)).apply {
    builtBy(patchedJunrarJar)
}

android {
    namespace = codeNamespace
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        ndk.abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        applicationId = codeNamespace
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_requires_host_version", "5276")
        resValue(
            "string",
            "plugin_runtime_component",
            "$codeNamespace/$codeNamespace.ExplorerActionService",
        )
        resValue(
            "string",
            "plugin_supported_abis",
            "arm64-v8a,armeabi-v7a,x86,x86_64",
        )
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        aidl = true
        resValues = true
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
        assets.directories += "$rootDir/third_party"
    }

    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
        abi.enableSplit = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val architecture = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    val abiSuffix = architecture?.let { "-$it" }.orEmpty()
                    "${rootProject.name}-v$version$abiSuffix.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    val zstdJniVersion = libs.versions.zstd.jni.get()

    coreLibraryDesugaring(libs.desugar)

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:2.2.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation(files("$rootDir/libs/common-plugin-api.aar"))
    implementation(files("$rootDir/libs/explorer-action-api.aar"))

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation(patchedCommonsCompress)
    implementation("commons-codec:commons-codec:1.19.0")
    implementation("commons-io:commons-io:2.20.0")
    implementation(libs.commons.lang3)
    implementation(libs.tukaani.xz)
    implementation("com.github.luben:zstd-jni:$zstdJniVersion@aar")
    implementation(libs.zip4j)
    implementation(patchedJunrar)
    implementation("org.slf4j:slf4j-nop:2.0.17")

    testImplementation(libs.junit)
    // The Android AAR contains device libraries only. JVM tests need the desktop-native JAR.
    testImplementation("com.github.luben:zstd-jni:$zstdJniVersion")
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }


}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

apply(from = rootProject.file("gradle/release-archive.gradle"))
