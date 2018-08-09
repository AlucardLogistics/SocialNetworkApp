package com.logistics.alucard.socialnetwork.Profile;




import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.logistics.alucard.socialnetwork.Dialogs.ConfirmPasswordDialog;
import com.logistics.alucard.socialnetwork.Models.User;
import com.logistics.alucard.socialnetwork.Models.UserAccountSettings;
import com.logistics.alucard.socialnetwork.Models.UserSettings;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Share.NextActivity;
import com.logistics.alucard.socialnetwork.Share.ShareActivity;
import com.logistics.alucard.socialnetwork.Utils.FirebaseMethods;
import com.logistics.alucard.socialnetwork.Utils.UniversalImageLoader;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment implements ConfirmPasswordDialog.OnConfirmPasswordListener {

    @Override
    public void onConfirmPassword(String password) {
        Log.d(TAG, "onConfirmPassword: password was captured");

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.
        AuthCredential credential = EmailAuthProvider
                .getCredential(mAuth.getCurrentUser().getEmail(), password);

        // Prompt the user to re-provide their sign-in credentials
        mAuth.getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated.");

                            //check to see if new email is not taken
                            mAuth.fetchSignInMethodsForEmail(mEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                @Override
                                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                    if(task.isSuccessful()) {
                                        try {
                                            if(task.getResult().getSignInMethods().isEmpty()) {
                                                Log.d(TAG, "onComplete: email is available: " + mEmail.getText().toString());

                                                // the email is available to update
                                                mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d(TAG, "User email address updated. " + user.getEmail());
                                                                    Toast.makeText(getActivity(), "Email Updated.", Toast.LENGTH_SHORT).show();
                                                                    mFirebaseMethods.updateEmail(mEmail.getText().toString());
                                                                }
                                                            }
                                                        });

                                            } else if(!task.getResult().getSignInMethods().isEmpty()){
                                                Log.d(TAG, "onComplete: email is already in use !" + mEmail.getText().toString());
                                                Toast.makeText(getActivity(), "Email is already in use.", Toast.LENGTH_SHORT).show();

                                            }
                                        } catch (NullPointerException e) {
                                            Log.e(TAG, "onComplete: NullPointerException " + e.getMessage() );
                                        }

                                    }
                                }
                            });


                        } else {
                            Log.d(TAG, "onComplete: Re-Authentication failed.");
                        }
                    }
                });

    }

    private static final String TAG = "EditProfileFragment";

    //firebase auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private String userID;

    //EditProfileFragment widgets
    private CircleImageView mProfilePhoto;
    private EditText mDisplayName, mUsername, mWebsite, mDescription, mEmail, mPhoneNumber;
    private TextView mChangeProfilePhoto;

    //variables
    private UserSettings mUserSettings;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        mProfilePhoto = (CircleImageView) view.findViewById(R.id.profilePhoto);
        mDisplayName = (EditText) view.findViewById(R.id.displayName);
        mUsername = (EditText) view.findViewById(R.id.username);
        mWebsite = (EditText) view.findViewById(R.id.website);
        mDescription = (EditText) view.findViewById(R.id.description);
        mEmail = (EditText) view.findViewById(R.id.email);
        mPhoneNumber = (EditText) view.findViewById(R.id.phoneNumber);
        mChangeProfilePhoto = (TextView) view.findViewById(R.id.changeProfilePhoto);
        mFirebaseMethods = new FirebaseMethods(getActivity());

        setupFirebaseAuth();


        //back arrow for navigation to profile activity
        ImageView backArrow = (ImageView) view.findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back to ProfileActivity");
                getActivity().finish();
            }
        });

        ImageView checkmark = (ImageView) view.findViewById(R.id.saveChanges);
        checkmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: attempt to save changes");
                saveProfileSettings();
                updateProfileLog();
            }
        });


        return view;


    }

    private void updateProfileLog() {
        Log.d(TAG, "updateProfileLog: started");


        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        String mCurrentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference shareImageLogPush = mRootRef.child("logs").child(mCurrentUserID).push();
        String logPushId = shareImageLogPush.getKey();
        String logCurrentUser = "You updated your profile.";
        String logCurrentUserRef = "logs/" + mCurrentUserID + "/" + logPushId;

        Map imgCurrentUserLogMap = new HashMap();
        imgCurrentUserLogMap.put("log_id", logPushId);
        imgCurrentUserLogMap.put("time", ServerValue.TIMESTAMP);
        imgCurrentUserLogMap.put("log", logCurrentUser);

        Map logMap = new HashMap();
        logMap.put(logCurrentUserRef, imgCurrentUserLogMap);

        mRootRef.updateChildren(logMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError != null) {
                    Log.d(TAG, "onComplete: error: " + databaseError.getMessage());
                }
            }
        });
    }

