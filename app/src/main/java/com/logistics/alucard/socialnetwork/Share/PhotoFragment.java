package com.logistics.alucard.socialnetwork.Share;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.logistics.alucard.socialnetwork.Profile.AccountSettingsActivity;
import com.logistics.alucard.socialnetwork.R;
import com.logistics.alucard.socialnetwork.Utils.Permissions;

import static android.app.Activity.RESULT_OK;

public class PhotoFragment extends Fragment {

    private static final String TAG = "PhotoFragment";

    //constant
    private static final int PHOTO_FRAGMENT_NUM = 1;
    private static final int GALLERY_FRAGMENT_NUM = 2;
    private static final int CAMERA_REQUEST_CODE = 5;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);

        Button btnLaunchCamera = (Button) view.findViewById(R.id.btnLaunchCamera);
        btnLaunchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: launching camera");

                if(((ShareActivity)getActivity()).getCurrentTabNumber() == PHOTO_FRAGMENT_NUM) {
                    if(((ShareActivity)getActivity()).checkPermissions(Permissions.CAMERA_PERMISSION[0])) {
                        Log.d(TAG, "onClick: start camera");
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                    } else {
                        Intent intent = new Intent(getActivity(), ShareActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }

            }
        });

        return view;
    }

    private boolean isRootTask() {
        if(((ShareActivity)getActivity()).getTask() == 0 ) { // define root of fragment
            return true; // root task (ShareActivity) based on intent flag number
        } else {
            return false; // not root task (EditProfileFragment)
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: done taking a photo");
            Log.d(TAG, "onActivityResult: attempting to navigate to share screen");
            //if taking photo is canceled and hit the back button avoid nullPointException
            if(resultCode == RESULT_OK && data.getAction() != null) {
                Bitmap bitmap;
                bitmap = (Bitmap) data.getExtras().get("data"); //"data" keyword argument
                //navigate to final share screen to publish the photo
                if (isRootTask()) {
                    try {
                        Log.d(TAG, "onActivityResult: recieved new bitmap from camera:" + bitmap);
                        Intent intent = new Intent(getActivity(), NextActivity.class);
                        intent.putExtra(getString(R.string.selected_bitmap), bitmap);
                        startActivity(intent);
                    } catch (NullPointerException e) {
                        Log.d(TAG, "onActivityResult: NullPointerException " + e.getMessage());
                    }
                } else {
                    try {
                        Log.d(TAG, "onActivityResult: recieved new bitmap from camera:" + bitmap);
                        Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                        intent.putExtra(getString(R.string.selected_bitmap), bitmap);
                        intent.putExtra(getString(R.string.return_to_fragment), getString(R.string.edit_profile_fragment));
                        startActivity(intent);
                        getActivity().finish();
                    } catch (NullPointerException e) {
                        Log.d(TAG, "onActivityResult: NullPointerException " + e.getMessage());
                    }

                }
            } else {
                getActivity().finish();
            }

        }
    }
}
