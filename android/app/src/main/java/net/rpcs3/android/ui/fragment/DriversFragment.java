package net.rpcs3.android.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.rpcs3.android.R;
import net.rpcs3.android.RPCS3;
import net.rpcs3.android.model.GpuDriver;
import net.rpcs3.android.repository.DriverRepository;
import net.rpcs3.android.ui.adapter.DriverAdapter;

import java.io.InputStream;
import java.util.List;

public class DriversFragment extends Fragment implements DriverAdapter.OnDriverClickListener {
    private TextView mTxtGpuStatusTitle;
    private TextView mTxtGpuStatusDesc;
    private MaterialButton mBtnInstallDriver;
    private RecyclerView mRecyclerDrivers;

    private DriverAdapter mAdapter;
    private DriverRepository mRepository;

    private ActivityResultLauncher<Intent> mZipPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_drivers, container, false);

        mTxtGpuStatusTitle = root.findViewById(R.id.txt_gpu_status_title);
        mTxtGpuStatusDesc = root.findViewById(R.id.txt_gpu_status_desc);
        mBtnInstallDriver = root.findViewById(R.id.btn_install_driver);
        mRecyclerDrivers = root.findViewById(R.id.recycler_drivers);

        mRepository = DriverRepository.getInstance(requireContext());
        mAdapter = new DriverAdapter(this);

        mRecyclerDrivers.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerDrivers.setAdapter(mAdapter);

        setupGpuStatus();

        mZipPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            installDriverFromUri(uri);
                        }
                    }
                }
        );

        mBtnInstallDriver.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            mZipPickerLauncher.launch(intent);
        });

        reloadDrivers();
        return root;
    }

    private void setupGpuStatus() {
        boolean supportsAdreno = RPCS3.supportsCustomDriver();
        if (supportsAdreno) {
            mTxtGpuStatusTitle.setText("Qualcomm Adreno GPU Detected");
            mTxtGpuStatusDesc.setText("Your Snapdragon processor supports custom Turnip Mesa Vulkan drivers for enhanced performance and rendering fixes in RPCS3.");
            mBtnInstallDriver.setEnabled(true);
        } else {
            mTxtGpuStatusTitle.setText("Standard GPU Detected");
            mTxtGpuStatusDesc.setText("Qualcomm Adreno custom driver hooks are not supported on this SoC. The standard system Vulkan driver will be used.");
            mBtnInstallDriver.setEnabled(false);
        }
    }

    public void reloadDrivers() {
        if (mRepository == null) return;
        List<GpuDriver> drivers = mRepository.getInstalledDrivers();
        String active = mRepository.getActiveDriverFolder();
        mAdapter.setDrivers(drivers, active);
    }

    private void installDriverFromUri(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(requireContext(), "Failed to open selected zip", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder error = new StringBuilder();
            boolean ok = mRepository.installDriverZip(is, error);
            if (ok) {
                Toast.makeText(requireContext(), "Custom GPU driver installed successfully!", Toast.LENGTH_LONG).show();
                reloadDrivers();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Driver Installation Failed")
                        .setMessage(error.toString())
                        .setPositiveButton("OK", null)
                        .show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error installing driver: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDriverSelect(GpuDriver driver) {
        mRepository.setActiveDriver(driver);
        reloadDrivers();
        Toast.makeText(requireContext(), "Active GPU driver set to: " + driver.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDriverDelete(GpuDriver driver) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Driver")
                .setMessage("Are you sure you want to delete '" + driver.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mRepository.deleteDriver(driver);
                    reloadDrivers();
                    Toast.makeText(requireContext(), "Deleted driver", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