//    private void setProfileImage() {
//        AppLogs.d(TAG, "setProfileImage: setting profile image");
//        String imgUrl = "https://avatarfiles.alphacoders.com/717/71761.jpg";
//        UniversalImageLoader.setImage(imgUrl, mProfilePhoto, null, "");
//    }

    /**
     * Retrieves the data contained on the widgets and submits it to the database
     * Before doing so it checks to make sure the username chosen is unique
     */
    private void saveProfileSettings() {
        Log.d(TAG, "saveProfileSettings: started");
        final String displayName = mDisplayName.getText().toString();
        final String username = mUsername.getText().toString();
        final String website = mWebsite.getText().toString();
        final String description = mDescription.getText().toString();
        final String email = mEmail.getText().toString();
        final long phoneNumber = Long.parseLong(mPhoneNumber.getText().toString());
        Log.d(TAG, "saveProfileSettings: ////////phoneNumber is: " + phoneNumber);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                //case 1: if the user made a change to the username
                if(!mUserSettings.getUser().getUsername().equals(username)) {

                    checkIfUsernameExists(username);
                }
                //case 2: if the user made a change to their email
                if(!mUserSettings.getUser().getEmail().equals(email)) {
                    //step 1 Re-authenticate
                    //          -Confirm the password and email
                    ConfirmPasswordDialog dialog = new ConfirmPasswordDialog();
                    dialog.show(getFragmentManager(), getString(R.string.confirm_password_dialog));
                    //pass data directly to the EditProfileFragment trough targetFragment method
                    dialog.setTargetFragment(EditProfileFragment.this, 1);
                    //step 2 check if the email is already registered
                    //          -fetchProvidersForEmail(String email) - check to see if email is available
                    //step 3 change the email
                    //          -submit the new email to the database and authentication

                }
                /**
                 * change the rest of the settings that do not require uniqueness
                 */
                if(!mUserSettings.getUserAccountSettings().getDisplay_name().equals(displayName)) {
                    //update display name
                    mFirebaseMethods.updateUserAccountSettings(displayName, null, null, 0);
                }
                if(!mUserSettings.getUserAccountSettings().getWebsite().equals(website)) {
                    //update website
                    mFirebaseMethods.updateUserAccountSettings(null, website, null, 0);
                }
                if(!mUserSettings.getUserAccountSettings().getDescription().equals(description)) {
                    //update description
                    mFirebaseMethods.updateUserAccountSettings(null, null, description, 0);
                }
                if(mUserSettings.getUser().getPhone_number() != phoneNumber) {
                    //update phone number
                    mFirebaseMethods.updateUserAccountSettings(null, null, null, phoneNumber);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Check if @param username exists in the database
     * @param username
     */
    private void checkIfUsernameExists(final String username) {
        Log.d(TAG, "checkIfUsernameExists: Check if " + username + " already exists!");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query  query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    //add username
                    mFirebaseMethods.updateUsername(username);
                    Toast.makeText(getActivity(), "Username was changed!", Toast.LENGTH_SHORT).show();
                }
                for(DataSnapshot singleSnampshot : dataSnapshot.getChildren()) {
                    if(singleSnampshot.exists()) {
                        Log.d(TAG, "onDataChange: USERNAME" + singleSnampshot.getValue(User.class).getUsername() + "ALREADY EXISTS");
                        Toast.makeText(getActivity(), "Username already exists! Please choose a different one.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void setProfileWidgets(UserSettings userSettings) {
        Log.d(TAG, "setProfileWidgets: setting widgets with data from FireBase");

        mUserSettings = userSettings;

        //User user = userSettings.getUser();
        UserAccountSettings userAccountSettings = userSettings.getUserAccountSettings();

        UniversalImageLoader.setImage(userAccountSettings.getProfile_photo(), mProfilePhoto, null, "");
        mDisplayName.setText(userAccountSettings.getDisplay_name());
        mUsername.setText(userAccountSettings.getUsername());
        mWebsite.setText(userAccountSettings.getWebsite());
        mDescription.setText(userAccountSettings.getDescription());
        mEmail.setText(userSettings.getUser().getEmail());
        mPhoneNumber.setText(String.valueOf(userSettings.getUser().getPhone_number()));

        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: attempt to update profile photo");

                Intent intent = new Intent(getActivity(), ShareActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //268435456
                // Flag returns a number so is not 0 or null
                //this intent will be caught in GalleryFragment when nextScreen text view is clicked
                getActivity().startActivity(intent);
                getActivity().finish();


            }
        });
    }

    /*
     * ------------------------- FIREBASE SETUP ------------------------------
     * */

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase Auth");
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                Log.d(TAG, "onAuthStateChanged: myRef value : " + myRef.toString());

                if(user != null) {
                    //User is signed in
                    Log.d(TAG, "onAuthStateChanged: signed_in" + user.getUid());
                } else {
                    //User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                }
            }
        };

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //retrieve user information from database
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));

                //retrieve images for the user in question
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
