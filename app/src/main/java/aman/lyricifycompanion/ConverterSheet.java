package aman.lyricifycompanion;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ConverterSheet extends BottomSheetDialogFragment {

    public interface ConverterListener {
        void onConversionSuccess(String path);
    }

    private Uri inputUri;
    private File finalTempFile;
    private ConverterListener listener;
    private long startTime;
    private boolean calledFromLyricify = false; 

    private TextView tvStatus, tvTitle, tvOriginalInfo, tvAvifInfo, btnTopSave;
    private ImageView btnBack, imgAvifPreview;
    private LinearLayout layoutControls, layoutPreview;
    private VideoView videoViewOriginal;
    private ProgressBar progressBar;
    private MaterialButton btnConvert;
    private MaterialCheckBox cbAutoConfirm;
    
    private AutoCompleteTextView inputRes, inputQuality, inputFps, inputSpeed;

    public static ConverterSheet newInstance(Uri uri, boolean calledFromLyricify) {
        ConverterSheet f = new ConverterSheet();
        Bundle args = new Bundle();
        args.putParcelable("uri", uri);
        args.putBoolean("mode_lyricify", calledFromLyricify);
        f.setArguments(args);
        return f;
    }

    @Override
    public int getTheme() { return R.style.RoundedBottomSheetTheme; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog d = (BottomSheetDialog) getDialog();
        if (d != null) {
            if (d.getWindow() != null) d.getWindow().setDimAmount(0f);

            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setHideable(true);

                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            cleanupAndFinish();
                        }
                    }
                    @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
                });
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ConverterListener) listener = (ConverterListener) context;
        
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutPreview != null && layoutPreview.getVisibility() == View.VISIBLE) {
                    showControlsUi();
                } else {
                    setEnabled(false); 
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_converter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            inputUri = getArguments().getParcelable("uri");
            calledFromLyricify = getArguments().getBoolean("mode_lyricify", false);
        }

        initViews(view);
        
        videoViewOriginal.setFocusable(false);
        videoViewOriginal.setFocusableInTouchMode(false);

        setupInputs();
        setupModeUI();

        btnConvert.setOnClickListener(v -> startConversion());
        btnTopSave.setOnClickListener(v -> saveAndDismiss());
        
        View.OnClickListener backAction = v -> showControlsUi();
        btnBack.setOnClickListener(backAction);
        tvTitle.setOnClickListener(backAction);
        
        SharedPreferences prefs = requireContext().getSharedPreferences("lyricify_prefs", Context.MODE_PRIVATE);
        cbAutoConfirm.setChecked(prefs.getBoolean("auto_confirm", true));
        cbAutoConfirm.setOnCheckedChangeListener((b, isChecked) -> 
            prefs.edit().putBoolean("auto_confirm", isChecked).apply());
    }

    private void cleanupAndFinish() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().finish();
            getActivity().overridePendingTransition(0, 0); 
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        cleanupAndFinish();
    }

    private void initViews(View v) {
        tvStatus = v.findViewById(R.id.tvStatus);
        tvTitle = v.findViewById(R.id.tvTitle);
        btnBack = v.findViewById(R.id.btnBack);
        btnTopSave = v.findViewById(R.id.btnTopSave);
        progressBar = v.findViewById(R.id.progressBar);
        btnConvert = v.findViewById(R.id.btnConvert);
        cbAutoConfirm = v.findViewById(R.id.cbAutoConfirm);
        layoutControls = v.findViewById(R.id.layoutControls);
        layoutPreview = v.findViewById(R.id.layoutPreview);
        tvOriginalInfo = v.findViewById(R.id.tvOriginalInfo);
        tvAvifInfo = v.findViewById(R.id.tvAvifInfo);
        videoViewOriginal = v.findViewById(R.id.videoViewOriginal);
        imgAvifPreview = v.findViewById(R.id.imgAvifPreview);
        inputRes = v.findViewById(R.id.inputResolution);
        inputQuality = v.findViewById(R.id.inputQuality);
        inputFps = v.findViewById(R.id.inputFps);
        inputSpeed = v.findViewById(R.id.inputSpeed);
    }

    private void setupModeUI() {
        if (calledFromLyricify) {
            btnTopSave.setText("CONFIRM");
            cbAutoConfirm.setText("Confirm automatically");
        } else {
            btnTopSave.setText("SAVE");
            cbAutoConfirm.setText("Save automatically");
        }
    }

    private void setupInputs() {
        String originalRes = "Original (" + MediaUtils.getResolution(getContext(), inputUri) + ")";
        setupDropdown(inputRes, new String[]{originalRes, "720p", "480p", "360p"}, originalRes);
        setupDropdown(inputQuality, new String[]{"High (Slow)", "Medium", "Low (Fast)"}, "Medium");
        setupDropdown(inputFps, new String[]{"60", "30", "24", "20", "15"}, "20");
        setupDropdown(inputSpeed, new String[]{"Ultrafast", "Balanced", "Best Quality"}, "Ultrafast");
    }

    private void setupDropdown(AutoCompleteTextView v, String[] opts, String def) {
        // FIX: Use your custom 'spinner_item' layout
        v.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.spinner_item, opts));
        v.setText(def, false);
        v.setOnClickListener(view -> v.showDropDown());
    }

    private void startConversion() {
        final String sRes = inputRes.getText().toString();
        final String sFps = inputFps.getText().toString();
        final String sQuality = inputQuality.getText().toString();
        final String sSpeed = inputSpeed.getText().toString();

        setUiState(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Preparing...");
        startTime = System.currentTimeMillis();

        new Thread(() -> {
            String realInputPath = MediaUtils.copyUriToTempFile(getContext(), inputUri);
            if (realInputPath == null) return;
            
            File outputDir = requireContext().getCacheDir();
            String outputPath = new File(outputDir, "temp_output.avif").getAbsolutePath();

            String cmd = CommandBuilder.build(realInputPath, outputPath, sRes, sFps, sQuality, sSpeed);

            new FFmpegBinaryHandler().execute(requireContext(), cmd, new FFmpegBinaryHandler.FFmpegListener() {
                @Override
                public void onProgress(String msg, double speed, int time) {
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    double elapsedSecs = elapsedMillis / 1000.0;
                    requireActivity().runOnUiThread(() -> 
                        tvStatus.setText(String.format(Locale.US, "Speed: %.2fx | Time: %ds in %.1fs", speed, time, elapsedSecs)));
                }

                @Override
                public void onSuccess() {
                    new File(realInputPath).delete();
                    finalTempFile = new File(outputPath);
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (cbAutoConfirm.isChecked()) {
                            saveAndDismiss();
                        } else {
                            showPreviewUi();
                        }
                    });
                }

                @Override
                public void onFailure(String err) {
                    new File(realInputPath).delete();
                    requireActivity().runOnUiThread(() -> {
                        tvStatus.setText("Error: " + err);
                        setUiState(true);
                    });
                }
            });
        }).start();
    }

    private void showPreviewUi() {
        layoutControls.setVisibility(View.GONE);
        btnConvert.setVisibility(View.GONE);
        cbAutoConfirm.setVisibility(View.GONE);
        
        layoutPreview.setVisibility(View.VISIBLE);
        btnTopSave.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        tvTitle.setText("Edit");

        long orgSize = 0;
        try { orgSize = requireContext().getContentResolver().openInputStream(inputUri).available(); } catch(Exception e){}
        tvOriginalInfo.setText("Original : " + MediaUtils.getResolution(getContext(), inputUri) + " , " + MediaUtils.formatFileSize(orgSize));
        tvAvifInfo.setText("AVIF : " + MediaUtils.formatFileSize(finalTempFile.length()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            videoViewOriginal.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        }
        
        videoViewOriginal.setVideoURI(inputUri);
        videoViewOriginal.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0f, 0f); 
            videoViewOriginal.start();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(finalTempFile);
                Drawable drawable = ImageDecoder.decodeDrawable(source);
                imgAvifPreview.setImageDrawable(drawable);
                if (drawable instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) drawable).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } else {
            imgAvifPreview.setImageURI(Uri.fromFile(finalTempFile));
        }
    }

    private void showControlsUi() {
        layoutPreview.setVisibility(View.GONE);
        btnTopSave.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        
        layoutControls.setVisibility(View.VISIBLE);
        btnConvert.setVisibility(View.VISIBLE);
        cbAutoConfirm.setVisibility(View.VISIBLE);
        
        tvTitle.setText("Convert Video");
        tvStatus.setText("Ready");
        setUiState(true);
        videoViewOriginal.stopPlayback();
    }

    private void saveAndDismiss() {
        if (finalTempFile == null || !finalTempFile.exists()) return;

        if (calledFromLyricify) {
            try {
                // Securely share file using FileProvider (Requires provider_paths.xml setup)
                Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "aman.lyricifycompanion.fileprovider",
                    finalTempFile
                );

                if (listener != null) {
                    listener.onConversionSuccess(contentUri.toString());
                }
                dismiss();
            } catch (Exception e) {
                // Fallback if provider not set up, or on error
                Toast.makeText(getContext(), "FileProvider Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Uri savedUri = MediaUtils.saveToDownloads(getContext(), finalTempFile);
            if (savedUri != null) {
                Toast.makeText(getContext(), "Saved to Downloads", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onConversionSuccess(savedUri.toString());
            } else {
                Toast.makeText(getContext(), "Save Failed", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }
    }

    private void setUiState(boolean enabled) {
        btnConvert.setEnabled(enabled);
        inputRes.setEnabled(enabled);
        inputFps.setEnabled(enabled);
    }
}
