package aman.lyricifycompanion;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MediaUtils {

    public static String getResolution(Context context, Uri uri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            retriever.release();
            if (w != null && h != null) return w + "x" + h;
        } catch (Exception e) { e.printStackTrace(); }
        return "Unknown";
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static String copyUriToTempFile(Context context, Uri uri) {
        try {
            File tempFile = new File(context.getCacheDir(), "temp_input_" + System.currentTimeMillis() + ".mp4");
            InputStream in = context.getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            in.close();
            return tempFile.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    public static Uri saveToDownloads(Context context, File file) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Lyricify_" + System.currentTimeMillis() + ".avif");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/avif");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/LyricifyConverted");

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = resolver.openOutputStream(uri);
                 FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                out.flush();
                return uri;
            } catch (Exception e) { resolver.delete(uri, null, null); }
        }
        return null;
    }
}
