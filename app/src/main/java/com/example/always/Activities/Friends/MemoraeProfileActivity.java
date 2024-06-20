package com.example.always.Activities.Friends;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.always.Activities.Main.MainActivity;
import com.example.always.Activities.Main.NewReminderActivity;
import com.example.always.Activities.Main.ProfileSettingsActivity;
import com.example.always.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class MemoraeProfileActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_memorae_profile);

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

        // Obtener el userId pasado por la intención
        Intent intent = getIntent();
        String userId = intent.getStringExtra("USER_ID");

        if (userId != null) {
            db.collection("Usuarios").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String nombreUsuario = documentSnapshot.getString("NombreUsuario");
                    if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                        String titulo = nombreUsuario + " Profile";
                        textViewTitle.setText(titulo);
                    }
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(MemoraeProfileActivity.this, "Error obtaining user name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }

        if (userId != null) {
            db.collection("Usuarios").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImage");
                    RequestOptions requestOptions = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.emoticon6);
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
                Toast.makeText(MemoraeProfileActivity.this, "Error obtaining user name " + e.getMessage(), Toast.LENGTH_SHORT).show();
                textViewName.setText(userId);
            });
        } else {
            textViewName.setText("User not authenticated");
        }

        loadUserDescription(userId);
        loadEmoticonsFromFirestore(userId);
    }

    public void goToRemoveFriend(View view) {
        String currentUserId = mCurrentUser.getUid();
        Intent intent = getIntent();
        String otherUserId = intent.getStringExtra("USER_ID");

        deleteFriendReference(currentUserId, otherUserId);
        deleteFriendReference(otherUserId, currentUserId);

        Toast.makeText(this, "Friends connection has been removed", Toast.LENGTH_SHORT).show();

        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private void deleteFriendReference(String userId, String friendId) {
        DocumentReference userRef = db.collection("Usuarios").document(userId);
        userRef.collection("friends").document(friendId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error obtaining friend connection " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadUserDescription(String userId) {
        DocumentReference userRef = db.collection("Usuarios").document(userId);
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

    private void loadEmoticonsFromFirestore(String userId) {
        DocumentReference userRef = db.collection("Usuarios").document(userId);
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

    public void goToNewReminderActivity(View view) {
        Intent intent = new Intent(MemoraeProfileActivity.this, NewReminderActivity.class);
        startActivity(intent);
    }

    public void goToMainActivity(View view) {
        Intent intent = new Intent(MemoraeProfileActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void goToSettingsActivity(View view) {
        Intent intent = new Intent(MemoraeProfileActivity.this, ProfileSettingsActivity.class);
        startActivity(intent);
    }
}
