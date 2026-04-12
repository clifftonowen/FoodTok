package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.dto.UserDto;

import java.util.List;

public class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserDto user);
    }

    private List<UserDto> users;
    private OnUserClickListener clickListener;
    private int lastAnimatedPosition = -1;

    public FollowUserAdapter(List<UserDto> users) {
        this.users = users;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(List<UserDto> newUsers) {
        this.users = newUsers;
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserDto user = users.get(position);

        holder.tvUsername.setText(user.username != null ? user.username : "");

        if (!TextUtils.isEmpty(user.avatarUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatarUrl)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_burger_foodtok);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    clickListener.onUserClick(users.get(adapterPosition));
                }
            }
        });

        // Staggered entrance: slide in from right, only on first appearance
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(60f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setStartDelay(Math.min(position * 70L, 420L))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;

        ViewHolder(View view) {
            super(view);
            ivAvatar = view.findViewById(R.id.ivUserAvatar);
            tvUsername = view.findViewById(R.id.tvFollowUsername);
        }
    }
}
