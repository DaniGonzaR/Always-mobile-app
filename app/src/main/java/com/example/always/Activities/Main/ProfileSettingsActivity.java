package com.example.always.Activities.Main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.always.Activities.Account.LoginActivity;
import com.example.always.R;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private FirebaseFirestore db;
    private StorageReference mStorageRef;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        Button btnResetPassword = findViewById(R.id.btn_reset_password);
        Button btnUpdateEmail = findViewById(R.id.btn_update_email);
        Button btnEditDescription = findViewById(R.id.btn_edit_description);
        Button btnChangeProfilePhoto = findViewById(R.id.btn_change_profile_photo);
        Button btnLogout = findViewById(R.id.btn_logout);

        btnResetPassword.setOnClickListener(v -> resetPassword());

        btnUpdateEmail.setOnClickListener(v -> updateEmail());

        btnEditDescription.setOnClickListener(v -> editDescription());

        btnChangeProfilePhoto.setOnClickListener(v -> openFileChooser());

        btnLogout.setOnClickListener(v -> signOut());

    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Pick an image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {

        StorageReference fileRef = mStorageRef.child("profile_images/" + mCurrentUser.getUid() + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveImageToFirestore(imageUrl);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Error uploading image.", Toast.LENGTH_SHORT).show());
    }

    private void saveImageToFirestore(String imageUrl) {
        DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());
        userRef.update("profileImage", imageUrl)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileSettingsActivity.this, "Profile picture updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Error updating profile picture.", Toast.LENGTH_SHORT).show());
    }

    private void resetPassword() {
        if (mCurrentUser != null) {
            mAuth.sendPasswordResetEmail(mCurrentUser.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileSettingsActivity.this, "Reset email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "Error sending reset email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User not authored.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmail() {
        if (mCurrentUser != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update email");

            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                String newEmail = input.getText().toString().trim();
                if (newEmail.isEmpty()) {
                    Toast.makeText(ProfileSettingsActivity.this, "Please enter an email.", Toast.LENGTH_SHORT).show();
                    return;
                }

                reauthenticateAndUpdateEmail(newEmail);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        } else {
            Toast.makeText(this, "User not authored.", Toast.LENGTH_SHORT).show();
        }
    }

    private void reauthenticateAndUpdateEmail(String newEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Current password");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(ProfileSettingsActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reautenticar al usuario
            mCurrentUser.reauthenticate(EmailAuthProvider.getCredential(mCurrentUser.getEmail(), password))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mCurrentUser.updateEmail(newEmail)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            mCurrentUser.sendEmailVerification()
                                                    .addOnCompleteListener(verificationTask -> {
                                                        if (verificationTask.isSuccessful()) {
                                                            Toast.makeText(ProfileSettingsActivity.this, "Updated email. Please verify your new email.", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(ProfileSettingsActivity.this, "Error sending verification email", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(ProfileSettingsActivity.this, "Error updating email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "Authentication error", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void editDescription() {
        if (mCurrentUser != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit description");

            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newDescription = input.getText().toString().trim();
                if (newDescription.isEmpty()) {
                    Toast.makeText(ProfileSettingsActivity.this, "Please enter a description.", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateDescription(newDescription);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        } else {
            Toast.makeText(this, "User not authored.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDescription(String newDescription) {
        DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());
        userRef.update("Descripcion", newDescription)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileSettingsActivity.this, "Description updated.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileSettingsActivity.this, "Error updating description.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileSettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
