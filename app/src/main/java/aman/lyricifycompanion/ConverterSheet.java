package aman.lyricifycompanion;

import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.File;

public class ConverterSheet extends BottomSheetDialogFragment {

    private String format = "webp";
    private Uri sourceUri;
    private TextView btnFormatAvif, btnFormatWebp;
    private ImageView preview;

    public static ConverterSheet newInstance(Uri uri) {
        ConverterSheet s = new ConverterSheet();
        s.sourceUri = uri;
        return s;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_converter, container, false);

        btnFormatAvif = v.findViewById(R.id.btnFormatAvif);
        btnFormatWebp = v.findViewById(R.id.btnFormatWebp);
        preview = v.findViewById(R.id.preview);

        btnFormatAvif.setOnClickListener(view -> setFormat("avif"));
        btnFormatWebp.setOnClickListener(view -> setFormat("webp"));

        setFormat("webp");

        v.findViewById(R.id.btnConvert).setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                
                String ext = format.equalsIgnoreCase("avif") ? ".avif" : ".webp";
                File input = new File(UriHelper.getPathFromUri(activity, sourceUri));
                String outName = input.getName().contains(".") ? 
                    input.getName().substring(0, input.getName().lastIndexOf('.')) + "_conv" + ext :
                    input.getName() + "_conv" + ext;
                
                File output = new File(activity.getExternalCacheDir(), outName);
                activity.startConversion(sourceUri, output.getAbsolutePath(), format);
                dismiss();
            }
        });

        loadPreview();
        return v;
    }

    private void setFormat(String f) {
        this.format = f;
        if (f.equals("avif")) {
            btnFormatAvif.setBackgroundResource(R.drawable.bg_pill_selected);
            btnFormatWebp.setBackgroundResource(R.drawable.bg_pill);
        } else {
            btnFormatWebp.setBackgroundResource(R.drawable.bg_pill_selected);
            btnFormatAvif.setBackgroundResource(R.drawable.bg_pill);
        }
    }

    private void loadPreview() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source src = ImageDecoder.createSource(getContext().getContentResolver(), sourceUri);
                Drawable d = ImageDecoder.decodeDrawable(src);
                preview.setImageDrawable(d);
                if (d instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) d).start();
                }
            } catch (Exception e) {
                preview.setImageResource(R.drawable.ic_image);
            }
        } else {
            preview.setImageURI(sourceUri);
        }
    }
}
