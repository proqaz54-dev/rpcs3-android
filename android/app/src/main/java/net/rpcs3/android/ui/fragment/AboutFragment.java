package net.rpcs3.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.rpcs3.android.R;
import net.rpcs3.android.RPCS3;

public class AboutFragment extends Fragment {
    private TextView mTxtSystemInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);
        mTxtSystemInfo = root.findViewById(R.id.txt_system_info);

        try {
            String sysInfo = RPCS3.systemInfo();
            if (sysInfo != null && !sysInfo.isEmpty()) {
                mTxtSystemInfo.setText(sysInfo);
            } else {
                mTxtSystemInfo.setText("RPCS3 Android\nArchitecture: ARM64-v8a\nVulkan 1.3 Compatible");
            }
        } catch (Exception e) {
            mTxtSystemInfo.setText("RPCS3 Android\nARM64-v8a");
        }

        return root;
    }
}
