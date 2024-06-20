package com.example.always.Activities.Main;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.always.Adapters.ImageAdapter;
import com.example.always.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            showDatePickerDialog();
        } else {
            Toast.makeText(this, "User not authored", Toast.LENGTH_SHORT).show();
            goBack(null);
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog;
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(RemindersActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String fechaSeleccionada = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year);
                Toast.makeText(RemindersActivity.this, "Selected date: " + fechaSeleccionada, Toast.LENGTH_SHORT).show();
                cargarImagenesDesdeFirestore(fechaSeleccionada);
            }
        }, year, month, day);

        datePickerDialog.show();
    }

    private void cargarImagenesDesdeFirestore(String fechaSeleccionada) {
        db.collection("images")
                .whereEqualTo("date", fechaSeleccionada)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            mostrarGaleriaImagenes(queryDocumentSnapshots);
                        } else {
                            Toast.makeText(RemindersActivity.this, "No images found for the selected date", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RemindersActivity.this, "Error consulting database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarGaleriaImagenes(QuerySnapshot queryDocumentSnapshots) {
        List<String> imageUrls = new ArrayList<>();
        List<String> descriptionsList = new ArrayList<>();
        List<String> timesList = new ArrayList<>();

        for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
            String imageUrl = snapshot.getString("imageUrl");
            String description = snapshot.getString("description");
            String time = snapshot.getString("time");

            if (imageUrl != null) {
                imageUrls.add(imageUrl);
                descriptionsList.add(description);
                timesList.add(time);
            }
        }

        Collections.sort(timesList);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ImageAdapter adapter = new ImageAdapter(imageUrls, descriptionsList, timesList, RemindersActivity.this);
        recyclerView.setAdapter(adapter);

        recyclerView.setVisibility(View.VISIBLE);
    }

    public void goBack(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
