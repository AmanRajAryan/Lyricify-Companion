package aman.lyricifycompanion;



import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
                // Show the sheet
                ConverterSheet sheet = ConverterSheet.newInstance(videoUri , false);
                // Prevent user from cancelling by clicking outside (optional)
                sheet.setCancelable(false); 
                sheet.show(getSupportFragmentManager(), "ConverterSheet");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    
    public void onConversionSuccess(String path) {
        Intent resultIntent = new Intent();
        // Return the path string or URI
        resultIntent.setData(Uri.parse("file://" + path));
        
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    
    public void onConversionCancelled() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
