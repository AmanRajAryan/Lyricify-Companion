package aman.lyricifycompanion;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegBinaryHandler {

    public interface FFmpegListener {
        void onProgress(String message, double speed, int time);
        void onSuccess();
        void onFailure(String error);
    }

    private Process process;
    private boolean isRunning = false;

    public void execute(Context context, String cmd, FFmpegListener listener) {
        new Thread(() -> {
            try {
                // 1. Find the extracted native library
                String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
                File ffmpegBin = new File(nativeLibDir, "libffmpeg.so");

                if (!ffmpegBin.exists()) {
                    listener.onFailure("Binary not found! Check jniLibs folder and extractNativeLibs=true.");
                    return;
                }

                // FIX: Ensure it is executable
                if (!ffmpegBin.canExecute()) {
                    if (!ffmpegBin.setExecutable(true)) {
                        Log.w("FFmpeg", "Failed to set executable permission");
                    }
                }

                // 2. Prepare Command
                List<String> command = new ArrayList<>();
                command.add(ffmpegBin.getAbsolutePath());
                
                String[] args = cmd.split(" "); 
                for (String arg : args) {
                    if (!arg.trim().isEmpty()) command.add(arg);
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                process = pb.start();
                isRunning = true;

                // 3. Parse Output
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                
                Pattern speedPattern = Pattern.compile("speed=\\s*(\\d+\\.?\\d*)x");
                Pattern timePattern = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})");

                StringBuilder errorLog = new StringBuilder(); // Capture logs in case of error

                while ((line = reader.readLine()) != null) {
                    if (!isRunning) break;
                    
                    // Keep last few lines for error reporting
                    if (errorLog.length() > 500) errorLog.delete(0, 100); 
                    errorLog.append(line).append("\n");

                    Matcher speedMatcher = speedPattern.matcher(line);
                    Matcher timeMatcher = timePattern.matcher(line);

                    double speed = 0;
                    int totalSeconds = 0;

                    if (speedMatcher.find()) {
                        try { speed = Double.parseDouble(speedMatcher.group(1)); } catch (Exception e) {}
                    }
                    if (timeMatcher.find()) {
                        try {
                            int h = Integer.parseInt(timeMatcher.group(1));
                            int m = Integer.parseInt(timeMatcher.group(2));
                            int s = Integer.parseInt(timeMatcher.group(3));
                            totalSeconds = h * 3600 + m * 60 + s;
                        } catch (Exception e) {}
                    }

                    if (speed > 0 || line.contains("frame=")) {
                        listener.onProgress(line, speed, totalSeconds);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    listener.onSuccess();
                } else {
                    // Send the captured logs as the error message so you know WHY it failed
                    listener.onFailure("Exit Code " + exitCode + ": " + errorLog.toString());
                }

            } catch (Exception e) {
                listener.onFailure("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public void cancel() {
        isRunning = false;
        if (process != null) process.destroy();
    }
}
