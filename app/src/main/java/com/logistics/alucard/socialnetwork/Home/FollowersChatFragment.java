package com.logistics.alucard.socialnetwork.Home;


import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.logistics.alucard.socialnetwork.Chat.ChatActivity;
import com.logistics.alucard.socialnetwork.Models.Followers;
import com.logistics.alucard.socialnetwork.R;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FollowersChatFragment extends Fragment {

    private static final String TAG = "FollowersChatFragment";

    //widgets
    private RecyclerView mFollowersList;
    private View mMainView;

    //firebase
    private DatabaseReference mFollowersDatabase, mUsersSettingsDatabase;
    private FirebaseAuth mAuth;

    //vares
    String mCurrentUserID;

    //adapter
    private FirebaseRecyclerAdapter mAdapter;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_followers_chat, container, false);

        //setup Firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();
        mUsersSettingsDatabase = FirebaseDatabase.getInstance().getReference().child("user_account_settings");
        mUsersSettingsDatabase.keepSynced(true);
        mFollowersDatabase = FirebaseDatabase.getInstance().getReference().child("followers").child(mCurrentUserID);

        //setup widgets
        mFollowersList = mMainView.findViewById(R.id.following_list);
        mFollowersList.setHasFixedSize(true);
        mFollowersList.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseRecyclerAdapter();
        mFollowersList.setAdapter(mAdapter);




        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    private void firebaseRecyclerAdapter() {

        FirebaseRecyclerOptions<Followers> options =
                new FirebaseRecyclerOptions.Builder<Followers>()
                .setQuery(mFollowersDatabase, Followers.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Followers, FollowersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FollowersViewHolder holder, int position, @NonNull Followers model) {

                //get position of the following and his user id
                final String followingUserID = getRef(position).getKey();
                mUsersSettingsDatabase.child(followingUserID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: getting followers person data");
                        final String username = dataSnapshot.child("username").getValue().toString();
                        final String description = dataSnapshot.child("description").getValue().toString();
                        final String profilePhoto = dataSnapshot.child("profile_photo").getValue().toString();

                        holder.setName(username);
                        holder.setDescription(description);
                        holder.setProfileImage(profilePhoto);

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("followerUserID", followingUserID);
                                chatIntent.putExtra("followerUsername", username);
                                chatIntent.putExtra("profilePhoto", profilePhoto);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public FollowersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_follower_model, parent, false);
                return new FollowersViewHolder(view);
            }
        };

    }

    public static class FollowersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public FollowersViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public void setName(String username) {
            TextView tvUsername = mView.findViewById(R.id.tvUserName);
            tvUsername.setText(username);
        }

        public void setDescription(String description) {
            TextView tvEmail = mView.findViewById(R.id.tvUserDescription);
            tvEmail.setText(description);
        }

        public void setProfileImage(String imgUrl) {
            CircleImageView profilePhoto = mView.findViewById(R.id.circle_profile_image);
            Picasso.get().load(imgUrl).placeholder(R.drawable.logo).into(profilePhoto);
        }
    }


}
