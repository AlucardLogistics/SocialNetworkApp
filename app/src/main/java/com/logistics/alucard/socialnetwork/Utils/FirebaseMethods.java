package com.logistics.alucard.socialnetwork.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.logistics.alucard.socialnetwork.Home.HomeActivity;
import com.logistics.alucard.socialnetwork.Models.Photo;
import com.logistics.alucard.socialnetwork.Models.User;
import com.logistics.alucard.socialnetwork.Models.UserAccountSettings;
import com.logistics.alucard.socialnetwork.Models.UserSettings;
import com.logistics.alucard.socialnetwork.Profile.AccountSettingsActivity;
import com.logistics.alucard.socialnetwork.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FirebaseMethods {
    private static final String TAG = "FirebaseMethods";

    //firebase auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private StorageReference mStorageReference;

    //vars
    private String userID;
    private Context mContext;
    private double mPhotoUploadProgress = 0;

    public FirebaseMethods (Context context) {
        mAuth = FirebaseAuth.getInstance();
        mContext = context;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        if(mAuth.getCurrentUser() != null) {
            userID = mAuth.getCurrentUser().getUid(); // getUid() - getUserId()
        }
    }

    public void uploadNewPhoto(String photo_type, final String caption, int count, String imgUrl, Bitmap bitmap) {
        Log.d(TAG, "uploadNewPhoto: attempting to upload new photo");
        FilePaths filePaths = new FilePaths();
        //case 1 new photo
        if(photo_type.equals(mContext.getString(R.string.new_photo))) {
            Log.d(TAG, "uploadNewPhoto: uploading NEW photo");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            StorageReference storageReference = mStorageReference
                    .child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/photo" + (count + 1));

            //convert image URL to bitmap
            if(bitmap == null) {
                bitmap = ImageManager.getBitmap(imgUrl);
            }
            byte[] bytes = ImageManager.getBytesFromBitmap(bitmap, 100);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri firebaseUrl = taskSnapshot.getDownloadUrl();
                    //Uri firebaseUrl = taskSnapshot.getStorage().getDownloadUrl().getResult();
                    Log.d(TAG, "onSuccess: fireBaseURL from getStorage().getDownloadUrl().getResult() is: " + firebaseUrl.toString());
                    Toast.makeText(mContext, "Photo Upload Success.", Toast.LENGTH_SHORT).show();
                    //add the new photo to 'photo' node and 'user_photos' node
                    addPhotoToDatabase(caption, firebaseUrl.toString());

                    //navigate to the main feed so the user can see their photo
                    Intent intent = new Intent(mContext, HomeActivity.class);
                    mContext.startActivity(intent);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Photo Upload Failed.");
                    Toast.makeText(mContext, "Failed to upload the photo", Toast.LENGTH_SHORT).show();

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if(progress - 15 > mPhotoUploadProgress) { //display toast every 15 from 100
                        Toast.makeText(mContext, String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }
                    Log.d(TAG, "onProgress: upload progress" + progress + "% done.");
                }
            });


            //case 2 new profile photo
        } else if(photo_type.equals(mContext.getString(R.string.profile_photo))) {
            Log.d(TAG, "uploadNewPhoto: uploading NEW PROFILE photo");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            StorageReference storageReference = mStorageReference
                    .child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/profile_photo");

            //convert image URL to bitmap
            if(bitmap == null) {
                bitmap = ImageManager.getBitmap(imgUrl);
            }
            byte[] bytes = ImageManager.getBytesFromBitmap(bitmap, 100);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri firebaseUrl = taskSnapshot.getDownloadUrl();
                    //Uri firebaseUrl = taskSnapshot.getStorage().getDownloadUrl().getResult();
                    Log.d(TAG, "onSuccess: fireBaseURL from getStorage().getDownloadUrl().getResult() is: " + firebaseUrl.toString());
                    Toast.makeText(mContext, "Photo Upload Success.", Toast.LENGTH_SHORT).show();

                    //insert into 'user_account_settings' node
                    setProfilePhoto(firebaseUrl.toString());

                    //after profile is updated set the current fragment to EditProfileFragment
                    ((AccountSettingsActivity)mContext).setViewPager(
                            ((AccountSettingsActivity)mContext).pagerAdapter
                                    .getFragmentNumber(mContext.getString(R.string.edit_profile_fragment))
                    );

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Photo Upload Failed.");
                    Toast.makeText(mContext, "Failed to upload the photo", Toast.LENGTH_SHORT).show();

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if(progress - 15 > mPhotoUploadProgress) { //display toast every 15 from 100
                        Toast.makeText(mContext, String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }
                    Log.d(TAG, "onProgress: upload progress" + progress + "% done.");
                }
            });




        }
    }

    private void setProfilePhoto(String url) {
        Log.d(TAG, "setProfilePhoto: setting new profile image" + url);

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mContext.getString(R.string.profile_photo))
                .setValue(url);
    }

    private String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("US/Central"));
        return sdf.format(new Date());
    }

    private void addPhotoToDatabase(String caption, String url) {
        Log.d(TAG, "addPhotoToDatabase: adding photo to database");

        String tags = StringManipulation.getTags(caption);
        String newPhotoKey = myRef.child(mContext.getString(R.string.dbname_photos)).push().getKey();
        Photo photo = new Photo();
        photo.setCaption(caption);
        photo.setDate_created(getTimeStamp());
        photo.setImage_path(url);
        photo.setTags(tags);
        photo.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        photo.setPhoto_id(newPhotoKey);

        //insert into database
        myRef.child(mContext.getString(R.string.dbname_user_photos))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(newPhotoKey).setValue(photo);
        myRef.child(mContext.getString(R.string.dbname_photos))
                .child(newPhotoKey).setValue(photo);

    }

    /**
     * gets the count of pictures in Firebase under
     * 'user_photos node' -> userID -> pictures
     * @param dataSnapshot
     * @return
     */
    public int getImageCount(DataSnapshot dataSnapshot) {
        int count = 0;
        for(DataSnapshot ds: dataSnapshot
                .child(mContext.getString(R.string.dbname_user_photos))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .getChildren()){
            count ++;
        }
        return count;
    }

