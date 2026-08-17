package net.rpcs3.android.ui.fragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import net.rpcs3.android.R;
import net.rpcs3.android.model.Game;
import net.rpcs3.android.repository.GameRepository;
import net.rpcs3.android.ui.EmulationActivity;
import net.rpcs3.android.ui.adapter.GameAdapter;

import java.util.ArrayList;
import java.util.List;

public class GamesFragment extends Fragment implements GameAdapter.OnGameClickListener {
    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mRecyclerView;
    private LinearLayout mLayoutEmpty;
    private MaterialButton mBtnAddGamesEmpty;
    private GameAdapter mAdapter;
    private GameRepository mRepository;
    private List<Game> mAllGames = new ArrayList<>();
    private String mCurrentFilter = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_games, container, false);

        mSwipeRefresh = root.findViewById(R.id.swipe_refresh);
        mRecyclerView = root.findViewById(R.id.recycler_games);
        mLayoutEmpty = root.findViewById(R.id.layout_empty_state);
        mBtnAddGamesEmpty = root.findViewById(R.id.btn_add_games_empty);

        mRepository = GameRepository.getInstance(requireContext());
        mAdapter = new GameAdapter(this);

        updateSpanCount();
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefresh.setOnRefreshListener(this::reloadGames);
        mBtnAddGamesEmpty.setOnClickListener(v -> {
            if (getActivity() instanceof OnAddGamesRequestedListener) {
                ((OnAddGamesRequestedListener) getActivity()).onAddGamesRequested();
            }
        });

        reloadGames();
        return root;
    }

    public interface OnAddGamesRequestedListener {
        void onAddGamesRequested();
    }

    public void reloadGames() {
        if (mRepository == null) return;
        mAllGames = mRepository.getGames();
        applyFilter(mCurrentFilter);
        if (mSwipeRefresh != null) {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    public void filter(String query) {
        mCurrentFilter = query != null ? query.trim() : "";
        applyFilter(mCurrentFilter);
    }

    private void applyFilter(String query) {
        List<Game> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(mAllGames);
        } else {
            String lower = query.toLowerCase();
            for (Game game : mAllGames) {
                if (game.getTitle().toLowerCase().contains(lower) ||
                    game.getTitleId().toLowerCase().contains(lower)) {
                    filtered.add(game);
                }
            }
        }

        mAdapter.setGames(filtered);
        boolean isEmpty = filtered.isEmpty();
        mLayoutEmpty.setVisibility(isEmpty && mAllGames.isEmpty() ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isEmpty && mAllGames.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateSpanCount();
    }

    private void updateSpanCount() {
        if (getContext() == null || mRecyclerView == null) return;
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
    }

    @Override
    public void onGameClick(Game game) {
        if (game == null || game.getPath().isEmpty()) return;
        mRepository.updateLastPlayed(game.getPath());

        Intent intent = new Intent(requireContext(), EmulationActivity.class);
        intent.putExtra(EmulationActivity.EXTRA_GAME_PATH, game.getPath());
        intent.putExtra(EmulationActivity.EXTRA_GAME_TITLE, game.getTitle());
        intent.putExtra(EmulationActivity.EXTRA_GAME_TITLE_ID, game.getTitleId());
        startActivity(intent);
    }

    @Override
    public void onGameLongClick(Game game, View view) {
        if (getContext() == null || game == null) return;

        String[] options = {"Boot Game", "Game Details", "Remove from Library"};
        new AlertDialog.Builder(requireContext())
                .setTitle(game.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            onGameClick(game);
                            break;
                        case 1:
                            showGameDetails(game);
                            break;
                        case 2:
                            mRepository.removeGame(game);
                            reloadGames();
                            Toast.makeText(requireContext(), "Removed " + game.getTitle(), Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showGameDetails(Game game) {
        String msg = "Title: " + game.getTitle() + "\n" +
                "Title ID: " + game.getTitleId() + "\n" +
                "Category: " + game.getCategory() + " (" + (game.isDisc() ? "Disc Game" : "Digital HDD") + ")\n" +
                "Version: " + game.getAppVersion() + "\n" +
                "Path: " + game.getPath();

        new AlertDialog.Builder(requireContext())
                .setTitle("Game Information")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
