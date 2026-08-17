package net.rpcs3.android.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.rpcs3.android.RPCS3;
import net.rpcs3.android.model.Game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameRepository {
    private static final String TAG = "GameRepository";
    private static final String GAMES_FILE = "games.json";

    private static GameRepository sInstance;
    private final Context mContext;
    private final List<Game> mGames = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface OnGamesUpdatedListener {
        void onGamesUpdated(List<Game> games);
    }

    public interface OnScanListener {
        void onScanProgress(String currentPath);
        void onGameFound(Game game);
        void onScanComplete(int count);
    }

    private GameRepository(Context context) {
        mContext = context.getApplicationContext();
        loadGames();
    }

    public static synchronized GameRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GameRepository(context);
        }
        return sInstance;
    }

    public synchronized List<Game> getGames() {
        return new ArrayList<>(mGames);
    }

    public synchronized void addGame(Game game) {
        if (game == null || game.getPath() == null || game.getPath().isEmpty()) {
            return;
        }

        // Check if already exists
        for (int i = 0; i < mGames.size(); i++) {
            Game existing = mGames.get(i);
            if (existing.getPath().equals(game.getPath()) ||
                (!existing.getTitleId().isEmpty() && existing.getTitleId().equals(game.getTitleId()))) {
                mGames.set(i, game);
                saveGames();
                return;
            }
        }

        mGames.add(0, game);
        saveGames();
    }

    public synchronized void removeGame(Game game) {
        if (game == null) return;
        mGames.removeIf(g -> g.getPath().equals(game.getPath()));
        saveGames();
    }

    public synchronized void updateLastPlayed(String path) {
        for (Game game : mGames) {
            if (game.getPath().equals(path)) {
                game.setLastPlayedTime(System.currentTimeMillis());
                mGames.remove(game);
                mGames.add(0, game);
                saveGames();
                break;
            }
        }
    }

    public void scanDirectory(File directory, OnScanListener listener) {
        mExecutor.execute(() -> {
            if (directory == null || !directory.exists() || !directory.canRead()) {
                if (listener != null) {
                    mMainHandler.post(() -> listener.onScanComplete(0));
                }
                return;
            }

            List<File> candidates = new ArrayList<>();
            findGameDirectories(directory, candidates, 0, 4);

            int foundCount = 0;
            for (File cand : candidates) {
                if (listener != null) {
                    mMainHandler.post(() -> listener.onScanProgress(cand.getAbsolutePath()));
                }

                String jsonResult = RPCS3.scanGame(cand.getAbsolutePath());
                if (jsonResult != null && !jsonResult.isEmpty()) {
                    try {
                        JSONObject obj = new JSONObject(jsonResult);
                        if (obj.optBoolean("valid", false)) {
                            Game game = Game.fromJsonObject(obj);
                            addGame(game);
                            foundCount++;
                            if (listener != null) {
                                mMainHandler.post(() -> listener.onGameFound(game));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to parse scanGame result for " + cand.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }

            final int finalFound = foundCount;
            if (listener != null) {
                mMainHandler.post(() -> listener.onScanComplete(finalFound));
            }
        });
    }

    public void scanSinglePath(String path, OnScanListener listener) {
        mExecutor.execute(() -> {
            String jsonResult = RPCS3.scanGame(path);
            if (jsonResult != null && !jsonResult.isEmpty()) {
                try {
                    JSONObject obj = new JSONObject(jsonResult);
                    if (obj.optBoolean("valid", false)) {
                        Game game = Game.fromJsonObject(obj);
                        addGame(game);
                        if (listener != null) {
                            mMainHandler.post(() -> {
                                listener.onGameFound(game);
                                listener.onScanComplete(1);
                            });
                        }
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "scanSinglePath error: " + e.getMessage());
                }
            }
            if (listener != null) {
                mMainHandler.post(() -> listener.onScanComplete(0));
            }
        });
    }

    private void findGameDirectories(File dir, List<File> candidates, int depth, int maxDepth) {
        if (depth > maxDepth || dir == null || !dir.isDirectory()) return;

        File paramSfo = new File(dir, "PARAM.SFO");
        File ps3Game = new File(dir, "PS3_GAME");
        File ps3GameSfo = new File(ps3Game, "PARAM.SFO");

        if (paramSfo.exists() || ps3GameSfo.exists()) {
            candidates.add(dir);
            return; // Don't recurse deeper into game directory
        }

		File[] files = dir.listFiles();
		if (files == null) return;

		for (File file : files) {
			if (file.isDirectory()) {
				findGameDirectories(file, candidates, depth + 1, maxDepth);
			} else if (file.isFile()) {
				String name = file.getName().toLowerCase();
				if (name.endsWith(".iso") || name.endsWith(".pkg") || name.endsWith(".rap")) {
					candidates.add(file);
				}
			}
		}
    }

    private synchronized void loadGames() {
        mGames.clear();
        File file = new File(mContext.getFilesDir(), GAMES_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                mGames.add(Game.fromJsonObject(obj));
            }
            Log.i(TAG, "Loaded " + mGames.size() + " games from storage");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load games: " + e.getMessage());
        }
    }

    private synchronized void saveGames() {
        File file = new File(mContext.getFilesDir(), GAMES_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            JSONArray array = new JSONArray();
            for (Game game : mGames) {
                array.put(game.toJsonObject());
            }
            writer.write(array.toString(2));
            Log.i(TAG, "Saved " + mGames.size() + " games to storage");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save games: " + e.getMessage());
        }
    }
}
