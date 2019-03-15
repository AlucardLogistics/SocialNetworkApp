package com.logistics.alucard.socialnetwork.Utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.logistics.alucard.socialnetwork.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FullScreenImage extends AppCompatActivity {
    private static final String TAG = "FullScreenImage";

    private ImageView ivFullScreen;
    private Button btnClose;

    private String imgUrl, imgId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ivFullScreen = findViewById(R.id.imgDisplay);
        btnClose = findViewById(R.id.btnClose);

        imgUrl = getIntent().getStringExtra("imgUrl");
        imgId = getIntent().getStringExtra("imgId");


        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullScreenImage.this.finish();
            }
        });

        Picasso.get().load(imgUrl).into(ivFullScreen);

        ivFullScreen.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                saveImage();
                Toast.makeText(FullScreenImage.this, "Image Saved to Phone", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void saveImage() {
        ivFullScreen.buildDrawingCache();
        Bitmap bitmap = ivFullScreen.getDrawingCache();
        FilePaths filePaths = new FilePaths();

        OutputStream fOut = null;
        Uri outputFileUri;
        try {
            File root = new File(filePaths.ALSN);
            root.mkdirs();
            File sdImageMainDirectory = new File(root, imgId + ".jpg");
            outputFileUri = Uri.fromFile(sdImageMainDirectory);
            fOut = new FileOutputStream(sdImageMainDirectory);
            Log.d(TAG, "saveImage: saved into: " + filePaths.ALSN);
        } catch (Exception e) {
            Log.d(TAG, "saveImage: error: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.close();
            fOut.flush();
        } catch (Exception e) {
            Log.d(TAG, "saveImage: error: " + e.getMessage());
        }

    }
}