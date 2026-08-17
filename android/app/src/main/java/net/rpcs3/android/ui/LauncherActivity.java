package net.rpcs3.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import net.rpcs3.android.R;
import net.rpcs3.android.RPCS3;
import net.rpcs3.android.model.Game;
import net.rpcs3.android.repository.GameRepository;
import net.rpcs3.android.ui.fragment.AboutFragment;
import net.rpcs3.android.ui.fragment.DriversFragment;
import net.rpcs3.android.ui.fragment.GamesFragment;
import net.rpcs3.android.ui.fragment.SettingsFragment;

import java.io.File;

public class LauncherActivity extends AppCompatActivity implements GamesFragment.OnAddGamesRequestedListener {
    private static final String TAG = "LauncherActivity";

    private EditText mEditSearch;
    private ExtendedFloatingActionButton mFabAdd;
    private BottomNavigationView mBottomNav;

    private final GamesFragment mGamesFragment = new GamesFragment();
    private final DriversFragment mDriversFragment = new DriversFragment();
    private final SettingsFragment mSettingsFragment = new SettingsFragment();
    private final AboutFragment mAboutFragment = new AboutFragment();

    private Fragment mCurrentFragment;
    private GameRepository mGameRepository;

    private ActivityResultLauncher<Intent> mFolderPickerLauncher;
    private ActivityResultLauncher<Intent> mFilePickerLauncher;
    private ActivityResultLauncher<Intent> mRapPickerLauncher;
    private ActivityResultLauncher<Intent> mAllFilesLauncher;
    private ActivityResultLauncher<String[]> mRuntimePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Initialize RPCS3 storage & core callbacks
        RPCS3.init(this);
        mGameRepository = GameRepository.getInstance(this);

        mEditSearch = findViewById(R.id.edit_search);
        mFabAdd = findViewById(R.id.fab_add);
        mBottomNav = findViewById(R.id.bottom_navigation);

        setupNavigation();
        setupSearch();
        setupPickers();
        setupPermissionLaunchers();

        // Android 11+ requires "All files access" to read game files from shared storage
        ensureStoragePermission();

