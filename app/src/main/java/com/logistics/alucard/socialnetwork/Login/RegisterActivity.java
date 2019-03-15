package com.logistics.alucard.socialnetwork.Login;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.logistics.alucard.socialnetwork.Models.User;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Utils.FirebaseMethods;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    @BindView(R.id.input_email)
    EditText mEmail;
    @BindView(R.id.input_username)
    EditText mUsername;
    @BindView(R.id.input_password)
    EditText mPassword;
    @BindView(R.id.btn_register)
    AppCompatButton btnRegister;
    @BindView(R.id.pleaseWait)
    TextView mPleaseWait;

    private Context mContext;
    private ProgressBar mProgressBar;
    private String email = "", password = "", username = "";
    //string to append to username in case it already exists
    private String append = "";


    //firebase auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    //custom class in Utils for firebase methods creation
    private FirebaseMethods firebaseMethods;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        mContext = RegisterActivity.this;
        firebaseMethods = new FirebaseMethods(mContext);
        Log.d(TAG, "onCreate: register activity started");

        initWidgets();
        setupFirebaseAuth();
        registerUser();
    }

    @OnClick(R.id.btn_register)
    public void registerUser() {
        email = mEmail.getText().toString();
        username = mUsername.getText().toString();
        password = mPassword.getText().toString();

        if (checkInputs(email, username, password)) {
            mProgressBar.setVisibility(View.VISIBLE);
            mPleaseWait.setVisibility(View.VISIBLE);

            firebaseMethods.registerNewEmail(email, password, username);
        }
    }


    //check if any fields are empty and display message if any is
    private boolean checkInputs(String email, String username, String password) {
        Log.d(TAG, "checkInputs: checking inputs for null values");
        if (email.equals("") || username.equals("") || password.equals("")) {
            Toast.makeText(mContext, "All fields must be filled out", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /*initialize the activity widgets*/
    private void initWidgets() {
        Log.d(TAG, "initWidgets: Initializing Widgets.");

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
//        mPleaseWait = (TextView) findViewById(R.id.pleaseWait);
//        mEmail = (EditText) findViewById(R.id.input_email);
//        mUsername = (EditText) findViewById(R.id.input_username);
//        mPassword = (EditText) findViewById(R.id.input_password);
//        btnRegister = (Button) findViewById(R.id.btn_register);
        mContext = RegisterActivity.this;

        mProgressBar.setVisibility(View.GONE);
        mPleaseWait.setVisibility(View.GONE);
    }

    private boolean isStringNull(String string) {
        Log.d(TAG, "isStringNull: check if string is null");

        if (string.equals("")) {
            return true;
        } else {
            return false;
        }
    }


    /*
     * ------------------------- FIREBASE SETUP ------------------------------
     * */

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up FireBase Auth");

        mAuth = FirebaseAuth.getInstance();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    //User signed in
                    Log.d(TAG, "onAuthStateChanged: signed_in " + user.getUid());
                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) { //update database success
                            checkIfUsernameExists(username);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    //finish the activity and navigate back to previous activity (LoginActivity)
                    finish();

                } else {
                    //User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                }
            }
        };
    }

    /**
     * Check if @param username exists in the database
     *
     * @param username
     */
    private void checkIfUsernameExists(final String username) {
        Log.d(TAG, "checkIfUsernameExists: Check if " + username + " already exists!");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot singleSnampshot : dataSnapshot.getChildren()) {
                    if (singleSnampshot.exists()) {
                        Log.d(TAG, "onDataChange: USERNAME" + singleSnampshot.getValue(User.class).getUsername() + "ALREADY EXISTS");
                        append = myRef.push().getKey().substring(3, 7);
                        Log.d(TAG, "onDataChange: username already exists. Appending random string to name " + append);
                    }
                }

                String mUsername = "";
                mUsername = username + append;

                //add new user to the database
                firebaseMethods.addNewUser(email, mUsername, "", "", "");
                //AppLogs.d(TAG, "onDataChange:addNewUser INFO to database " + dataSnapshot.getValue(User.class).getUser_id());
                Toast.makeText(mContext, "Signup successful. Sending verification email.", Toast.LENGTH_SHORT).show();
                //sign out and go to email verification
                mAuth.signOut();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
