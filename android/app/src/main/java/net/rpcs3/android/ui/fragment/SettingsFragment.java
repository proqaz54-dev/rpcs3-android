package net.rpcs3.android.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import net.rpcs3.android.R;
import net.rpcs3.android.RPCS3;

import java.io.File;

public class SettingsFragment extends Fragment {
    public static final String PREFS_SETTINGS = "rpcs3_settings";
    public static final String KEY_RESOLUTION_SCALE = "resolution_scale";
    public static final String KEY_ASPECT_RATIO = "aspect_ratio";
    public static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    public static final String KEY_OVERLAY_OPACITY = "overlay_opacity";
    public static final String KEY_HAPTIC_ENABLED = "haptic_enabled";

    private SharedPreferences mPrefs;

    private TextView mTxtResolutionTitle;
    private Slider mSliderResolution;
    private RadioGroup mRgAspectRatio;
    private RadioButton mRb169;
    private RadioButton mRb43;

    private MaterialSwitch mSwitchOverlay;
    private TextView mTxtOpacityTitle;
    private Slider mSliderOpacity;
    private MaterialSwitch mSwitchHaptic;

    private MaterialButton mBtnClearCache;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        mPrefs = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

        mTxtResolutionTitle = root.findViewById(R.id.txt_resolution_title);
        mSliderResolution = root.findViewById(R.id.slider_resolution);
        mRgAspectRatio = root.findViewById(R.id.rg_aspect_ratio);
        mRb169 = root.findViewById(R.id.rb_aspect_16_9);
        mRb43 = root.findViewById(R.id.rb_aspect_4_3);

        mSwitchOverlay = root.findViewById(R.id.switch_overlay);
        mTxtOpacityTitle = root.findViewById(R.id.txt_opacity_title);
        mSliderOpacity = root.findViewById(R.id.slider_opacity);
        mSwitchHaptic = root.findViewById(R.id.switch_haptic);

        mBtnClearCache = root.findViewById(R.id.btn_clear_cache);

        loadSettings();
        setupListeners();
        return root;
    }

    private void loadSettings() {
        int resScale = mPrefs.getInt(KEY_RESOLUTION_SCALE, 100);
        mSliderResolution.setValue(resScale);
        updateResolutionLabel(resScale);

        String aspect = mPrefs.getString(KEY_ASPECT_RATIO, "16:9");
        if ("4:3".equals(aspect)) {
            mRb43.setChecked(true);
        } else {
            mRb169.setChecked(true);
        }

        boolean overlayEnabled = mPrefs.getBoolean(KEY_OVERLAY_ENABLED, true);
        mSwitchOverlay.setChecked(overlayEnabled);

        int opacity = mPrefs.getInt(KEY_OVERLAY_OPACITY, 70);
        mSliderOpacity.setValue(opacity);
        mTxtOpacityTitle.setText("Controls Opacity: " + opacity + "%");

        boolean haptic = mPrefs.getBoolean(KEY_HAPTIC_ENABLED, true);
        mSwitchHaptic.setChecked(haptic);
    }

    private void setupListeners() {
        mSliderResolution.addOnChangeListener((slider, value, fromUser) -> {
            int val = (int) value;
            mPrefs.edit().putInt(KEY_RESOLUTION_SCALE, val).apply();
            updateResolutionLabel(val);
        });

        mRgAspectRatio.setOnCheckedChangeListener((group, checkedId) -> {
            String aspect = checkedId == R.id.rb_aspect_4_3 ? "4:3" : "16:9";
            mPrefs.edit().putString(KEY_ASPECT_RATIO, aspect).apply();
        });

        mSwitchOverlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_OVERLAY_ENABLED, isChecked).apply();
        });

        mSliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            int val = (int) value;
            mPrefs.edit().putInt(KEY_OVERLAY_OPACITY, val).apply();
            mTxtOpacityTitle.setText("Controls Opacity: " + val + "%");
        });

        mSwitchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_HAPTIC_ENABLED, isChecked).apply();
        });

        mBtnClearCache.setOnClickListener(v -> clearShaderCache());
    }

    private void updateResolutionLabel(int val) {
        String resolutionDesc;
        if (val <= 50) resolutionDesc = "360p (Low Performance)";
        else if (val <= 75) resolutionDesc = "540p (Balanced)";
        else if (val <= 100) resolutionDesc = "720p HD (Native PS3)";
        else if (val <= 150) resolutionDesc = "1080p Full HD";
        else resolutionDesc = "1440p 2K (High Resolution)";

        mTxtResolutionTitle.setText("Resolution Scale: " + val + "% (" + resolutionDesc + ")");
    }

    private void clearShaderCache() {
        String cacheDir = RPCS3.getCacheDirectory();
        if (cacheDir != null && !cacheDir.isEmpty()) {
            File cDir = new File(cacheDir);
            if (cDir.exists()) {
                deleteDir(cDir);
                cDir.mkdirs();
                Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(requireContext(), "Cache is already empty", Toast.LENGTH_SHORT).show();
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDir(f);
                }
            }
        }
        dir.delete();
    }
}
