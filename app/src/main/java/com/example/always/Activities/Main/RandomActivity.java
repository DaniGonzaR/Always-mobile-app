package com.example.always.Activities.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.always.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class RandomActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView imageView;
    private TextView imageDescription;
    private TextView imageDate;
    private TextView textViewTime;
    private SharedPreferences userSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        imageView = findViewById(R.id.imageView);
        imageDescription = findViewById(R.id.textViewImageDescription);
        imageDate = findViewById(R.id.textViewImageDate);
        textViewTime = findViewById(R.id.textViewTime);
        userSharedPreferences = getSharedPreferences(getUserSharedPreferencesName(), Context.MODE_PRIVATE);

        int remainingPhotos = userSharedPreferences.getInt("remaining_photos3", 1);

        if (remainingPhotos > 0) {
            getRandomImage();
        } else {
            Toast.makeText(this, "You have already seen your random image today", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void getRandomImage() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("images")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            int randomIndex = new Random().nextInt(queryDocumentSnapshots.size());
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(randomIndex);
                            String imageUrl = document.getString("imageUrl");
                            String description = document.getString("description");
                            String date = document.getString("date");
                            String time = document.getString("time");

                            Glide.with(getApplicationContext())
                                    .load(imageUrl)
                                    .into(imageView);

                            imageDescription.setText(description);
                            imageDate.setText(date);
                            textViewTime.setText(time);

                            int remainingPhotos = userSharedPreferences.getInt("remaining_photos3", 1) - 1;
                            SharedPreferences.Editor editor = userSharedPreferences.edit();
                            editor.putInt("remaining_photos3", remainingPhotos);
                            editor.apply();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getApplicationContext(), "Error obtaining images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getUserSharedPreferencesName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return "test_user_preferences_" + user.getUid();
        } else {
            return "test_user_preferences_guest";
        }
    }
}

