package com.example.always.Activities.Account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.always.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore bd;
    private EditText signupUsername, signupEmail, signupPassword;
    private Button signupButton;
    private TextView loginRedirectText;
    private CheckBox showPasswordCheckBox;

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        // Debe tener al menos una mayúscula, una minúscula, un número y una longitud mínima de 6 caracteres
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$";
        return password.matches(regex);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        bd = FirebaseFirestore.getInstance();

        signupUsername = findViewById(R.id.signup_username);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox);
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Mostrar contraseña
                signupPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                // Ocultar contraseña
                signupPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });


        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = signupUsername.getText().toString().trim();
                String email = signupEmail.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                boolean hasError = false;

                if (username.isEmpty()) {
                    signupUsername.setError("Username cannot be empty");
                    hasError = true;
                }

                if (email.isEmpty()) {
                    signupEmail.setError("Email cannot be empty");
                    hasError = true;
                } else if (!isEmailValid(email)) {
                    signupEmail.setError("Enter a valid email address");
                    hasError = true;
                }

                if (password.isEmpty()) {
                    signupPassword.setError("Password cannot be empty");
                    hasError = true;
                } else if (!isPasswordValid(password)) {
                    signupPassword.setError("Password must contain at least 1 uppercase, 1 lowercase, 1 number, and minimum 6 characters");
                    hasError = true;
                }

                if (!hasError) {
                    registrarUser(username, email, "Default description", password);
                }
            }
        });


        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
    }


    private void registrarUser(String nombreUsuario, String emailUsuario, String descripcionUsuario, String passwordUsuario) {
        if (auth == null) {
            Log.v(this.getClass().getName(), "Authentication" + " nombre= " + nombreUsuario + " email= " + emailUsuario + " password= " + passwordUsuario);
        } else {
            auth.createUserWithEmailAndPassword(emailUsuario, passwordUsuario).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String id = usuario.getUid();

                            Map<String, Object> map = new HashMap<>();
                            map.put("IDUsuario", id);
                            map.put("NombreUsuario", nombreUsuario);
                            map.put("Email", emailUsuario);
                            map.put("Descripcion", descripcionUsuario);
                            map.put("emoticon1Url", "");
                            map.put("emoticon2Url", "");
                            map.put("emoticon3Url", "");
                            map.put("emoticon4Url", "");
                            map.put("profileImage", "");

                            // Agregar el usuario a la colección "Usuarios"
                            bd.collection("Usuarios").document(id).set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    // Código eliminado para no crear la colección "Amigos"
                                    finish();
                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    Toast.makeText(SignUpActivity.this, "Registered user", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(SignUpActivity.this, "Register error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Register error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

}
