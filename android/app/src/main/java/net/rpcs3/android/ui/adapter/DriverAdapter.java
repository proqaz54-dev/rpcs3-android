package net.rpcs3.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.rpcs3.android.R;
import net.rpcs3.android.model.GpuDriver;

import java.util.ArrayList;
import java.util.List;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {
    private final List<GpuDriver> mDrivers = new ArrayList<>();
    private String mActiveDriverFolder = "";
    private final OnDriverClickListener mListener;

    public interface OnDriverClickListener {
        void onDriverSelect(GpuDriver driver);
        void onDriverDelete(GpuDriver driver);
    }

    public DriverAdapter(OnDriverClickListener listener) {
        mListener = listener;
    }

    public void setDrivers(List<GpuDriver> drivers, String activeFolder) {
        mDrivers.clear();
        if (drivers != null) {
            mDrivers.addAll(drivers);
        }
        mActiveDriverFolder = activeFolder != null ? activeFolder : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        GpuDriver driver = mDrivers.get(position);
        boolean isSelected = driver.isSystemDefault() ? mActiveDriverFolder.isEmpty() : mActiveDriverFolder.equals(driver.getFolderPath());
        holder.bind(driver, isSelected, mListener);
    }

    @Override
    public int getItemCount() {
        return mDrivers.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton radioSelected;
        private final TextView txtName;
        private final TextView txtMeta;
        private final TextView txtDescription;
        private final ImageButton btnDelete;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            radioSelected = itemView.findViewById(R.id.radio_selected);
            txtName = itemView.findViewById(R.id.txt_driver_name);
            txtMeta = itemView.findViewById(R.id.txt_driver_meta);
            txtDescription = itemView.findViewById(R.id.txt_driver_description);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(GpuDriver driver, boolean isSelected, OnDriverClickListener listener) {
            txtName.setText(driver.getName());
            String meta = "by " + driver.getAuthor() + " • " + (driver.getVendor().isEmpty() ? "Vulkan" : driver.getVendor());
            txtMeta.setText(meta);
            txtDescription.setText(driver.getDescription().isEmpty() ? driver.getDriverVersion() : driver.getDescription());

            radioSelected.setChecked(isSelected);

            btnDelete.setVisibility(driver.isSystemDefault() ? View.GONE : View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDriverDelete(driver);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDriverSelect(driver);
            });
        }
    }
}
