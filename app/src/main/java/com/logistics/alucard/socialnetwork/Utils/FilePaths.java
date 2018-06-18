package com.logistics.alucard.socialnetwork.Utils;

import android.os.Environment;

public class FilePaths {
    //"storage/emulated/0"
    public String ROOT_DIR = Environment.getExternalStorageDirectory().getPath();

    public String PICTURES = ROOT_DIR + "/Pictures";
    public String CAMERA = ROOT_DIR + "/DCIM/camera";
    public String ALSN = ROOT_DIR + "/Pictures/ALSN/";


    public String FIREBASE_IMAGE_STORAGE = "photos/users/";
    public String FIREBASE_IMAGE_MESSAGES_STORAGE = "photos/message_images/";
}
