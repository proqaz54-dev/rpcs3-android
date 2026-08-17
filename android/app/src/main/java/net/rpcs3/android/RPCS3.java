package net.rpcs3.android;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import org.json.JSONObject;

import java.io.File;

public class RPCS3 {
    private static final String TAG = "RPCS3-JAVA";

    static {
        try {
            System.loadLibrary("rpcs3_android");
            Log.i(TAG, "librpcs3_android.so loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load librpcs3_android.so: " + e.getMessage());
        }
    }

    public enum EmulatorState {
        Stopped(0),
        Loading(1),
        Stopping(2),
        Running(3),
        Paused(4),
        Frozen(5),
        Ready(6),
        Starting(7);

        private final int value;

        EmulatorState(int value) {
            this.value = value;
        }

        public static EmulatorState fromInt(int val) {
            for (EmulatorState state : values()) {
                if (state.value == val) return state;
            }
            return Stopped;
        }
    }

    public enum BootResult {
        NoErrors(0),
        GenericError(1),
        NothingToBoot(2),
        WrongDiscLocation(3),
        InvalidFileOrFolder(4),
        InvalidBDvdFolder(5),
        InstallFailed(6),
        DecryptionError(7),
        FileCreationError(8),
        FirmwareMissing(9),
        UnsupportedDiscType(10),
        SavestateCorrupted(11),
        SavestateVersionUnsupported(12),
        StillRunning(13),
        AlreadyAdded(14),
        CurrentlyRestricted(15);

        private final int value;

        BootResult(int value) {
            this.value = value;
        }

        public static BootResult fromInt(int val) {
            for (BootResult result : values()) {
                if (result.value == val) return result;
            }
            return GenericError;
        }
    }

    public static final int CELL_PAD_CTRL_SELECT   = 0x00000001;
    public static final int CELL_PAD_CTRL_L3       = 0x00000002;
    public static final int CELL_PAD_CTRL_R3       = 0x00000004;
    public static final int CELL_PAD_CTRL_START    = 0x00000008;
    public static final int CELL_PAD_CTRL_UP       = 0x00000010;
    public static final int CELL_PAD_CTRL_RIGHT    = 0x00000020;
    public static final int CELL_PAD_CTRL_DOWN     = 0x00000040;
    public static final int CELL_PAD_CTRL_LEFT     = 0x00000080;
    public static final int CELL_PAD_CTRL_PS       = 0x00000100;

    public static final int CELL_PAD_CTRL_L2       = 0x00000001;
    public static final int CELL_PAD_CTRL_R2       = 0x00000002;
    public static final int CELL_PAD_CTRL_L1       = 0x00000004;
    public static final int CELL_PAD_CTRL_R1       = 0x00000008;
    public static final int CELL_PAD_CTRL_TRIANGLE = 0x00000010;
    public static final int CELL_PAD_CTRL_CIRCLE   = 0x00000020;
    public static final int CELL_PAD_CTRL_CROSS    = 0x00000040;
    public static final int CELL_PAD_CTRL_SQUARE   = 0x00000080;

    private static boolean sInitialized = false;
    private static String sDataDirectory = "";
    private static String sCacheDirectory = "";

    public static synchronized boolean init(Context context) {
        if (sInitialized) {
            return true;
        }

        File filesDir = context.getExternalFilesDir(null);
        if (filesDir == null) {
            filesDir = context.getFilesDir();
        }
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }

        sDataDirectory = filesDir.getAbsolutePath() + "/";
        sCacheDirectory = cacheDir.getAbsolutePath() + "/";

        Log.i(TAG, "Initializing RPCS3 directories: Data=" + sDataDirectory + ", Cache=" + sCacheDirectory);

        sInitialized = initialize(sDataDirectory, sCacheDirectory);
        return sInitialized;
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    public static String getDataDirectory() {
        return sDataDirectory;
    }

    public static String getCacheDirectory() {
        return sCacheDirectory;
    }

    // Native JNI functions
    public static native boolean initialize(String rootDir, String cacheDir);
    public static native int boot(String bootPath);
    public static native boolean surfaceEvent(Surface surface, int event);
    public static native void pause();
    public static native void resume();
    public static native void stop();
    public static native int getState();
    public static native String getTitleId();
    public static native String getTitle();
    public static native String scanGame(String gamePath);
    public static native String systemInfo();
    public static native boolean supportsCustomDriver();
    public static native boolean sendPadData(int digital1, int digital2, int lsX, int lsY, int rsX, int rsY, int l2Axis, int r2Axis);
}
