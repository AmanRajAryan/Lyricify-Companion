package aman.lyricifycompanion;

import java.util.Locale;

public class CommandBuilder {

    public static String build(String inputPath, String outputPath, 
                             String res, String fps, String quality, String speed, String format) {
        
        String filter = buildFilterChain(res, fps);

        if ("webp".equals(format)) {
            // WEBP Logic
            String compression = getWebpCompression(speed);
            String q = getWebpQuality(quality);
            
            // -c:v libwebp -lossless 0 -q:v [0-100] -compression_level [0-6] -loop 0 -an -vsync 0
            return String.format(Locale.US,
                "-i %s -c:v libwebp -lossless 0 -q:v %s -compression_level %s -loop 0 -vf %s -an -vsync 0 -y %s",
                inputPath, q, compression, filter, outputPath
            );

        } else {
            // AVIF Logic (libsvtav1)
            String preset = getSvtPreset(speed);
            String crf = getCrfValue(quality);

            // -c:v libsvtav1 -preset [0-13] -crf [0-63] -pix_fmt yuv420p
            return String.format(Locale.US,
                "-i %s -c:v libsvtav1 -preset %s -crf %s -vf %s -pix_fmt yuv420p -an -y %s",
                inputPath, preset, crf, filter, outputPath
            );
        }
    }

    private static String buildFilterChain(String res, String fps) {
        if (res == null || res.startsWith("Original")) {
            return "fps=" + fps;
        }
        String scale = "scale=-2:-2";
        switch (res) {
            case "720p": scale = "scale=-2:720"; break;
            case "480p": scale = "scale=-2:480"; break;
            case "360p": scale = "scale=-2:360"; break;
        }
        return "fps=" + fps + "," + scale;
    }

    // --- AVIF Helpers ---
    private static String getCrfValue(String selection) {
        if (selection == null) return "35";
        if (selection.startsWith("High")) return "25";
        if (selection.startsWith("Low")) return "45";
        return "35";
    }

    private static String getSvtPreset(String selection) {
        if (selection == null) return "12";
        if (selection.equals("Ultrafast")) return "13";
        if (selection.equals("Balanced")) return "10";
        return "6";
    }

    // --- WebP Helpers ---
    private static String getWebpQuality(String selection) {
        // Maps to -q:v (0-100)
        if (selection == null) return "50";
        if (selection.startsWith("High")) return "75";
        if (selection.startsWith("Low")) return "30";
        return "50";
    }

    private static String getWebpCompression(String selection) {
        // Maps to -compression_level (0-6). 0=fastest, 6=best size
        if (selection == null) return "4";
        if (selection.equals("Ultrafast")) return "0";
        if (selection.equals("Balanced")) return "3";
        return "6"; // Best Quality
    }
}
