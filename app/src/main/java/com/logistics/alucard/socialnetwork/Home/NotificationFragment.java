package com.logistics.alucard.socialnetwork.Home;


import android.content.Intent;
import android.graphics.Typeface;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.logistics.alucard.socialnetwork.Chat.ChatActivity;
import com.logistics.alucard.socialnetwork.Models.Conversation;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Utils.GetTimeAgo;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";

    //widgets
    private RecyclerView mConvList;
    private View mMainView;

    //firebase
    private DatabaseReference mConvDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;

    //adapter
    private FirebaseRecyclerAdapter adapter;

    //vars
    private String mCurrentUserID;
    private String userName;
    private String profilePhotoUrl;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_notification, container, false);
        Log.d(TAG, "onCreateView: started");

        mConvList = mMainView.findViewById(R.id.conv_list);

        //--------------- firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();
        mConvDatabase = FirebaseDatabase.getInstance().getReference().child("chat").child(mCurrentUserID);
        mConvDatabase.keepSynced(true); //? not sure if i want this

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child(getString(R.string.dbname_user_account_settings));
        mUserDatabase.keepSynced(true);

        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUserID);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvList.setHasFixedSize(true); //???
        mConvList.setLayoutManager(linearLayoutManager);

        fireBaseRecyclerAdapter();
        mConvList.setAdapter(adapter);

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void fireBaseRecyclerAdapter() {
        Log.d(TAG, "fireBaseRecyclerAdapter: started");

        Query conversationQuery = mConvDatabase.orderByChild("time_stamp");

        FirebaseRecyclerOptions<Conversation> options =
                new FirebaseRecyclerOptions.Builder<Conversation>()
                .setQuery(conversationQuery, Conversation.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Conversation, ConvViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, int position, @NonNull final Conversation conv) {

                Log.d(TAG, "onBindViewHolder: setting the user data: ");

                final String userID = getRef(position).getKey();

                Query lastMessageQuery = mMessageDatabase.child(userID).limitToLast(1);
                Log.d(TAG, "onBindViewHolder: *************lastMessageQuery*********** is " + lastMessageQuery.getRef().getKey());

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String message = dataSnapshot.child("message").getValue().toString();
                        String messageType = dataSnapshot.child("type").getValue().toString();
                        String messageTimeStamp = dataSnapshot.child("time").getValue().toString();

                        if(messageType.equals("text")) {
                            holder.setMessage(message, conv.isSeen());
                        } else {
                            holder.setMessage("New photo sent.", conv.isSeen());

                        }

                        holder.setTimeStamp(messageTimeStamp);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                mUserDatabase.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userName = dataSnapshot.child("username").getValue().toString();
                        profilePhotoUrl = dataSnapshot.child("profile_photo").getValue().toString();

//                        if(dataSnapshot.hasChild("online")) {
//                            boolean userOnline = (boolean) dataSnapshot.child("online").getValue();
//                            AppLogs.d(TAG, "onDataChange: online status is " + userOnline);
//                            holder.setOnlineStatus(userOnline);
//
//                        }

                        holder.setName(userName);
                        holder.setUserImage(profilePhotoUrl);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //go to chat page
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //disable prevent double click on items
                        //holder.mView.setEnabled(false);
                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                        chatIntent.putExtra("followerUserID", userID);
                        chatIntent.putExtra("followerUsername", userName);
                        chatIntent.putExtra("profilePhoto", profilePhotoUrl);
                        startActivity(chatIntent);
                    }
                });

            }

            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Log.d(TAG, "onCreateViewHolder: create users view holder: ");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_conversation_model, parent, false);
                return new ConvViewHolder(view);
            }
        };



    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

        }

        public void setMessage(String message, boolean isSeen){

            TextView userStatusView = mView.findViewById(R.id.tvMessage);
            userStatusView.setText(message);

            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setName(String name){

            TextView userNameView = mView.findViewById(R.id.tvUserName);
            userNameView.setText(name);

        }

        public void setUserImage(String thumb_image){

            CircleImageView userImageView = mView.findViewById(R.id.circle_profile_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.logo).into(userImageView);

        }

        public void setTimeStamp(String timeStamp) {
            long time = Long.parseLong(timeStamp);
            timeStamp = GetTimeAgo.getTimeAgo(time, mView.getContext());
            TextView tvTimeStamp = mView.findViewById(R.id.tvTimeStamp);
            tvTimeStamp.setText(timeStamp);
        }

//        public void setOnlineStatus(Boolean onlineStatus) {
//            ImageView userOnline = mView.findViewById(R.id.onlineStatus);
//            if(onlineStatus == true) {
//                userOnline.setImageResource(R.drawable.green_dot_online);
//                userOnline.setVisibility(View.VISIBLE);
//            } else if(onlineStatus == false) {
//                userOnline.setImageResource(R.drawable.gray_dot_offline);
//                userOnline.setVisibility(View.VISIBLE);
//            } else {
//                userOnline.setVisibility(View.GONE);
//            }
//        }

    }




























}
