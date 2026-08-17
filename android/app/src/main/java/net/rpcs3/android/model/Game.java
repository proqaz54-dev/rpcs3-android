package net.rpcs3.android.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class Game {
    private String mTitle;
    private String mTitleId;
    private String mCategory;
    private String mAppVersion;
    private String mPath;
    private String mIconPath;
    private boolean mIsDisc;
    private long mLastPlayedTime;

    public Game() {
    }

    public Game(String title, String titleId, String category, String appVersion, String path, String iconPath, boolean isDisc) {
        mTitle = title;
        mTitleId = titleId;
        mCategory = category;
        mAppVersion = appVersion;
        mPath = path;
        mIconPath = iconPath;
        mIsDisc = isDisc;
        mLastPlayedTime = System.currentTimeMillis();
    }

    public static Game fromJsonObject(JSONObject json) {
        Game game = new Game();
        game.mTitle = json.optString("title", "Unknown PS3 Game");
        game.mTitleId = json.optString("titleId", "");
        game.mCategory = json.optString("category", "DG");
        game.mAppVersion = json.optString("appVersion", "1.00");
        game.mPath = json.optString("path", "");
        game.mIconPath = json.optString("iconPath", "");
        game.mIsDisc = json.optBoolean("isDisc", false);
        game.mLastPlayedTime = json.optLong("lastPlayedTime", 0);
        return game;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("title", mTitle != null ? mTitle : "");
            json.put("titleId", mTitleId != null ? mTitleId : "");
            json.put("category", mCategory != null ? mCategory : "");
            json.put("appVersion", mAppVersion != null ? mAppVersion : "");
            json.put("path", mPath != null ? mPath : "");
            json.put("iconPath", mIconPath != null ? mIconPath : "");
            json.put("isDisc", mIsDisc);
            json.put("lastPlayedTime", mLastPlayedTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public String getTitle() {
        return mTitle != null && !mTitle.isEmpty() ? mTitle : (mTitleId != null ? mTitleId : "Unknown Game");
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitleId() {
        return mTitleId != null ? mTitleId : "";
    }

    public void setTitleId(String titleId) {
        mTitleId = titleId;
    }

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getAppVersion() {
        return mAppVersion;
    }

    public void setAppVersion(String appVersion) {
        mAppVersion = appVersion;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getIconPath() {
        return mIconPath;
    }

    public void setIconPath(String iconPath) {
        mIconPath = iconPath;
    }

    public boolean isDisc() {
        return mIsDisc;
    }

    public void setDisc(boolean disc) {
        mIsDisc = disc;
    }

    public long getLastPlayedTime() {
        return mLastPlayedTime;
    }

    public void setLastPlayedTime(long lastPlayedTime) {
        mLastPlayedTime = lastPlayedTime;
    }

    public boolean hasIcon() {
        if (mIconPath == null || mIconPath.isEmpty()) return false;
        File f = new File(mIconPath);
        return f.exists() && f.isFile();
    }
}