        // Also check if external storage directory contains PS3 games
        scanDefaultStorage();
    }

    private void setupPermissionLaunchers() {
        mAllFilesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (hasStoragePermission()) {
                        Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show();
                    }
                    scanDefaultStorage();
                }
        );

        mRuntimePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    onStoragePermissionResult(result);
                }
        );
    }

    private void onStoragePermissionResult(java.util.Map<String, Boolean> result) {
        boolean granted = false;
        for (Boolean value : result.values()) {
            granted |= Boolean.TRUE.equals(value);
        }
        if (granted) {
            Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show();
        }
        scanDefaultStorage();
    }

    private void ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: needs the "All files access" special permission
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Grant 'All files access' so RPCS3 can read your games (ISO/PKG/PS3_GAME folders)", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    mAllFilesLauncher.launch(intent);
                } catch (Exception e) {
                    Log.w(TAG, "All files access intent failed, using fallback", e);
                    mAllFilesLauncher.launch(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
            }
        } else {
            // Android 10 and below: runtime storage permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mRuntimePermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            }
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupNavigation() {
        switchFragment(mGamesFragment);

        mBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_games) {
                switchFragment(mGamesFragment);
                mEditSearch.setVisibility(View.VISIBLE);
                mFabAdd.show();
                return true;
            } else if (id == R.id.nav_drivers) {
                switchFragment(mDriversFragment);
                mEditSearch.setVisibility(View.GONE);
                mFabAdd.hide();
                return true;
            } else if (id == R.id.nav_settings) {
                switchFragment(mSettingsFragment);
                mEditSearch.setVisibility(View.GONE);
                mFabAdd.hide();
                return true;
            } else if (id == R.id.nav_about) {
                switchFragment(mAboutFragment);
                mEditSearch.setVisibility(View.GONE);
                mFabAdd.hide();
                return true;
            }
            return false;
        });

        mFabAdd.setOnClickListener(v -> showAddOptionsDialog());
    }

    private void setupSearch() {
        mEditSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentFragment == mGamesFragment) {
                    mGamesFragment.filter(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPickers() {
        mFolderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            handleFolderPicked(treeUri);
                        }
                    }
                }
        );

        mFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleFilePicked(fileUri);
                        }
                    }
                }
        );

        mRapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri rapUri = result.getData().getData();
                        if (rapUri != null) {
                            handleRapPicked(rapUri);
                        }
                    }
                }
        );
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == mCurrentFragment) return;
        mCurrentFragment = fragment;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showAddOptionsDialog() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, "Storage access required to add games", Toast.LENGTH_SHORT).show();
            ensureStoragePermission();
            return;
        }

        String[] options = {
                "Select Game Directory (Folder with PS3_GAME)",
                "Select Game File (ISO / PKG / SFO)",
                "Select RAP License Key (.rap)"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Games")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        mFolderPickerLauncher.launch(intent);
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        mFilePickerLauncher.launch(intent);
                    } else if (which == 2) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        mRapPickerLauncher.launch(intent);
                    }
                })
                .show();
    }

    @Override
    public void onAddGamesRequested() {
        showAddOptionsDialog();
    }

    private void handleFolderPicked(Uri uri) {
        String docId = DocumentsContract.getTreeDocumentId(uri);
        String path = resolveDocIdToPath(docId);
        if (path != null && new File(path).exists()) {
            scanDirectory(new File(path));
        } else {
            // Scan app internal games directory fallback
            File gamesDir = new File(RPCS3.getDataDirectory(), "games");
            scanDirectory(gamesDir);
        }
    }

    private void handleFilePicked(Uri uri) {
        String docId = DocumentsContract.getDocumentId(uri);
        String path = resolveDocIdToPath(docId);
        if (path == null || !new File(path).exists()) {
            Toast.makeText(this, "Could not access the selected file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (path.toLowerCase().endsWith(".rap")) {
            handleRapPicked(path);
            return;
        }

        mGameRepository.scanSinglePath(path, new GameRepository.OnScanListener() {
            @Override
            public void onScanProgress(String currentPath) {}

            @Override
            public void onGameFound(Game game) {
                Toast.makeText(LauncherActivity.this, "Found: " + game.getTitle(), Toast.LENGTH_SHORT).show();
                mGamesFragment.reloadGames();
            }

            @Override
            public void onScanComplete(int count) {
                mGamesFragment.reloadGames();
            }
        });
    }

    private void handleRapPicked(Uri uri) {
        String docId = DocumentsContract.getDocumentId(uri);
        String path = resolveDocIdToPath(docId);
        if (path == null || !new File(path).exists()) {
            Toast.makeText(this, "Could not access the selected RAP file", Toast.LENGTH_SHORT).show();
            return;
        }
        handleRapPicked(path);
    }

    private void handleRapPicked(String path) {
        boolean ok = RPCS3.importRap(path);
        Toast.makeText(this, ok ? "RAP license key imported" : "Failed to import RAP key",
                Toast.LENGTH_SHORT).show();
        mGamesFragment.reloadGames();
    }

    private String resolveDocIdToPath(String docId) {
        if (docId == null) return null;
        if (docId.startsWith("primary:")) {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + docId.substring("primary:".length());
        }
        if (docId.startsWith("raw:")) {
            return docId.substring("raw:".length());
        }
        if (docId.startsWith("/")) {
            return docId;
        }
        // Some providers expose a full filesystem path after a single colon
        int sep = docId.indexOf(':');
        if (sep > 0 && docId.substring(sep + 1).startsWith("/")) {
            return docId.substring(sep + 1);
        }
        return null;
    }

    private void scanDirectory(File dir) {
        Toast.makeText(this, "Scanning for PS3 games...", Toast.LENGTH_SHORT).show();
        mGameRepository.scanDirectory(dir, new GameRepository.OnScanListener() {
            @Override
            public void onScanProgress(String currentPath) {}

            @Override
            public void onGameFound(Game game) {
                mGamesFragment.reloadGames();
            }

            @Override
            public void onScanComplete(int count) {
                mGamesFragment.reloadGames();
                Toast.makeText(LauncherActivity.this, "Scan complete: found " + count + " game(s)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scanDefaultStorage() {
        File romsDir = new File(Environment.getExternalStorageDirectory(), "ROMs/PS3");
        if (romsDir.exists() && romsDir.isDirectory()) {
            mGameRepository.scanDirectory(romsDir, null);
        }
        File gamesDir = new File(RPCS3.getDataDirectory(), "games");
        if (gamesDir.exists() && gamesDir.isDirectory()) {
            mGameRepository.scanDirectory(gamesDir, null);
        }
    }
}
