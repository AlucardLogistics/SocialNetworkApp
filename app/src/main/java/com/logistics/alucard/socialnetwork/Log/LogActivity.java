package com.logistics.alucard.socialnetwork.Log;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.logistics.alucard.socialnetwork.Models.AppLogs;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Utils.BottomNavigationViewHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogActivity extends AppCompatActivity {
    private static final String TAG = "LogActivity";
    private static final int ACTIVITY_NUM = 3;

    //widgets
    private Context mContext = LogActivity.this;
    private RecyclerView mLogRecycleView;

    //firebase
    private DatabaseReference mLogDatabase;
    private FirebaseAuth mAuth;

    //adapter
    private FirebaseRecyclerAdapter adapter;

    //vars
    private String mCurrentUserID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        Log.d(TAG, "onCreate: Started");

        mCurrentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mLogRecycleView = findViewById(R.id.recyclerList);
        mLogDatabase = FirebaseDatabase.getInstance().getReference().child("logs").child(mCurrentUserID);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mLogRecycleView.setHasFixedSize(true);
        mLogRecycleView.setLayoutManager(linearLayoutManager);

        firebaseListAdapter();

        mLogRecycleView.setAdapter(adapter);

        setupBottomNavigationView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void firebaseListAdapter() {
        Log.d(TAG, "firebaseListAdapter: started");

        Query logQuery = mLogDatabase.orderByChild("time");

        FirebaseRecyclerOptions<AppLogs> options =
                new FirebaseRecyclerOptions.Builder<AppLogs>()
                .setQuery(logQuery, AppLogs.class).build();

        adapter = new FirebaseRecyclerAdapter<AppLogs, LogViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final LogViewHolder holder, int position, @NonNull AppLogs model) {
                Log.d(TAG, "onBindViewHolder: started");

                //get the ID of the messages
                final String logID = getRef(position).getKey();

                mLogDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        String log = dataSnapshot.child(logID).child("log").getValue().toString();
                        String timeStamp = dataSnapshot.child(logID).child("time").getValue().toString();
                        Log.d(TAG, "onDataChange: logs:--------------> " + log);

                        holder.setLog(log);
                        holder.setTimeStamp(timeStamp);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Log.d(TAG, "onCreateViewHolder: create users view holder: ");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_log_list_view, parent, false);
                return new LogViewHolder(view);
            }
        };

    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public LogViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public void setLog(String log) {
            TextView tvLog = mView.findViewById(R.id.tvLog);
            tvLog.setText(log);
        }

        public void setTimeStamp(String timeStamp) {

            long longTimeStamp = Long.parseLong(timeStamp);
            Date time = new Date(longTimeStamp);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
            timeStamp = sdf.format(time);
            TextView tvTimeStamp = mView.findViewById(R.id.tvTimeStamp);
            tvTimeStamp.setText(timeStamp);
        }

    }



    /*
     *BottomNavigationView Setup
     */
    private void setupBottomNavigationView() {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);
        BottomNavigationViewHelper.enableNavigation(mContext, this, bottomNavigationViewEx);
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }
}
