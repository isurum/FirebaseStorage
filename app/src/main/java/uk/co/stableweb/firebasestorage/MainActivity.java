package uk.co.stableweb.firebasestorage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.*;
import android.Manifest;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {

    // Initialization of views
    private EditText titleEditText;
    private EditText descriptionEditText;
    private ImageButton selectedImage;
    private Button submitBtn;
    private ProgressDialog progressDialog;

    // Stores the selected image Uri
    private Uri selectedImageUri = null;
    private static final int GALLERY_REQUEST = 1;

    // Initialization of the FirebaseStorage variable
    private StorageReference firebaseStorage;
    // Initialization of FirebaseDatabase reference
    private DatabaseReference databaseReference;
    //Initialization of FirebaseAuth variable
    private FirebaseAuth auth;
    // Initialization of User database reference
    private DatabaseReference userDatabaseRef;
    // AuthStateListener variable
    private FirebaseAuth.AuthStateListener mAuthListener;

    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the FirebaseAuth instance
        auth = FirebaseAuth.getInstance();

        // Start signing the user annonymously
        // Make sure anonymous SIGN-IN-METHOD is enabled
        signInAnonymously();

        // Get the instance of the FirebaseStorage
        firebaseStorage = FirebaseStorage.getInstance().getReference();

        //capture view objects from the layout
        selectedImage = (ImageButton) findViewById(R.id.add_image_btn);
        titleEditText = (EditText) findViewById(R.id.title_et);
        descriptionEditText = (EditText) findViewById(R.id.desc_et);
        submitBtn = (Button) findViewById(R.id.submit_btn);
        progressDialog = new ProgressDialog(this);

        // Get the reference to the "photos" folder
        databaseReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("photos");

        // [START auth_state_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in: " +
                            user.getUid());

                    // Get the reference to the "users" database path
                    userDatabaseRef = FirebaseDatabase.getInstance()
                            .getReference()
                            .child("users")
                            .child(auth.getCurrentUser().getUid());


                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };
        // [END auth_state_listener]

        // Checks the SDK version and manage new permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.MEDIA_CONTENT_CONTROL},
                    100);
        }

        // set the on click listener to the selectedImage
        selectedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to get the gallery image
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });

        // set the on click listener to the submit button
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // invoke postImage() method
                postImage();
            }
        });


    }

    private void postImage() {

        // show the progress dialog
        progressDialog.setMessage("Uploading....");
        progressDialog.show();

        // get the user inputs
        final String titleValue = titleEditText.getText().toString();
        final String descriptionValue = descriptionEditText.getText().toString();

        //validate titleValue and descriptionValue
        if(!TextUtils.isEmpty(titleValue)
                && !TextUtils.isEmpty(descriptionValue)
                && selectedImageUri != null){

            // Checks the SDK version and manage new permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.INTERNET,
                                Manifest.permission.MEDIA_CONTENT_CONTROL},
                        100);
            }else {

                // Storage path of the photos
                StorageReference filePath = firebaseStorage.child("photos")
                        .child(selectedImageUri.getLastPathSegment());

                // upload the file using putFile() method
                filePath.putFile(selectedImageUri).addOnSuccessListener(
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        // Gets the download URL of the uploaded image
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        // Creates new post reference
                        final DatabaseReference newPost = databaseReference.push();

                        userDatabaseRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Add following data to the database
                                newPost.child("title")
                                        .setValue(titleValue);
                                newPost.child("desc")
                                        .setValue(descriptionValue);
                                newPost.child("image")
                                        .setValue(downloadUrl.toString());
                                newPost.child("uploaded_time")
                                        .setValue(ServerValue.TIMESTAMP);
                                newPost.child("uid")
                                        .setValue(auth.getCurrentUser().getUid());

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d(TAG, "Image Upload cancelled");

                            }
                        });

                        progressDialog.dismiss();

                        /*// Start the MainActivity after the posting of image.
                        startActivity(new Intent(PostImageActivity.this, MainActivity.class));*/
                    }
                });
            }
        }

    }

    private void signInAnonymously() {
        Toast.makeText(
          getApplicationContext(),
                "Signing Annonymously...",
                Toast.LENGTH_SHORT
        ).show();
        // [START signin_anonymously]
        auth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(
                                TAG,
                                "signInAnonymously:onComplete:"
                                        + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(
                                    TAG,
                                    "signInAnonymously",
                                    task.getException());
                            Toast.makeText(
                                    MainActivity.this,
                                    "Authentication failed.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        // [START_EXCLUDE]
                        Toast.makeText(
                                getApplicationContext(),
                                "Finished Signing Annonymously...",
                                Toast.LENGTH_SHORT
                        ).show();
                        // [END_EXCLUDE]
                    }
                });
        // [END signin_anonymously]
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check the requestCode
        if(requestCode == GALLERY_REQUEST
                && resultCode == RESULT_OK){

            // Store the path of the selected image
            selectedImageUri = data.getData();

            // Set the ImageButton to the path value
            selectedImage.setImageURI(selectedImageUri);

            // Disable the ImageButton
            selectedImage.setClickable(false);
        }else{
            // If the requestCode is different
            // set the image to default value
            selectedImage
                    .setBackground(getResources()
                            .getDrawable(R.mipmap.add_btn));
        }
    }

    // [START on_start_add_listener]
    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(mAuthListener);
    }
    // [END on_start_add_listener]

    // [START on_stop_remove_listener]
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            auth.removeAuthStateListener(mAuthListener);
        }
    }

}
