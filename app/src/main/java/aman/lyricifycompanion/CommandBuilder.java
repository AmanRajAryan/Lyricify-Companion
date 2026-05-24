package aman.lyricifycompanion;

import java.util.Locale;

public class CommandBuilder {

    public static String build(String inputPath, String outputPath, 
                             String res, String fps, String quality, 
                             String format) {
        
        if (format != null && format.equalsIgnoreCase("avif")) {
            String filter = buildFilterChain(res, fps);
            String crf = getCrfValue(quality);
            return String.format(Locale.US, 
                "-i \"%s\" -vf \"%s\" -c:v libsvtav1 -crf %s -preset 6 -y \"%s\"",
                inputPath, filter, crf, outputPath);
        } else {
            String filter = buildFilterChain(res, fps);
            String q = getWebpQuality(quality);
            String comp = getWebpCompression(quality);
            return String.format(Locale.US, 
                "-i \"%s\" -vf \"%s\" -c:v libwebp -lossless 0 -q:v %s -compression_level %s -loop 0 -y \"%s\"",
                inputPath, filter, q, comp, outputPath);
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

    private static String getCrfValue(String selection) {
        if (selection == null) return "35";
        if (selection.startsWith("High")) return "25";
        if (selection.startsWith("Low")) return "45";
        return "35";
    }

    private static String getWebpQuality(String selection) {
        if (selection == null) return "50";
        if (selection.startsWith("High")) return "75";
        if (selection.startsWith("Low")) return "30";
        return "50";
    }

    private static String getWebpCompression(String selection) {
        if (selection == null) return "4";
        if (selection.equals("Ultrafast")) return "0";
        if (selection.equals("Balanced")) return "3";
        return "6";
    }
}
