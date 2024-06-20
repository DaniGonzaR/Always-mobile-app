package com.example.always.Activities.Account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.always.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {

    private EditText mEditTextEmail;
    private Button mButtonResetPassword,goBackButton;
    private String email = "";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        goBackButton = findViewById(R.id.btnGoBackLogin);
        mEditTextEmail = findViewById(R.id.editTextEmail);
        mButtonResetPassword = findViewById(R.id.btnResetPassword);
        mButtonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = mEditTextEmail.getText().toString().trim();
                if(!email.isEmpty()) {
                    resetPassword();
                } else {
                    Toast.makeText(ResetPassword.this, "You must enter the email", Toast.LENGTH_SHORT).show();
                }
            }

            private void resetPassword() {
                mAuth.setLanguageCode("es");
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ResetPassword.this, "Email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ResetPassword.this, "Email could not be sent", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        goBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(ResetPassword.this, LoginActivity.class));
            }
        });
    }
}