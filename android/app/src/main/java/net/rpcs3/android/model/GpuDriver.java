package net.rpcs3.android.model;

import org.json.JSONObject;

import java.io.File;

public class GpuDriver {
    private String mName;
    private String mAuthor;
    private String mPackageVersion;
    private String mVendor;
    private String mDriverVersion;
    private int mMinApi;
    private String mDescription;
    private String mLibraryName;
    private String mFolderPath;
    private boolean mIsSystemDefault;

    public GpuDriver() {
    }

    public static GpuDriver createSystemDefault() {
        GpuDriver driver = new GpuDriver();
        driver.mName = "System Default";
        driver.mAuthor = "Qualcomm / System";
        driver.mPackageVersion = "System";
        driver.mVendor = "Qualcomm";
        driver.mDriverVersion = "System default driver";
        driver.mMinApi = 0;
        driver.mDescription = "Default Vulkan graphics driver provided by your device's Android OS.";
        driver.mLibraryName = "";
        driver.mFolderPath = "";
        driver.mIsSystemDefault = true;
        return driver;
    }

    public static GpuDriver fromMetaJson(File folder, JSONObject meta) {
        GpuDriver driver = new GpuDriver();
        driver.mName = meta.optString("name", folder.getName());
        driver.mAuthor = meta.optString("author", "Unknown");
        driver.mPackageVersion = meta.optString("packageVersion", "1.0");
        driver.mVendor = meta.optString("vendor", "Mesa/Turnip");
        driver.mDriverVersion = meta.optString("driverVersion", "");
        driver.mMinApi = meta.optInt("minApi", 29);
        driver.mDescription = meta.optString("description", "");
        driver.mLibraryName = meta.optString("libraryName", "vulkan.adreno.so");
        driver.mFolderPath = folder.getAbsolutePath();
        driver.mIsSystemDefault = false;
        return driver;
    }

    public String getName() {
        return mName != null ? mName : "Unknown Driver";
    }

    public String getAuthor() {
        return mAuthor != null ? mAuthor : "";
    }

    public String getPackageVersion() {
        return mPackageVersion != null ? mPackageVersion : "";
    }

    public String getVendor() {
        return mVendor != null ? mVendor : "";
    }

    public String getDriverVersion() {
        return mDriverVersion != null ? mDriverVersion : "";
    }

    public int getMinApi() {
        return mMinApi;
    }

    public String getDescription() {
        return mDescription != null ? mDescription : "";
    }

    public String getLibraryName() {
        return mLibraryName != null ? mLibraryName : "";
    }

    public String getFolderPath() {
        return mFolderPath != null ? mFolderPath : "";
    }

    public boolean isSystemDefault() {
        return mIsSystemDefault;
    }
}
