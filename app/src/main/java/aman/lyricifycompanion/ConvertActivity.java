package aman.lyricifycompanion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ConvertActivity extends AppCompatActivity implements ConverterSheet.ConverterListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No setContentView(), we use the transparent theme
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            Uri videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (videoUri != null) {
                // Read the mode flag
                boolean isLyricifyMode = intent.getBooleanExtra("mode_lyricify", false);
                
                ConverterSheet sheet = ConverterSheet.newInstance(videoUri, isLyricifyMode);
                sheet.setCancelable(false); 
                sheet.show(getSupportFragmentManager(), "ConverterSheet");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onConversionSuccess(String pathOrUri) {
        Intent resultIntent = new Intent();
        Uri finalUri;

        // FIX: Check if it's already a Content URI before adding "file://"
        if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            finalUri = Uri.parse(pathOrUri);
        } else {
            // It's a raw path, convert to File URI
            finalUri = Uri.fromFile(new File(pathOrUri));
        }

        resultIntent.setData(finalUri);
        
        // Grant permissions to the calling app (Lyricify) to read this URI
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
    
    public void onConversionCancelled() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
