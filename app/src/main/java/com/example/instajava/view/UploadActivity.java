package com.example.instajava.view;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.instajava.databinding.ActivityUploadBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {
    Uri imageData;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    ActivityResultLauncher<Intent> cameraLauncher;

    private ActivityUploadBinding binding;

    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private StorageReference reference;

    private static final int REQUEST_IMAGE = 101;
    private static final int CAMERA_PERMISSION_CODE = 202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        reference = storage.getReference();
    }

    public void onClickedCamera(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        ){

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null){
                Intent intentt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.instajava.provider", photoFile);
                intentt.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                imageData = photoURI;
                cameraLauncher.launch(intentt);
            }
        }else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null){
                    Intent intentt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Uri photoURI = FileProvider.getUriForFile(this, "com.example.instajava.provider", photoFile);
                    intentt.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    imageData = photoURI;
                    cameraLauncher.launch(intentt);
                }

            }else {
                Toast.makeText(this, "Permission was denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void uploadClicked(View view){
        UUID uuid = UUID.randomUUID();
        String imageName = "images/"+ uuid + ".jpg";
        reference.child(imageName).putFile(imageData).addOnSuccessListener(taskSnapshot -> {
            StorageReference newReference = storage.getReference(imageName);
            newReference.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadURL = uri.toString();
                String comment = binding.imgText.getText().toString();

                FirebaseUser user = auth.getCurrentUser();
                String email = user.getEmail();

                HashMap<String, Object> postData = new HashMap<>();
                postData.put("useremail", email);
                postData.put("downloadurl", downloadURL);
                postData.put("comment", comment);
                postData.put("date", FieldValue.serverTimestamp());

                firestore.collection("Posts").add(postData).addOnSuccessListener(documentReference -> {
                    Intent intent = new Intent(UploadActivity.this, FeedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }).addOnFailureListener(e -> {
                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public void imageClicked(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                // show message and ask permission
                Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ask permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();

            }else {
                // ask permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else {
            // Intent
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    private void registerLauncher(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK){
                    Intent intent = result.getData();
                    if (intent != null){
                        imageData = intent.getData();
                        binding.imgUpload.setImageURI(imageData);
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    Intent intent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intent);
                }else {
                    Toast.makeText(UploadActivity.this, "permission needed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                binding.imgUpload.setImageURI(imageData);
            }
        });
    }

    String currentPhotoPath;
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());  // // TODO: 12/8/2021 content provider
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}