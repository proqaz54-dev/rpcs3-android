package net.rpcs3.android.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.rpcs3.android.R;
import net.rpcs3.android.model.Game;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private final List<Game> mGames = new ArrayList<>();
    private final OnGameClickListener mListener;

    public interface OnGameClickListener {
        void onGameClick(Game game);
        void onGameLongClick(Game game, View view);
    }

    public GameAdapter(OnGameClickListener listener) {
        mListener = listener;
    }

    public void setGames(List<Game> games) {
        mGames.clear();
        if (games != null) {
            mGames.addAll(games);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = mGames.get(position);
        holder.bind(game, mListener);
    }

    @Override
    public int getItemCount() {
        return mGames.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgIcon;
        private final TextView txtTitle;
        private final TextView txtGameId;
        private final TextView txtGameType;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_game_icon);
            txtTitle = itemView.findViewById(R.id.txt_game_title);
            txtGameId = itemView.findViewById(R.id.txt_game_id);
            txtGameType = itemView.findViewById(R.id.txt_game_type);
        }

        public void bind(Game game, OnGameClickListener listener) {
            txtTitle.setText(game.getTitle());
            String meta = (!game.getTitleId().isEmpty() ? game.getTitleId() : "PS3") + " • v" + game.getAppVersion();
            txtGameId.setText(meta);
            txtGameType.setText(game.isDisc() ? "DISC" : "HDD");

            if (game.hasIcon()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(game.getIconPath());
                    if (bmp != null) {
                        imgIcon.setImageBitmap(bmp);
                    } else {
                        imgIcon.setImageResource(R.drawable.ic_rpcs3_foreground);
                    }
                } catch (Exception e) {
                    imgIcon.setImageResource(R.drawable.ic_rpcs3_foreground);
                }
            } else {
                imgIcon.setImageResource(R.drawable.ic_rpcs3_foreground);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onGameClick(game);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onGameLongClick(game, v);
                return true;
            });
        }
    }
}
