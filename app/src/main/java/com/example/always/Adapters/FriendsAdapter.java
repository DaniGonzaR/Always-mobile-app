package com.example.always.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.always.R;

import java.util.List;
import java.util.Map;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private List<Map<String, Object>> friendList;
    private OnFriendClickListener onFriendClickListener;


    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        public TextView usernameTextView;
        public ImageView profileImageView;

        public FriendViewHolder(View itemView, final OnFriendClickListener listener) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.friend_username_text_view);
            profileImageView = itemView.findViewById(R.id.profile_image_view);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onFriendClick(position);
                        }
                    }
                }
            });
        }
    }

    public interface OnFriendClickListener {
        void onFriendClick(int position);
    }

    public void setOnFriendClickListener(OnFriendClickListener listener) {
        this.onFriendClickListener = listener;
    }

    public FriendsAdapter(List<Map<String, Object>> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_item, parent, false);
        return new FriendViewHolder(view, onFriendClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Map<String, Object> friend = friendList.get(position);
        holder.usernameTextView.setText((String) friend.get("NombreUsuario"));

        String profileImageUrl = (String) friend.get("profileImage");
        if (profileImageUrl != null) {
            Glide.with(holder.itemView.getContext())
                    .load(profileImageUrl)
                    .into(holder.profileImageView);
        }
    }


    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public List<Map<String, Object>> getFriendList() {
        return friendList;
    }

    public void updateProfileImage(String userId, String profileImageUrl) {
        for (Map<String, Object> friend : friendList) {
            String friendUserId = (String) friend.get("UserId");
            if (friendUserId.equals(userId)) {
                friend.put("profileImage", profileImageUrl);
                notifyDataSetChanged();
                return;
            }
        }
    }

}
