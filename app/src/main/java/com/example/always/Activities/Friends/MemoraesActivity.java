package com.example.always.Activities.Friends;

import com.example.always.Activities.Main.MainActivity;
import com.example.always.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemoraesActivity extends AppCompatActivity {

    private ImageView[] imageViewsProfile;
    private TextView[] textViewsName;
    private TextView[] textViewsTime;
    private ImageView[] imageViews;
    private TextView[] textViewsImageDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memoraes);

        TextView textViewTitle = findViewById(R.id.textViewTitle);
        textViewTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MemoraesActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });

        imageViewsProfile = new ImageView[]{
                findViewById(R.id.imageViewProfileUpperLeft),
                findViewById(R.id.imageViewProfileUpperRight),
                findViewById(R.id.imageViewProfileLowerLeft),
                findViewById(R.id.imageViewProfileLowerRight),
                findViewById(R.id.imageViewProfileCenterTop),
                findViewById(R.id.imageViewProfileCenterBottom),
                findViewById(R.id.imageViewProfileUpperLeft2),
                findViewById(R.id.imageViewProfileUpperRight2)
        };

        textViewsName = new TextView[]{
                findViewById(R.id.textViewNameUpperLeft),
                findViewById(R.id.textViewNameUpperRight),
                findViewById(R.id.textViewNameLowerLeft),
                findViewById(R.id.textViewNameLowerRight),
                findViewById(R.id.textViewNameCenterTop),
                findViewById(R.id.textViewNameCenterBottom),
                findViewById(R.id.textViewNameUpperLeft2),
                findViewById(R.id.textViewNameUpperRight2)
        };

        textViewsTime = new TextView[]{
                findViewById(R.id.textViewTimeUpperLeft),
                findViewById(R.id.textViewTimeUpperRight),
                findViewById(R.id.textViewTimeLowerLeft),
                findViewById(R.id.textViewTimeLowerRight),
                findViewById(R.id.textViewTimeCenterTop),
                findViewById(R.id.textViewTimeCenterBottom),
                findViewById(R.id.textViewTimeUpperLeft2),
                findViewById(R.id.textViewTimeUpperRight2)
        };

        imageViews = new ImageView[]{
                findViewById(R.id.imageViewUpperLeft),
                findViewById(R.id.imageViewUpperRight),
                findViewById(R.id.imageViewLowerLeft),
                findViewById(R.id.imageViewLowerRight),
                findViewById(R.id.imageViewCenterTop),
                findViewById(R.id.imageViewCenterBottom),
                findViewById(R.id.imageViewUpperLeft2),
                findViewById(R.id.imageViewUpperRight2)
        };

        textViewsImageDescription = new TextView[]{
                findViewById(R.id.textViewImageDescriptionUpperLeft),
                findViewById(R.id.textViewImageDescriptionUpperRight),
                findViewById(R.id.textViewImageDescriptionLowerLeft),
                findViewById(R.id.textViewImageDescriptionLowerRight),
                findViewById(R.id.textViewImageDescriptionCenterTop),
                findViewById(R.id.textViewImageDescriptionCenterBottom),
                findViewById(R.id.textViewImageDescriptionUpperLeft2),
                findViewById(R.id.textViewImageDescriptionUpperRight2)
        };

        for (int i = 1; i < imageViewsProfile.length; i++) {
            imageViewsProfile[i].setVisibility(View.GONE);
            textViewsName[i].setVisibility(View.GONE);
            textViewsTime[i].setVisibility(View.GONE);
            imageViews[i].setVisibility(View.GONE);
            textViewsImageDescription[i].setVisibility(View.GONE);
        }
        fetchUserData();
    }

    private void fetchUserData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Usuarios").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String profileImageUrl = documentSnapshot.getString("profileImage");
                String nombreUsuario = documentSnapshot.getString("NombreUsuario");

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).into(imageViewsProfile[0]);
                    imageViewsProfile[0].setVisibility(View.VISIBLE);
                }

                if (nombreUsuario != null) {
                    textViewsName[0].setText(nombreUsuario);
                    textViewsName[0].setVisibility(View.VISIBLE);
                }

                fetchUserImageOfTheDay(currentUserId);
                fetchFriendsData(currentUserId);
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
        });
    }

    private void fetchUserImageOfTheDay(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentDate = getCurrentDate();

        db.collection("images").whereEqualTo("userId", userId).whereEqualTo("date", currentDate).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String imageUrl = document.getString("imageUrl");
                    String description = document.getString("description");
                    String time = document.getString("time");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).into(imageViews[0]);
                        imageViews[0].setVisibility(View.VISIBLE);
                    }

                    if (description != null) {
                        textViewsImageDescription[0].setText(description);
                        textViewsImageDescription[0].setVisibility(View.VISIBLE);
                    }

                    if (time != null) {
                        textViewsTime[0].setText(time);
                        textViewsTime[0].setVisibility(View.VISIBLE);
                    }
                }
            } else {
                task.getException().printStackTrace();
            }
        });
    }

    private void fetchFriendsData(String currentUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentDate = getCurrentDate();

        db.collection("Usuarios").document(currentUserId).collection("friends").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int[] index = {1};
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String friendUserId = document.getString("UserId");
                    if (friendUserId != null && !friendUserId.isEmpty()) {
                        // Verificar si el amigo tiene una foto subida en el día actual
                        db.collection("images").whereEqualTo("userId", friendUserId).whereEqualTo("date", currentDate).get().addOnCompleteListener(imageTask -> {
                            if (imageTask.isSuccessful() && !imageTask.getResult().isEmpty()) {
                                // Obtener imágenes y datos del amigo
                                fetchFriendData(friendUserId, index[0]);
                                index[0]++;
                            }
                        }).addOnFailureListener(e -> {
                            e.printStackTrace();
                        });
                    }
                }
            } else {
                task.getException().printStackTrace();
            }
        });
    }

    private void fetchFriendData(String friendUserId, int index) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (index >= 0 && index < imageViewsProfile.length && index < textViewsName.length) {
            db.collection("Usuarios").document(friendUserId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImage");
                    String nombreUsuario = documentSnapshot.getString("NombreUsuario");

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this).load(profileImageUrl).into(imageViewsProfile[index]);
                        imageViewsProfile[index].setVisibility(View.VISIBLE);
                    }

                    if (nombreUsuario != null) {
                        textViewsName[index].setText(nombreUsuario);
                        textViewsName[index].setVisibility(View.VISIBLE);
                    }
                    fetchFriendImages(friendUserId, index);
                }
            }).addOnFailureListener(e -> {
                e.printStackTrace();
            });
        }
    }

    private void fetchFriendImages(String friendUserId, int index) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentDate = getCurrentDate();

        db.collection("images").whereEqualTo("userId", friendUserId).whereEqualTo("date", currentDate).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String imageUrl = document.getString("imageUrl");
                    String description = document.getString("description");
                    String time = document.getString("time");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).into(imageViews[index]);
                        imageViews[index].setVisibility(View.VISIBLE);
                    }

                    if (description != null) {
                        textViewsImageDescription[index].setText(description);
                        textViewsImageDescription[index].setVisibility(View.VISIBLE);
                    }

                    if (time != null) {
                        textViewsTime[index].setText(time);
                        textViewsTime[index].setVisibility(View.VISIBLE);
                    }
                }
            } else {
                task.getException().printStackTrace();
            }
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void goBack(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
