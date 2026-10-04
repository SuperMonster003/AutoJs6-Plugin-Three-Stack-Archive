package io.github.supermonster003.autojs6.plugin.three.stack.archive;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Test APK only. Its separate process must not depend on the target app's Kotlin runtime. */
public final class FixtureArchiveProvider extends ContentProvider {
    private File fixture;

    @Override public boolean onCreate() {
        fixture = new File(getContext().getCacheDir(), "standalone-fixture.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(fixture))) {
            zip.putNextEntry(new ZipEntry("hello.txt"));
            zip.write("Standalone archive fixture".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            return true;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to prepare synthetic ZIP", error);
        }
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"/sample.zip".equals(uri.getPath()) || !"r".equals(mode)) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(fixture, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) { return "application/zip"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor result = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        result.addRow(new Object[]{"sample.zip", fixture.length()});
        return result;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
