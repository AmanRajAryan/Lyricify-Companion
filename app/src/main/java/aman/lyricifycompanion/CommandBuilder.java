package aman.lyricifycompanion;

import java.util.Locale;

public class CommandBuilder {

    public static String build(String inputPath, String outputPath, 
                             String res, String fps, String quality, String speed) {
        
        String preset = getSvtPreset(speed);
        String crf = getCrfValue(quality);
        String filter = buildFilterChain(res, fps);

        // Added "-an" flag to remove audio streams
        return String.format(Locale.US,
            "-i %s -c:v libsvtav1 -preset %s -crf %s -vf %s -pix_fmt yuv420p -an -y %s",
            inputPath, preset, crf, filter, outputPath
        );
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

    private static String getSvtPreset(String selection) {
        if (selection == null) return "12";
        if (selection.equals("Ultrafast")) return "13";
        if (selection.equals("Balanced")) return "10";
        return "6";
    }
}
