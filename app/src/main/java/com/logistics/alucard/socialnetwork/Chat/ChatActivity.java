package com.logistics.alucard.socialnetwork.Chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.logistics.alucard.socialnetwork.Models.Messages;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Utils.FilePaths;
import com.logistics.alucard.socialnetwork.Utils.MessageRecycleAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private static final int GALLERY_PICK = 1;

    //firebase
    private DatabaseReference mRootRef, mUserOnlineRef;
    private FirebaseUser mCurrentUser;
    private FirebaseAuth mAuth;
    private StorageReference mMessageImageStorage;

    //vars
    private String mChatUserID, mChatUserName, mThumbImage,  mCurrentUserID, mCurrentUserName;
    private List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageRecycleAdapter mNewAdapter;
    private double mPhotoUploadProgress = 0;

    //pagination vars
    private int mCurrentPage = 1; // how many messages to load per pagination
    private int itemPos = 0;
    private String mLastKey = ""; //takes the last message loaded key
    private String mPervKey = "";
    private String mFirstKey = "";

    //widgets
    private Toolbar mChatToolbar;
    private TextView mTitleView, mLastSeenView;
    private CircleImageView mProfileImage;
    private ImageButton mChatAddBtn, mChatSendBtn;
    private EditText mChatMessageView;
    private RecyclerView mMessageList;
    private SwipeRefreshLayout mRefreshLayout;
    private ImageView mBackArrow;
    private Context mContext;
    private ProgressBar mProgeressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d(TAG, "onCreate: started");

        //----------------- toolbar setup ------------------
        mChatUserID = getIntent().getStringExtra("followerUserID");
        mChatUserName = getIntent().getStringExtra("followerUsername");
        mThumbImage = getIntent().getStringExtra("profilePhoto");

        mTitleView = findViewById(R.id.custom_bar_title);
        //mLastSeenView = findViewById(R.id.custom_bar_seen);
        mProfileImage = findViewById(R.id.custom_bar_image);
        mBackArrow = findViewById(R.id.ivBackArrow);

        mTitleView.setText(mChatUserName);
        Picasso.get().load(mThumbImage).into(mProfileImage);

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //----------------- widgets setup ------------------

        mChatAddBtn = findViewById(R.id.btnAddChat);
        mChatSendBtn = findViewById(R.id.btnSendChat);
        mChatMessageView = findViewById(R.id.chat_message_view);
        mProgeressBar = findViewById(R.id.mProgreeBar);
        mProgeressBar.setVisibility(View.GONE);

        mNewAdapter = new MessageRecycleAdapter(messagesList, ChatActivity.this);


        mMessageList = findViewById(R.id.messages_list);
        mLinearLayout = new LinearLayoutManager(this);

        //mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);

        mMessageList.setAdapter(mNewAdapter);

        mRefreshLayout = findViewById(R.id.message_swipe_layout);

        //-------------------- firebase setup

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();

        //---------------  Image storage
        mMessageImageStorage = FirebaseStorage.getInstance().getReference();

        loadMessages();

        //create chat node in database
        mRootRef.child("chat").child(mCurrentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: create the chat node");
                if(!dataSnapshot.hasChild(mChatUserID)) {
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("time_stamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("chat/" + mCurrentUserID + "/" + mChatUserID, chatAddMap);
                    chatUserMap.put("chat/" + mChatUserID + "/" + mCurrentUserID, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            Log.d(TAG, "onComplete: add the data to the chat node");

                            if(databaseError != null) {
                                Log.d(TAG, "onComplete: errors found: " + databaseError.getMessage());
                            }

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //set chat room seen field to true for notification fragment bold = false feature
        mRootRef.child("chat").child(mCurrentUserID).child(mChatUserID).child("seen").setValue(true);

        //setup send button
        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: sending message");
                sendMessage();
                mUserOnlineRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
            }
        });

        //share image button
        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: selecting a picture to send");
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);

            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh: started mLastKey is" + mLastKey + "and mPervKey is " + mPervKey);


                if(!mPervKey.equals(mFirstKey)) {

                    mCurrentPage++;

                    itemPos = 0;

                    loadMoreMessages();
                } else {
                    //hide the refresh icon
                    mRefreshLayout.setRefreshing(false);
                }
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: started");

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: ready to upload");

            mProgeressBar.setVisibility(View.VISIBLE);

            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + mCurrentUserID + "/" + mChatUserID;
            final String chatUserRef = "messages/" + mChatUserID  + "/" + mCurrentUserID;

            DatabaseReference userMessagePush = mRootRef.child("messages")
                    .child(mCurrentUserID).child(mChatUserID).push();

            final String pushID = userMessagePush.getKey();
            FilePaths filePaths = new FilePaths();

            final StorageReference filePath = mMessageImageStorage
                    .child(filePaths.FIREBASE_IMAGE_MESSAGES_STORAGE + mCurrentUserID + "/" + pushID + ".jpg");
            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Log.d(TAG, "onComplete: attempt to upload message image to storage");

                    if(task.isSuccessful()) {
                        Log.d(TAG, "onComplete: message image uploaded to storage");
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                               String downloadUrl = uri.toString();

                                Map messageMap = new HashMap();
                                messageMap.put("message_id", pushID);
                                messageMap.put("message", downloadUrl);
                                messageMap.put("seen", false);
                                messageMap.put("type", "image");
                                messageMap.put("time", ServerValue.TIMESTAMP);
                                messageMap.put("from", mCurrentUserID);
                                messageMap.put("to", mChatUserID);

                                Map messageUserMap = new HashMap();
                                messageUserMap.put(currentUserRef + "/" + pushID, messageMap);
                                messageUserMap.put(chatUserRef + "/" + pushID, messageMap);

                                mChatMessageView.setText("");
                                Toast.makeText(ChatActivity.this, "Photo shared", Toast.LENGTH_SHORT).show();

                                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                        shareImageLog(); //register image sharing in log node
                                        mProgeressBar.setVisibility(View.GONE);

                                        if(databaseError != null) {
                                            Log.d(TAG, "onComplete: there was an error: " + databaseError.getMessage());
                                        }
                                    }
                                });
                            }
                        });
                    }

                }
            });


        }
    }

    private void loadMoreMessages() {
        Log.d(TAG, "loadMoreMessages: started");

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUserID);

        //load messages by key by specifying how many to load and provide endAt last message loaded key
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(7);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();


                if(!mPervKey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                } else {
                    mPervKey = mLastKey;
                    mFirstKey = messageKey;

                }

                if(itemPos == 1) {
                    mLastKey = messageKey;
                }

                Log.d("TOTALKEYS", "Last Key : " + mLastKey + " | Prev Key : " + mPervKey + " | Message Key : " + messageKey + " | First Key : " + mFirstKey);


                //hide the refresh icon
                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(6, 0);

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

    }

    private void loadMessages() {
        Log.d(TAG, "loadMessages: started:");

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUserID);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if(itemPos == 1) {
                    String messageKey = dataSnapshot.getKey();
                    Log.d(TAG, "onChildAdded: messageKey" + messageKey);
                    mLastKey = messageKey;
                    mPervKey = messageKey;
                }
                messagesList.add(message);

                //start at the bottom of the recycle view when loaded
                mMessageList.scrollToPosition(messagesList.size() -1);

                //hide the refresh icon
                mRefreshLayout.setRefreshing(false);

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
    }

    private void shareImageLog() {
        Log.d(TAG, "sharePictureLog: started");

        DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child(getString(R.string.dbname_user_account_settings));

        mUserDatabase.child(mCurrentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentUserName  = dataSnapshot.child("username").getValue().toString();

                DatabaseReference shareImageLogPush = mRootRef.child("logs").child(mCurrentUserID).push();
                String logPushId = shareImageLogPush.getKey();
                String logCurrentUser = "You shared an image message with " + mChatUserName + ".";
                String logChatUser = mCurrentUserName +" shared an image message with you.";
                String logCurrentUserRef = "logs/" + mCurrentUserID + "/" + logPushId;
                String logChatUserRef = "logs/" + mChatUserID + "/" + logPushId;

                Map imgCurrentUserLogMap = new HashMap();
                imgCurrentUserLogMap.put("log_id", logPushId);
                imgCurrentUserLogMap.put("time", ServerValue.TIMESTAMP);
                imgCurrentUserLogMap.put("log", logCurrentUser);

                Map imgChatUserLogMap = new HashMap();
                imgChatUserLogMap.put("log_id", logPushId);
                imgChatUserLogMap.put("time", ServerValue.TIMESTAMP);
                imgChatUserLogMap.put("log", logChatUser);

                Map logMap = new HashMap();
                logMap.put(logCurrentUserRef, imgCurrentUserLogMap);
                logMap.put(logChatUserRef, imgChatUserLogMap);

                mRootRef.updateChildren(logMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError != null) {
                            Log.d(TAG, "onComplete: error: " + databaseError.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



    }

    private void sendMessage() {
        Log.d(TAG, "sendMessage: started");
        String message = mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message)) {

            String currentUserRef = "messages/" + mCurrentUserID + "/" + mChatUserID;
            String chatUserRef = "messages/" + mChatUserID + "/" + mCurrentUserID;

            DatabaseReference mUserMessagePush = mRootRef.child("messages")
                    .child(mCurrentUserID).child(mChatUserID).push();

            String pushId = mUserMessagePush.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message_id", pushId);
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);
            messageMap.put("to", mChatUserID);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

            mChatMessageView.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        Log.d(TAG, "onComplete: something went wrong " + databaseError.getMessage());
                    }
                }
            });

            //set seen message to false
            mRootRef.child("chat").child(mChatUserID).child(mCurrentUserID).child("seen").setValue(false);

        }
    }
}
