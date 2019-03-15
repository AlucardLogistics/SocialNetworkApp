package com.logistics.alucard.socialnetwork.Utils;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.logistics.alucard.socialnetwork.Models.Messages;
import com.logistics.alucard.socialnetwork.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageRecycleAdapter extends RecyclerView.Adapter {

    private static final String TAG = "MessageRecycleAdapter";

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;

    //firebase
    private DatabaseReference mUserDatabase;
    private DatabaseReference mMesageDatabase;
    private DatabaseReference mRootRef;

    private List<Messages> mMessageList;
    private Context mContext;

    public MessageRecycleAdapter(List<Messages> mMessageList, Context mContext) {
        this.mMessageList = mMessageList;
        this.mContext = mContext;
    }

    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType: started");
        Messages messages = mMessageList.get(position);

        String mCurrentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fromUser = messages.getFrom();
        String messageType = messages.getType();

        if(mCurrentUserID.equals(fromUser) && messageType.equals("text")) {
            //if current user is sender and message is type text
            return VIEW_TYPE_MESSAGE_SENT;
        } else if (mCurrentUserID.equals(fromUser) && messageType.equals("image")) {
            //if current user is sender and message is type image
            return VIEW_TYPE_IMAGE_SENT;
        } else if((!mCurrentUserID.equals(fromUser) && messageType.equals("text"))) {
            //if other user is sender and message is type text
            return VIEW_TYPE_MESSAGE_RECEIVED;
        } else {
            //if other user is sender and message is type image
            return VIEW_TYPE_IMAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: started");
        View mView;

        if(viewType == VIEW_TYPE_MESSAGE_SENT) {
            mView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.snippet_item_message_sent, parent, false);
            return new SentMessageHolder(mView);
        } else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            mView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.snippet_item_message_received, parent, false);
            return new ReceivedMessageHolder(mView);
        } else if(viewType == VIEW_TYPE_IMAGE_SENT) {
            mView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.snippet_item_image_sent, parent, false);
            return new SentImageHolder(mView, mContext);
        } else if (viewType == VIEW_TYPE_IMAGE_RECEIVED) {
            mView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.snippet_item_image_received, parent, false);
            return new ReceivedImageHolder(mView, mContext);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: started");

        final Messages message = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_IMAGE_RECEIVED:
                ((ReceivedImageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_IMAGE_SENT:
                ((SentImageHolder) holder).bind(message);
        }

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText, timeText, nameText;
        CircleImageView thumbProfileImage;

        public ReceivedMessageHolder(View itemView) {
            super(itemView);
            this.messageText = itemView.findViewById(R.id.text_message_body);
            this.timeText = itemView.findViewById(R.id.text_message_time);
            this.nameText = itemView.findViewById(R.id.text_message_name);
            this.thumbProfileImage = itemView.findViewById(R.id.image_message_profile);
        }

        void bind(Messages message) {
            Log.d(TAG, "bind: ReceivedMessageHolder started");

            String fromUser = message.getFrom();
            String messageType = message.getType();

            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("user_account_settings").child(fromUser);

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onDataChange: getting user data");

                    String username = dataSnapshot.child("username").getValue().toString();
                    String profile_photo = dataSnapshot.child("profile_photo").getValue().toString();

                    nameText.setText(username);
                    Picasso.get().load(profile_photo).into(thumbProfileImage);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            //determine the type of the message
            if(messageType.equals("text")) {
                messageText.setText(message.getMessage());
            }

            String timeStamp = GetTimeAgo.getTimeAgo(message.getTime(), mContext);
            timeText.setText(timeStamp);

        }

    }

    private class ReceivedImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        private TextView timeText, nameText;
        private CircleImageView thumbProfileImage;
        private ImageView messageImage;
        private Context receivedImagecontext;
        private String imgUrl, imgId;

        public ReceivedImageHolder(View itemView, Context context) {
            super(itemView);
            this.receivedImagecontext = context;
            this.timeText = itemView.findViewById(R.id.image_message_time);
            this.nameText = itemView.findViewById(R.id.image_message_name);
            this.thumbProfileImage = itemView.findViewById(R.id.image_message_profile);
            this.messageImage = itemView.findViewById(R.id.image_message);
            messageImage.setOnClickListener(this);
            messageImage.setOnLongClickListener(this);
        }

        void bind(Messages message) {
            Log.d(TAG, "bind: ReceivedImageHolder started");

            String fromUser = message.getFrom();
            String messageType = message.getType();

            imgUrl = message.getMessage();
            imgId = message.getMessage_id();

            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("user_account_settings").child(fromUser);

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onDataChange: getting user data");

                    String username = dataSnapshot.child("username").getValue().toString();
                    String profile_photo = dataSnapshot.child("profile_photo").getValue().toString();

                    nameText.setText(username);
                    Picasso.get().load(profile_photo).into(thumbProfileImage);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            if(messageType.equals("image")) {
                Picasso.get().load(message.getMessage()).into(messageImage);
                Log.d(TAG, "onBindViewHolder: ImageUrl: " + message.getMessage());
            }

            String timeStamp = GetTimeAgo.getTimeAgo(message.getTime(), receivedImagecontext);
            timeText.setText(timeStamp);

        }

        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: position: " + getAdapterPosition());
            Intent fullScreenIntent = new Intent(view.getContext(), FullScreenImage.class);
            Log.d(TAG, "onClick: imgUrl " + imgUrl);
            fullScreenIntent.putExtra("imgUrl", imgUrl);
            fullScreenIntent.putExtra("imgId", imgId);
            view.getContext().startActivity(fullScreenIntent);
        }

        @Override
        public boolean onLongClick(View view) {
            Log.d(TAG, "onLongClick: longClicked position:" + getAdapterPosition());

            //to do delete sender image?!?

            return false;
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText, timeText;

        public SentMessageHolder(View itemView) {
            super(itemView);
            this.messageText = itemView.findViewById(R.id.text_message_body_sent);
            this.timeText = itemView.findViewById(R.id.text_message_time_sent);
        }

        void bind(Messages message) {
            Log.d(TAG, "bind: SentMessageHolder started");


            String messageType = message.getType();

            //determine the type of the message
            if(messageType.equals("text")) {
                messageText.setText(message.getMessage());
            }

            String timeStamp = GetTimeAgo.getTimeAgo(message.getTime(), mContext);
            timeText.setText(timeStamp);
            Log.d(TAG, "bind: setMessage body and timeStamp: " + timeStamp);
        }
    }

    private class SentImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private TextView timeText;
        private ImageView messageImage;
        private Context sentImageContext;
        private String imgUrl, messageType, imgId, mCurrentUserID, mChatUserID;

        public SentImageHolder(View itemView, Context ctx) {
            super(itemView);
            this.timeText = itemView.findViewById(R.id.image_message_time_sent);
            this.messageImage = itemView.findViewById(R.id.image_message_sent);
            sentImageContext = ctx;
            messageImage.setOnClickListener(this);
            messageImage.setOnLongClickListener(this);
        }

        void bind(Messages message) {
            Log.d(TAG, "bind:SentImageHolder started ");

            messageType = message.getType();
            imgUrl = message.getMessage();
            imgId = message.getMessage_id();
            mCurrentUserID = message.getFrom();
            mChatUserID = message.getTo();


            if (messageType.equals("image")) {
                Picasso.get().load(message.getMessage()).placeholder(R.drawable.logo).into(messageImage);
                Log.d(TAG, "onBindViewHolder: ImageUrl: " + message.getMessage());
            }

            String timeStamp = GetTimeAgo.getTimeAgo(message.getTime(), mContext);
            timeText.setText(timeStamp);

        }


        /**
         * see image in full screen
         * @param view
         */
        @Override
        public void onClick(final View view) {
            Log.d(TAG, "onClick: position is " + getAdapterPosition());
            Intent fullScreenIntent = new Intent(view.getContext(), FullScreenImage.class);
            Log.d(TAG, "onClick: imgUrl " + imgUrl);
            fullScreenIntent.putExtra("imgUrl", imgUrl);
            fullScreenIntent.putExtra("imgId", imgId);
            view.getContext().startActivity(fullScreenIntent);
        }


        @Override
        public boolean onLongClick(final View view) {
            Log.d(TAG, "onLongClick: longClicked position:" + getAdapterPosition());

            mMesageDatabase = FirebaseDatabase.getInstance().getReference().child("messages");
            String mCurrentUserPath = mCurrentUserID + "/" + mChatUserID + "/" + imgId;
            String mChatUserPath = mChatUserID + "/" + mCurrentUserID + "/" + imgId;

            Map deleteImage = new HashMap();
            deleteImage.put(mCurrentUserPath, null);
            deleteImage.put(mChatUserPath, null);

            mMesageDatabase.updateChildren(deleteImage, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        Log.d(TAG, "onComplete: errors " + databaseError.getMessage());
                    }


                    Toast.makeText(view.getContext(), "Image Deleted", Toast.LENGTH_SHORT).show();
                }
            });

            mMessageList.remove(getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
            notifyItemRangeChanged(getAdapterPosition(), mMessageList.size());

            return true;
        }
    }
}
