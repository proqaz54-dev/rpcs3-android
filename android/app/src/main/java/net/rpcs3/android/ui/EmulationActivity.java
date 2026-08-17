package net.rpcs3.android.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.rpcs3.android.R;
import net.rpcs3.android.RPCS3;
import net.rpcs3.android.overlay.PadOverlayView;
import net.rpcs3.android.ui.fragment.SettingsFragment;

public class EmulationActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "EmulationActivity";

    public static final String EXTRA_GAME_PATH = "extra_game_path";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";
    public static final String EXTRA_GAME_TITLE_ID = "extra_game_title_id";

    private SurfaceView mSurfaceView;
    private PadOverlayView mPadOverlay;
    private ImageButton mBtnMenu;

    private String mGamePath;
    private String mGameTitle;
    private String mGameTitleId;

    private Thread mBootThread;
    private boolean mIsRunning = false;

    private int mGamepadDigital1 = 0;
    private int mGamepadDigital2 = 0;
    private int mGamepadLeftX = 128;
    private int mGamepadLeftY = 128;
    private int mGamepadRightX = 128;
    private int mGamepadRightY = 128;
    private int mGamepadL2 = 0;
    private int mGamepadR2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableFullScreen();

        setContentView(R.layout.activity_emulation);

        mGamePath = getIntent().getStringExtra(EXTRA_GAME_PATH);
        mGameTitle = getIntent().getStringExtra(EXTRA_GAME_TITLE);
        mGameTitleId = getIntent().getStringExtra(EXTRA_GAME_TITLE_ID);

        mSurfaceView = findViewById(R.id.surface_view);
        mPadOverlay = findViewById(R.id.pad_overlay);
        mBtnMenu = findViewById(R.id.btn_menu);

        mSurfaceView.getHolder().addCallback(this);

        setupControls();
        setupMenu();

        if (mGamePath == null || mGamePath.isEmpty()) {
            Toast.makeText(this, "No game path provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startBootThread();
    }

    private void setupControls() {
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE);
        boolean overlayEnabled = prefs.getBoolean(SettingsFragment.KEY_OVERLAY_ENABLED, true);
        int opacity = prefs.getInt(SettingsFragment.KEY_OVERLAY_OPACITY, 70);
        boolean haptic = prefs.getBoolean(SettingsFragment.KEY_HAPTIC_ENABLED, true);

        mPadOverlay.setVisibility(overlayEnabled ? View.VISIBLE : View.GONE);
        mPadOverlay.setOverlayAlpha(opacity / 100.0f);
        mPadOverlay.setHapticEnabled(haptic);
    }

    private void setupMenu() {
        mBtnMenu.setOnClickListener(v -> showInGameMenu());
    }

    private void startBootThread() {
        mBootThread = new Thread(() -> {
            mIsRunning = true;
            Log.i(TAG, "Starting RPCS3 boot on thread for: " + mGamePath);
            int res = RPCS3.boot(mGamePath);
            Log.i(TAG, "RPCS3 boot completed with code: " + res);
            mIsRunning = false;
        }, "RPCS3-BootThread");
        mBootThread.start();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "Surface created: " + holder.getSurface());
        RPCS3.surfaceEvent(holder.getSurface(), 0);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Surface changed: " + width + "x" + height);
        RPCS3.surfaceEvent(holder.getSurface(), 1);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed");
        RPCS3.surfaceEvent(holder.getSurface(), 2);
    }

    private void showInGameMenu() {
        RPCS3.pause();

        String[] options = {
                "Resume Game",
                "Toggle Touch Controls (" + (mPadOverlay.getVisibility() == View.VISIBLE ? "Hide" : "Show") + ")",
                "Exit to Launcher"
        };

        new AlertDialog.Builder(this)
                .setTitle(mGameTitle != null ? mGameTitle : "In-Game Menu")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        RPCS3.resume();
                    } else if (which == 1) {
                        mPadOverlay.setVisibility(mPadOverlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        RPCS3.resume();
                    } else if (which == 2) {
                        RPCS3.stop();
                        finish();
                    }
                })
                .setOnCancelListener(dialog -> RPCS3.resume())
                .show();
    }

    @Override
    public void onBackPressed() {
        showInGameMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        RPCS3.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullScreen();
        if (mIsRunning) {
            RPCS3.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RPCS3.stop();
        if (mBootThread != null) {
            mBootThread.interrupt();
        }
    }

    // Physical Gamepad Support
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isGamepadEvent(event)) {
            if (handleGamepadKey(keyCode, true)) {
                sendGamepadState();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isGamepadEvent(event)) {
            if (handleGamepadKey(keyCode, false)) {
                sendGamepadState();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            mGamepadLeftX = (int) (event.getAxisValue(MotionEvent.AXIS_X) * 127.0f + 128.0f);
            mGamepadLeftY = (int) (event.getAxisValue(MotionEvent.AXIS_Y) * 127.0f + 128.0f);
            mGamepadRightX = (int) (event.getAxisValue(MotionEvent.AXIS_Z) * 127.0f + 128.0f);
            mGamepadRightY = (int) (event.getAxisValue(MotionEvent.AXIS_RZ) * 127.0f + 128.0f);

            float l2Val = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
            float r2Val = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            mGamepadL2 = (int) (l2Val * 255.0f);
            mGamepadR2 = (int) (r2Val * 255.0f);

            if (l2Val > 0.2f) mGamepadDigital2 |= RPCS3.CELL_PAD_CTRL_L2;
            else mGamepadDigital2 &= ~RPCS3.CELL_PAD_CTRL_L2;

            if (r2Val > 0.2f) mGamepadDigital2 |= RPCS3.CELL_PAD_CTRL_R2;
            else mGamepadDigital2 &= ~RPCS3.CELL_PAD_CTRL_R2;

            sendGamepadState();
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean isGamepadEvent(KeyEvent event) {
        if (event == null) return false;
        int source = event.getSource();
        return (source & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK | InputDevice.SOURCE_DPAD)) != 0;
    }

    private boolean handleGamepadKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: setBit1(RPCS3.CELL_PAD_CTRL_UP, pressed); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: setBit1(RPCS3.CELL_PAD_CTRL_DOWN, pressed); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: setBit1(RPCS3.CELL_PAD_CTRL_LEFT, pressed); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: setBit1(RPCS3.CELL_PAD_CTRL_RIGHT, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_A: setBit2(RPCS3.CELL_PAD_CTRL_CROSS, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_B: setBit2(RPCS3.CELL_PAD_CTRL_CIRCLE, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_X: setBit2(RPCS3.CELL_PAD_CTRL_SQUARE, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_Y: setBit2(RPCS3.CELL_PAD_CTRL_TRIANGLE, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_L1: setBit2(RPCS3.CELL_PAD_CTRL_L1, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_R1: setBit2(RPCS3.CELL_PAD_CTRL_R1, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_L2:
                setBit2(RPCS3.CELL_PAD_CTRL_L2, pressed);
                mGamepadL2 = pressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                setBit2(RPCS3.CELL_PAD_CTRL_R2, pressed);
                mGamepadR2 = pressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_START: setBit1(RPCS3.CELL_PAD_CTRL_START, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT: setBit1(RPCS3.CELL_PAD_CTRL_SELECT, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: setBit1(RPCS3.CELL_PAD_CTRL_L3, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: setBit1(RPCS3.CELL_PAD_CTRL_R3, pressed); return true;
            case KeyEvent.KEYCODE_BUTTON_MODE: setBit1(RPCS3.CELL_PAD_CTRL_PS, pressed); return true;
        }
        return false;
    }

    private void setBit1(int bit, boolean set) {
        if (set) mGamepadDigital1 |= bit;
        else mGamepadDigital1 &= ~bit;
    }

    private void setBit2(int bit, boolean set) {
        if (set) mGamepadDigital2 |= bit;
        else mGamepadDigital2 &= ~bit;
    }

    private void sendGamepadState() {
        RPCS3.sendPadData(mGamepadDigital1, mGamepadDigital2, mGamepadLeftX, mGamepadLeftY, mGamepadRightX, mGamepadRightY, mGamepadL2, mGamepadR2);
    }

    private void enableFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
