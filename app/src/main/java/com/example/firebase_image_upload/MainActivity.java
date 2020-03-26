package com.example.firebase_image_upload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    int PICK_IMAGE = 1000;
    ImageView imageview, image_resize;
    Button btn_choose, btn_upload;
    Uri filePath = null;
    Uri imgPath = null;
    String download_url = null;
    Bitmap bm, converetdImage, centerBitmap;
    StorageReference storageReference;
    ProgressDialog progressDialog;
    ByteArrayOutputStream bytearrayoutputstream = null;
    DatabaseReference databaseReference;
    String path = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        imageview = (ImageView) findViewById(R.id.imageview);
        image_resize = (ImageView) findViewById(R.id.image_resize);
        btn_choose = (Button) findViewById(R.id.btn_choose);
        btn_upload = (Button) findViewById(R.id.btn_upload);

        bytearrayoutputstream = new ByteArrayOutputStream();

        btn_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose_image();
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

    }

    private void uploadImage() {
        if (imgPath != null) {

            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            final StorageReference sRef = storageReference.child("User_Profile/" + "User_" + System.currentTimeMillis());

            sRef.putFile(imgPath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();

                            sRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {

                                    if (task.isSuccessful()) {
                                        download_url = task.getResult().toString();
                                        Log.e(TAG, "onComplete:========>>><<<<" + download_url);
                                        String push_key = databaseReference.push().getKey().toString();
                                        databaseReference.getRoot().child("User_Profile").child(push_key).setValue(download_url);

                                        Glide.with(MainActivity.this)
                                                .load(download_url)
                                                .into(image_resize);
                                    }
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Image Upload Fail!" + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "onFailure:====>>><<<" + e.getMessage());
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.setMessage("Uploading " + taskSnapshot.getBytesTransferred() / 1024 + " / " + taskSnapshot.getTotalByteCount() / 1024 + " KB");
                        }
                    });
        }
    }

    private void choose_image() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            switch (requestCode) {
                case 1000:
                    filePath = data.getData();
                    imageview.setImageURI(filePath);
                    bytearrayoutputstream = null;
                    imgPath = getResizedBitmap(imageview);
                    break;
            }
        } else {
            Toast.makeText(this, "Image Not Selected!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public Uri getResizedBitmap(ImageView imageview) {

        //get bitmap from uri
        bm = ((BitmapDrawable) imageview.getDrawable()).getBitmap();

        //center crop the bitmap
        int dimension = Math.min(bm.getWidth(), bm.getHeight());
        centerBitmap = ThumbnailUtils.extractThumbnail(bm, dimension, dimension);

        // scale the bitmap
        converetdImage = Bitmap.createScaledBitmap(centerBitmap, 500, 500, true);
        path = null;

        //convert bitmap to uri
        path = MediaStore.Images.Media.insertImage(getContentResolver(), converetdImage, "title", null);
        return Uri.parse(path);
    }
}
