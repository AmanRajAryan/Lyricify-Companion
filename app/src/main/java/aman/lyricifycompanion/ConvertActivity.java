package aman.lyricifycompanion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.File;

public class ConvertActivity extends Activity implements FFmpegListener {

    private String inputPath;
    private String outputPath;
    private FFmpegBinaryHandler ffmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ffmpeg = new FFmpegBinaryHandler();
        
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            inputPath = UriHelper.getPathFromUri(this, uri);
            
            if (inputPath == null) {
                Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String format = intent.getStringExtra("format");
            String res = intent.getStringExtra("res");
            String fps = intent.getStringExtra("fps");
            String quality = intent.getStringExtra("quality");

            File inputFile = new File(inputPath);
            String folder = inputFile.getParent();
            String name = inputFile.getName();
            String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String ext = (format != null && format.equalsIgnoreCase("avif")) ? ".avif" : ".webp";
            
            outputPath = new File(folder, baseName + "_converted" + ext).getAbsolutePath();

            String cmd = CommandBuilder.build(inputPath, outputPath, res, fps, quality, format);
            ffmpeg.execute(this, cmd, this);
        } else {
            finish();
        }
    }

    @Override
    public void onProgress(String message, double speed, int totalSeconds) {
    }

    @Override
    public void onSuccess() {
        onConversionSuccess(outputPath);
    }

    @Override
    public void onFailure(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    public void onConversionSuccess(String pathOrUri) {
        Intent resultIntent = new Intent();
        Uri finalUri;

        if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            finalUri = Uri.parse(pathOrUri);
        } else {
            finalUri = Uri.fromFile(new File(pathOrUri));
        }

        resultIntent.setData(finalUri);
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ffmpeg != null) ffmpeg.cancel();
    }
}
