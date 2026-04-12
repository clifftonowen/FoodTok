package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.dto.UserDto;

import java.util.ArrayList;
import java.util.List;

/** Adapter for displaying user search results in a vertical list. */
public class UserSearchAdapter
    extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

  /** Callback when a user row is tapped. */
  public interface OnUserClickListener {
    void onUserClick(UserDto user);
  }

  private final List<UserDto> users = new ArrayList<>();
  private OnUserClickListener listener;

  public void setListener(OnUserClickListener listener) {
    this.listener = listener;
  }

  public void setUsers(List<UserDto> newUsers) {
    users.clear();
    users.addAll(newUsers);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
      int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_follow_user, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder,
      int position) {
    UserDto user = users.get(position);
    holder.tvUsername.setText(
        user.username != null ? user.username : "");

    if (!TextUtils.isEmpty(user.avatarUrl)) {
      Glide.with(holder.ivAvatar)
          .load(user.avatarUrl)
          .circleCrop()
          .placeholder(R.drawable.ic_burger_foodtok)
          .into(holder.ivAvatar);
    } else {
      holder.ivAvatar.setImageResource(R.drawable.ic_burger_foodtok);
    }

    holder.itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onUserClick(user);
      }
    });
  }

  @Override
  public int getItemCount() {
    return users.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    final ImageView ivAvatar;
    final TextView tvUsername;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
      tvUsername = itemView.findViewById(R.id.tvFollowUsername);
    }
  }
}
