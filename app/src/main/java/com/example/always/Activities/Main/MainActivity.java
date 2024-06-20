package com.example.always.Activities.Main;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.always.Activities.Friends.MemoraesActivity;
import com.example.always.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences userSharedPreferences;
    private int remainingRandomPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userSharedPreferences = getSharedPreferences(getUserSharedPreferencesName(), MODE_PRIVATE);
        checkAndResetDailyPhotoCounter();
        remainingRandomPhotos = userSharedPreferences.getInt("remaining_photos3 ", 1);

        // Referencias a los botones
        ImageButton buttonMemoraes = findViewById(R.id.buttonMemoraes);
        ImageButton buttonReminders = findViewById(R.id.buttonReminders);
        ImageButton buttonRandom = findViewById(R.id.buttonRandom);
        ImageButton buttonMyProfile = findViewById(R.id.buttonMyProfile);

        buttonMemoraes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MemoraesActivity.class);
                startActivity(intent);
            }
        });

        buttonReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemindersActivity.class);
                startActivity(intent);
            }
        });

        buttonRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });

        buttonMyProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        remainingRandomPhotos = userSharedPreferences.getInt("remaining_photos3", 1);
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert")
                .setMessage("Do you want to see a random photo? You have " + remainingRandomPhotos + " random photo left today.")
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (remainingRandomPhotos > 0) {
                            Intent intent = new Intent(MainActivity.this, RandomActivity.class);
                            startActivityForResult(intent, 1);
                        } else {
                            dialog.dismiss();
                            showNoRemainingPhotosDialog();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // Mostrar un mensaje cuando no quedan fotos aleatorias restantes
    private void showNoRemainingPhotosDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert")
                .setMessage("You no longer have any random photos left today.")
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // Obtener las SharedPreferences para el contador de Random
    private String getUserSharedPreferencesName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            return "test_user_preferences_" + user.getUid();
        } else {
            return "test_user_preferences_guest";
        }
    }

    // Método para verificar y reiniciar el contador diario de fotos (Random)
    private void checkAndResetDailyPhotoCounter() {
        String lastDate = userSharedPreferences.getString("last_date", "");
        String currentDate = getCurrentDate();

        if (!lastDate.equals(currentDate)) {
            SharedPreferences.Editor editor = userSharedPreferences.edit();
            editor.putString("last_date", currentDate);
            editor.putInt("remaining_photos3", 1); // Reiniciar el contador a 1
            editor.apply();
        }
    }

    // Obtener la fecha actual en formato yyyy-MM-dd
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
