package net.rpcs3.android.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import net.rpcs3.android.model.GpuDriver;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DriverRepository {
    private static final String TAG = "DriverRepository";
    private static final String PREF_NAME = "gpu_driver_prefs";
    private static final String KEY_ACTIVE_DRIVER = "active_driver_folder";
    private static final String DRIVERS_DIR_NAME = "gpu_drivers";

    private static DriverRepository sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final File mDriversDir;

    private DriverRepository(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mDriversDir = new File(mContext.getFilesDir(), DRIVERS_DIR_NAME);
        if (!mDriversDir.exists()) {
            mDriversDir.mkdirs();
        }
    }

    public static synchronized DriverRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DriverRepository(context);
        }
        return sInstance;
    }

    public List<GpuDriver> getInstalledDrivers() {
        List<GpuDriver> drivers = new ArrayList<>();
        drivers.add(GpuDriver.createSystemDefault());

        File[] files = mDriversDir.listFiles();
        if (files != null) {
            for (File folder : files) {
                if (folder.isDirectory()) {
                    File metaFile = new File(folder, "meta.json");
                    if (metaFile.exists()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            JSONObject meta = new JSONObject(sb.toString());
                            drivers.add(GpuDriver.fromMetaJson(folder, meta));
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to load driver meta for " + folder.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        return drivers;
    }

    public String getActiveDriverFolder() {
        return mPrefs.getString(KEY_ACTIVE_DRIVER, "");
    }

    public void setActiveDriver(GpuDriver driver) {
        if (driver == null || driver.isSystemDefault()) {
            mPrefs.edit().putString(KEY_ACTIVE_DRIVER, "").apply();
        } else {
            mPrefs.edit().putString(KEY_ACTIVE_DRIVER, driver.getFolderPath()).apply();
        }
    }

    public boolean installDriverZip(InputStream zipStream, StringBuilder outError) {
        File tempDir = new File(mContext.getCacheDir(), "driver_temp_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            unzip(zipStream, tempDir);

            File metaFile = new File(tempDir, "meta.json");
            if (!metaFile.exists()) {
                if (outError != null) outError.append("meta.json not found in driver zip archive");
                deleteRecursive(tempDir);
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject meta = new JSONObject(sb.toString());
            int minApi = meta.optInt("minApi", 0);
            if (Build.VERSION.SDK_INT < minApi) {
                if (outError != null) outError.append("Your Android version (API ").append(Build.VERSION.SDK_INT)
                        .append(") is lower than driver minimum required API ").append(minApi);
                deleteRecursive(tempDir);
                return false;
            }

            String driverName = meta.optString("name", "driver_" + System.currentTimeMillis());
            String sanitizedName = driverName.replaceAll("[^a-zA-Z0-9._-]", "_");
            File targetDir = new File(mDriversDir, sanitizedName);

            if (targetDir.exists()) {
                deleteRecursive(targetDir);
            }

            if (!tempDir.renameTo(targetDir)) {
                // If rename fails across mounts, do manual copy
                copyDirectory(tempDir, targetDir);
                deleteRecursive(tempDir);
            }

            Log.i(TAG, "Successfully installed custom driver to: " + targetDir.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to install driver: " + e.getMessage());
            if (outError != null) outError.append(e.getMessage());
            deleteRecursive(tempDir);
            return false;
        }
    }

    public boolean deleteDriver(GpuDriver driver) {
        if (driver == null || driver.isSystemDefault()) return false;
        File dir = new File(driver.getFolderPath());
        if (dir.exists()) {
            boolean ok = deleteRecursive(dir);
            if (getActiveDriverFolder().equals(driver.getFolderPath())) {
                setActiveDriver(null);
            }
            return ok;
        }
        return false;
    }

    private void unzip(InputStream is, File destDir) throws Exception {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDir.delete();
    }

    private void copyDirectory(File source, File target) throws Exception {
        if (source.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(source, child), new File(target, child));
                }
            }
        } else {
            try (InputStream in = new java.io.FileInputStream(source);
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