//    public boolean checkIfUsernameExists(String username, DataSnapshot dataSnapshot) {
//        AppLogs.d(TAG, "checkIfUsernameExists: check if " + username + " already exists!");
//
//        User user = new User();
//        for(DataSnapshot ds : dataSnapshot.child(userID).getChildren()) {
//            AppLogs.d(TAG, "checkIfUsernameExists: dataSnapshot: " + ds);
//
//            user.setUsername(ds.getValue(User.class).getUsername());
//            AppLogs.d(TAG, "checkIfUsernameExists: username: " + user.getUsername());
//            //StringManipulation is a custom class in Utils package
//            if(StringManipulation.expandUsername(user.getUsername()).equals(username)) {
//                AppLogs.d(TAG, "checkIfUsernameExists: FOUND A MATCH:" + user.getUsername());
//                return true;
//            }
//        }
//        return false;
//    }

    // register a new user to firebase authentication
    public void registerNewEmail(final String email, String password, final String username) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail onComplete: " + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail: failed" + task.getException());
                            Toast.makeText(mContext, R.string.auth_failed, Toast.LENGTH_SHORT).show();

                        } else if(task.isSuccessful()) {
                            //send verification email
                            sendVerificationEmail();
                            userID = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "onComplete: Authstate changed: " + userID);

                        }

                        // ...
                    }
                });
    }

    public void sendVerificationEmail() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                Log.d(TAG, "sendVerificationEmail: verification Email was sent to:" + user.getEmail());
                            }   else {
                                Toast.makeText(mContext, "Couldn't send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * add information to the users nodes
     * add information to the user_account_settings nodes
     * @param email
     * @param username
     * @param description
     * @param website
     * @param profile_photo
     */
    public void addNewUser(String email, String username, String description, String website, String profile_photo) {
        User user = new User(userID, 1, email, StringManipulation.condenseUsername(username));
        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID).setValue(user);

        UserAccountSettings userAccountSettings = new UserAccountSettings(
                description, username, 0, 0, 0, 1,
                profile_photo, StringManipulation.condenseUsername(username), website, userID
        );

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID).setValue(userAccountSettings);

    }

    /**
     * Update 'user_account_settings' node for the current user
     * @param displayName
     * @param website
     * @param description
     * @param phoneNumber
     */
    public void updateUserAccountSettings(String displayName, String website, String description, long phoneNumber) {
        Log.d(TAG, "updateUserAccountSettings: update the user account settings node in firebase");
        if(displayName != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_display_name))
                    .setValue(displayName);
        }

        if(website != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_website))
                    .setValue(website);
        }

        if(description != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_description))
                    .setValue(description);
        }

        if(phoneNumber != 1) {
            Log.d(TAG, "updateUserAccountSettings: ********phoneNumber is : " + phoneNumber);
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_phone_number))
                    .setValue(phoneNumber);
            myRef.child(mContext.getString(R.string.dbname_users))
                    .child(userID)
                    .child(mContext.getString(R.string.field_phone_number))
                    .setValue(phoneNumber);
        }
    }

    /**
     * Update the username in the 'users' and 'user_account_settings' node
     * @param username
     */
    public void updateUsername(String username) {
        Log.d(TAG, "updateUsername: updating username to " + username);

        //set username to lowercase for the search activity
        String lowerCaseUsername = username.toLowerCase();

        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(lowerCaseUsername);

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(lowerCaseUsername);
    }

    /**
     * Update the email in the 'users' node
     * @param email
     */
    public void updateEmail(String email) {
        Log.d(TAG, "updateEmail: updating email to " + email);
        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_email))
                .setValue(email);

    }

    /**
     * Retrieves the account settings for the user currently logged in
     * Database: user_account_settings node
     * @param dataSnapshot
     * @return
     */
    public UserSettings getUserSettings(DataSnapshot dataSnapshot) {
        Log.d(TAG, "getUserAccountSettings: retrieving user account settings from firebase");

        UserAccountSettings userAccountSettings = new UserAccountSettings();
        User user = new User();

        for(DataSnapshot ds : dataSnapshot.getChildren()) { //loop trough main nodes ("user_account_settings" and "users")
            //user_account_setting node
            if(ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings))) {
                Log.d(TAG, "getUserAccountSettings: dataSnapshot: " + ds);
                try {
                    userAccountSettings.setDisplay_name(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDisplay_name()
                    );
                    userAccountSettings.setUsername(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUsername()
                    );
                    userAccountSettings.setWebsite(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getWebsite()
                    );
                    userAccountSettings.setDescription(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDescription()
                    );
                    userAccountSettings.setPosts(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPosts()
                    );
                    userAccountSettings.setProfile_photo(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getProfile_photo()
                    );
                    userAccountSettings.setFollowing(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowing()
                    );
                    userAccountSettings.setFollowers(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowers()
                    );
                    Log.d(TAG, "getUserAccountSettings: retrieved user_account_settings information " + userAccountSettings.toString());
                } catch (NullPointerException e) {
                    Log.d(TAG, "getUserAccountSettings: NullPointerException: " + e.getMessage());
                }
            }

            //users node
            if(ds.getKey().equals(mContext.getString(R.string.dbname_users))) {
                Log.d(TAG, "getUserAccountSettings: dataSnapshot: " + ds);
                try {
                    user.setEmail(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getEmail()
                    );
                    user.setUsername(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getUsername()
                    );
                    user.setPhone_number(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getPhone_number()
                    );
                    user.setUser_id(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getUser_id()
                    );
                    Log.d(TAG, "getUserAccountSettings: retrieved user information " + user.toString());
                } catch (NullPointerException e) {
                    Log.d(TAG, "getUserAccountSettings: NullPointerException: " + e.getMessage());
                }
            }
        }
        return new UserSettings(user, userAccountSettings);
    }
}
