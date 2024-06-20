package com.example.always.Activities.Main;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.always.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class MyProfileActivity extends AppCompatActivity {

    ImageView imageViewProfile;
    TextView textViewTitle;
    TextView textViewName;
    TextView textViewSmallText;
    ImageView imageViewCircle1;
    ImageView imageViewCircle2;
    ImageView imageViewCircle3;
    ImageView imageViewCircle4;
    View divider;
    ImageView imageViewMainActivity;
    ImageView imageViewNewReminder;
    ImageView imageViewSettings;

    private FirebaseFirestore db;
    private FirebaseUser mCurrentUser;
    private FirebaseStorage storage;

    private static final int REQUEST_IMAGE_GALLERY = 101;
    private static final int REQUEST_IMAGE_CAMERA = 102;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        // Inicialización de todos los elementos
        imageViewProfile = findViewById(R.id.imageViewProfile);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewName = findViewById(R.id.textViewName);
        textViewSmallText = findViewById(R.id.textViewSmallText);
        imageViewCircle1 = findViewById(R.id.imageViewCircle1);
        imageViewCircle2 = findViewById(R.id.imageViewCircle2);
        imageViewCircle3 = findViewById(R.id.imageViewCircle3);
        imageViewCircle4 = findViewById(R.id.imageViewCircle4);
        divider = findViewById(R.id.divider);
        imageViewMainActivity = findViewById(R.id.imageViewMainActivity);
        imageViewNewReminder = findViewById(R.id.imageViewNewReminder);
        imageViewSettings = findViewById(R.id.imageViewSettings);

        db = FirebaseFirestore.getInstance();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();

        if (mCurrentUser != null) {
            String userId = mCurrentUser.getUid();
            db.collection("Usuarios").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImage");
                    RequestOptions requestOptions = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.baseline_person_24);
                    Glide.with(this)
                            .load(profileImageUrl)
                            .apply(requestOptions)
                            .circleCrop()
                            .into(imageViewProfile);
                    setEmoticonFromFirestore(imageViewCircle1, documentSnapshot.getString("emoticon1"));
                    setEmoticonFromFirestore(imageViewCircle2, documentSnapshot.getString("emoticon2"));
                    setEmoticonFromFirestore(imageViewCircle3, documentSnapshot.getString("emoticon3"));
                    setEmoticonFromFirestore(imageViewCircle4, documentSnapshot.getString("emoticon4"));

                    String nombreUsuario = documentSnapshot.getString("NombreUsuario");
                    if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                        textViewName.setText(nombreUsuario);
                    } else {
                        imageViewProfile.setImageResource(R.drawable.baseline_person_24);
                        textViewName.setText(userId);
                    }
                } else {
                    textViewName.setText(userId);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(MyProfileActivity.this, "Error obtaining user name " + e.getMessage(), Toast.LENGTH_SHORT).show();
                textViewName.setText(userId);
            });
        } else {
            textViewName.setText("User not authenticated");
        }

        loadUserDescription();
        setupCircleClickListeners();
        loadEmoticonsFromFirestore();
    }

    public void openImagePicker(View view) {
        // Crea un Intent para abrir la galería o la cámara
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_GALLERY);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_IMAGE_GALLERY || requestCode == REQUEST_IMAGE_CAMERA) && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            uploadImageToFirebaseStorage(selectedImageUri);
        }
    }

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        if (mCurrentUser != null) {
            String userId = mCurrentUser.getUid();
            StorageReference profileImageRef = storage.getReference().child("profile_images").child(userId + ".jpg");

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveProfileImageToFirestore(imageUrl);

                        Glide.with(this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .into(imageViewProfile);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(MyProfileActivity.this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileImageToFirestore(String imageUrl) {
        if (mCurrentUser != null) {
            DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());

            Map<String, Object> data = new HashMap<>();
            data.put("profileImage", imageUrl);

            userRef.update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MyProfileActivity.this, "Profile image saved", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MyProfileActivity.this, "Error saving profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(MyProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEmoticonsFromFirestore() {
        if (mCurrentUser != null) {
            DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    for (int i = 1; i <= 4; i++) {
                        String emoticonUrl = documentSnapshot.getString("emoticon" + i + "Url");
                        if (emoticonUrl != null) {
                            ImageView imageView = findImageViewByCircleIndex(i);
                            int resId = getEmoticonResourceIdByUrl(emoticonUrl);
                            imageView.setImageResource(resId);
                        }
                    }
                }
            });
        }
    }

    private int getEmoticonResourceIdByUrl(String url) {
        switch (url) {
            case "url_del_emoticon_1": return R.drawable.emoticon1;
            case "url_del_emoticon_2": return R.drawable.emoticon2;
            case "url_del_emoticon_3": return R.drawable.emoticon3;
            case "url_del_emoticon_4": return R.drawable.emoticon4;
            case "url_del_emoticon_5": return R.drawable.emoticon5;
            case "url_del_emoticon_6": return R.drawable.emoticon6;
            case "url_del_emoticon_7": return R.drawable.emoticon7;
            case "url_del_emoticon_8": return R.drawable.emoticon8;
            case "url_del_emoticon_9": return R.drawable.emoticon9;
            case "url_del_emoticon_10": return R.drawable.emoticon10;
            default: return R.drawable.emoticon1;
        }
    }

    private ImageView findImageViewByCircleIndex(int index) {
        switch (index) {
            case 1: return imageViewCircle1;
            case 2: return imageViewCircle2;
            case 3: return imageViewCircle3;
            case 4: return imageViewCircle4;
            default: return null;
        }
    }

    private void setEmoticonFromFirestore(ImageView imageView, String emoticonUrl) {
        if (emoticonUrl != null) {
            int resourceId = getEmoticonResourceIdFromUrl(emoticonUrl);
            imageView.setImageResource(resourceId);
        }
    }

    private int getEmoticonResourceIdFromUrl(String emoticonUrl) {
        switch (emoticonUrl) {
            case "url_del_emoticon_1": return R.drawable.emoticon1;
            case "url_del_emoticon_2": return R.drawable.emoticon2;
            case "url_del_emoticon_3": return R.drawable.emoticon3;
            case "url_del_emoticon_4": return R.drawable.emoticon4;
            case "url_del_emoticon_5": return R.drawable.emoticon5;
            case "url_del_emoticon_6": return R.drawable.emoticon6;
            case "url_del_emoticon_7": return R.drawable.emoticon7;
            case "url_del_emoticon_8": return R.drawable.emoticon8;
            case "url_del_emoticon_9": return R.drawable.emoticon9;
            case "url_del_emoticon_10": return R.drawable.emoticon10;
            case "url_del_emoticon_11": return R.drawable.emoticon11;
            case "url_del_emoticon_12": return R.drawable.emoticon12;
            default: return R.drawable.emoticon1;
        }
    }

    private void loadUserDescription() {
        DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());
        userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String description = snapshot.getString("Descripcion");
                if (description != null) {
                    textViewSmallText.setText(description);
                }
            }
        });
    }

    private void showEmoticonDialog(final ImageView imageView, int circleIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Emoticon");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emoticons, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        int totalEmoticons = 12;

        for (int i = 1; i <= totalEmoticons; i++) {
            int emoticonId = getResources().getIdentifier("emoticon" + i, "id", getPackageName());
            int drawableId = getResources().getIdentifier("emoticon" + i, "drawable", getPackageName());

            int finalI = i;
            dialogView.findViewById(emoticonId).setOnClickListener(v -> {
                updateEmoticon(imageView, circleIndex, "url_del_emoticon_" + finalI, drawableId);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void saveEmoticonReferenceToFirestore(int circleIndex, String emoticonUrl) {
        if (mCurrentUser != null) {
            DocumentReference userRef = db.collection("Usuarios").document(mCurrentUser.getUid());

            Map<String, Object> data = new HashMap<>();
            data.put("emoticon" + circleIndex + "Url", emoticonUrl);

            userRef.update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MyProfileActivity.this, "Emoticon saved", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MyProfileActivity.this, "Error saving emoticon: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(MyProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmoticon(ImageView imageView, int circleIndex, String emoticonUrl, int drawableId) {
        imageView.setImageResource(drawableId);
        saveEmoticonReferenceToFirestore(circleIndex, emoticonUrl);
    }

    private void setupCircleClickListeners() {
        imageViewCircle1.setOnClickListener(v -> showEmoticonDialog(imageViewCircle1, 1));
        imageViewCircle2.setOnClickListener(v -> showEmoticonDialog(imageViewCircle2, 2));
        imageViewCircle3.setOnClickListener(v -> showEmoticonDialog(imageViewCircle3, 3));
        imageViewCircle4.setOnClickListener(v -> showEmoticonDialog(imageViewCircle4, 4));
    }

    public void goToNewReminderActivity(View view) {
        Intent intent = new Intent(MyProfileActivity.this, NewReminderActivity.class);
        startActivity(intent);
    }

    public void goToMainActivity(View view) {
        Intent intent = new Intent(MyProfileActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void goToSettingsActivity(View view) {
        Intent intent = new Intent(MyProfileActivity.this, ProfileSettingsActivity.class);
        startActivity(intent);
    }
}
