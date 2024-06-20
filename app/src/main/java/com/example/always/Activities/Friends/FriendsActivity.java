package com.example.always.Activities.Friends;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.always.Adapters.FriendsAdapter;
import com.example.always.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FriendsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION_VIBRATE = 123;
    private static final String PERMISSION_VIBRATE = Manifest.permission.VIBRATE;
    private boolean friendsDisplayed = false;
    private EditText friendUsernameEditText;
    private Button addFriendButton;
    private Button displayFriendRequestsButton;
    private Button displayFriendsButton;
    private String nombre;
    private String idUsuarioSolicitante;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        friendUsernameEditText = findViewById(R.id.friend_username_edit_text);
        addFriendButton = findViewById(R.id.add_friend_button);
        displayFriendRequestsButton = findViewById(R.id.display_friend_requests_button);
        displayFriendsButton = findViewById(R.id.display_friends_button);


        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Usuarios").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    nombre = documentSnapshot.getString("NombreUsuario");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(FriendsActivity.this, "Error obtaining user name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }

        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFriendByUsername();
            }
        });

        displayFriendRequestsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFriendRequests();
            }
        });

        displayFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFriends();
            }
        });
    }

    private void searchFriendByUsername() {
        final String friendUsername = friendUsernameEditText.getText().toString().trim();

        if (friendUsername.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Introduce a valid username", Toast.LENGTH_SHORT).show();
            return;
        }

        final String currentUid = mAuth.getCurrentUser().getUid();

        // Verificar si ya existe una solicitud de amistad pendiente con este usuario
        db.collection("Usuarios").document(currentUid)
                .collection("solicitudes")
                .whereEqualTo("nombreUsuarioSolicitante", friendUsername)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Si ya existe una solicitud pendiente, mostrar un mensaje de error
                                Toast.makeText(getApplicationContext(), "You have already sent a friend request to this user.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Verificar si ya son amigos
                                db.collection("Usuarios").document(currentUid)
                                        .collection("friends")
                                        .whereEqualTo("NombreUsuario", friendUsername)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    if (!task.getResult().isEmpty()) {
                                                        // Si ya son amigos, mostrar un mensaje de error
                                                        Toast.makeText(getApplicationContext(), "You are already friends with this user.", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        // Si no hay una solicitud pendiente y no son amigos, enviar la solicitud de amistad
                                                        sendFriendRequest(friendUsername);
                                                    }
                                                } else {
                                                    Toast.makeText(getApplicationContext(), "Error verifying friends", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Error verifying the friend request", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendFriendRequest(String friendUsername) {
        final String requestId = UUID.randomUUID().toString();

        db.collection("Usuarios")
                .whereEqualTo("NombreUsuario", friendUsername)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            final String friendUid = document.getId();
                            final String currentUid = mAuth.getCurrentUser().getUid();

                            // Check if there is already a pending friend request to this user
                            db.collection("Usuarios").document(friendUid)
                                    .collection("solicitudes")
                                    .whereEqualTo("idUsuarioSolicitante", currentUid)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful() && task.getResult().isEmpty()) {
                                                // If there is no pending friend request, send the request
                                                Map<String, Object> requestMap = new HashMap<>();
                                                requestMap.put("nombreUsuarioSolicitante", nombre);
                                                requestMap.put("requestId", requestId);
                                                requestMap.put("idUsuarioSolicitante", currentUid);

                                                db.collection("Usuarios").document(friendUid)
                                                        .collection("solicitudes")
                                                        .document(requestId)
                                                        .set(requestMap)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(getApplicationContext(), "Friend request sent.", Toast.LENGTH_SHORT).show();
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(getApplicationContext(), "Error sending friend request", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(getApplicationContext(), "You have already sent a friend request to this user.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(getApplicationContext(), "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }




    private void acceptFriendRequest(final String requestId) {
        final String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("Usuarios").document(currentUid)
                .collection("solicitudes").document(requestId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            final String friendUsername = documentSnapshot.getString("nombreUsuarioSolicitante");
                            final String friendUid = documentSnapshot.getString("idUsuarioSolicitante");

                            // Eliminar la solicitud de amistad
                            db.collection("Usuarios").document(currentUid)
                                    .collection("solicitudes").document(requestId)
                                    .delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Agregar al amigo en la lista de amigos del usuario actual
                                                Map<String, Object> friendData = new HashMap<>();
                                                friendData.put("NombreUsuario", friendUsername);
                                                friendData.put("UserId", friendUid);

                                                db.collection("Usuarios").document(currentUid)
                                                        .collection("friends").document(friendUid)
                                                        .set(friendData)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    // Agregar al usuario actual en la lista de amigos del amigo
                                                                    Map<String, Object> currentUserData = new HashMap<>();
                                                                    currentUserData.put("NombreUsuario", nombre);
                                                                    currentUserData.put("UserId", currentUid);

                                                                    db.collection("Usuarios").document(friendUid)
                                                                            .collection("friends").document(currentUid)
                                                                            .set(currentUserData)
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        Toast.makeText(getApplicationContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("AcceptFriendRequest", "Error with the friend request " + requestId, e);
                    }
                });
    }


    private void displayFriendRequests() {
        db.collection("Usuarios").document(mAuth.getCurrentUser().getUid())
                .collection("solicitudes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            final List<DocumentSnapshot> requestList = task.getResult().getDocuments();

                            if (requestList.isEmpty()) {
                                Toast.makeText(FriendsActivity.this, "There is no pending friend requests", Toast.LENGTH_SHORT).show();
                            } else {
                                final List<String> requestUsernames = new ArrayList<>();
                                for (DocumentSnapshot document : requestList) {
                                    String requestUsername = document.getString("nombreUsuarioSolicitante");
                                    if (requestUsername != null) {
                                        requestUsernames.add(requestUsername);
                                    } else {
                                        requestUsernames.add("Unknow User");
                                    }
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
                                builder.setTitle("Pending friend requests")
                                        .setMultiChoiceItems(requestUsernames.toArray(new CharSequence[0]), null, new DialogInterface.OnMultiChoiceClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                            }
                                        })
                                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                for (int i = 0; i < requestList.size(); i++) {
                                                    if (((AlertDialog) dialog).getListView().isItemChecked(i)) {
                                                        acceptFriendRequest(requestList.get(i).getId());
                                                    }
                                                }
                                            }
                                        })
                                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();
                            }
                        }
                    }
                });
    }


    private void displayFriends() {
        // Verificar si los amigos ya se están mostrando
        if (friendsDisplayed) {
            // Si ya se están mostrando, ocultar el RecyclerView y cambiar el estado a no mostrado
            RecyclerView recyclerView = findViewById(R.id.friends_recycler_view);
            recyclerView.setVisibility(View.GONE);
            friendsDisplayed = false;
        } else {
            // Si no se están mostrando, obtener la lista de amigos y mostrar el RecyclerView
            db.collection("Usuarios").document(mAuth.getCurrentUser().getUid())
                    .collection("friends")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<Map<String, Object>> friendList = new ArrayList<>();
                                for (DocumentSnapshot document : task.getResult()) {
                                    friendList.add(document.getData());
                                }

                                RecyclerView recyclerView = findViewById(R.id.friends_recycler_view);
                                recyclerView.setLayoutManager(new LinearLayoutManager(FriendsActivity.this));

                                if (friendList.isEmpty()) {
                                    // Mostrar un mensaje indicando que no hay amigos agregados
                                    Toast.makeText(FriendsActivity.this, "You have no added friends.", Toast.LENGTH_SHORT).show();
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    FriendsAdapter adapter = new FriendsAdapter(friendList);
                                    adapter.setOnFriendClickListener(new FriendsAdapter.OnFriendClickListener() {
                                        @Override
                                        public void onFriendClick(int position) {
                                            FriendsActivity.this.onFriendClick(position);
                                        }
                                    });
                                    recyclerView.setAdapter(adapter);

                                    recyclerView.setVisibility(View.VISIBLE);
                                    friendsDisplayed = true;

                                    for (Map<String, Object> friend : friendList) {
                                        String userId = (String) friend.get("UserId");
                                        loadProfileImage(userId, adapter);
                                    }
                                }
                            } else {
                                Toast.makeText(FriendsActivity.this, "Error al obtener la lista de amigos.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    private void loadProfileImage(String userId, FriendsAdapter adapter) {
        db.collection("Usuarios").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImage");
                            adapter.updateProfileImage(userId, profileImageUrl);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Friends", "Error obtaining profile picture of user: " + userId, e);
                    }
                });
    }


    public void onFriendClick(int position) {
        db.collection("Usuarios").document(mAuth.getCurrentUser().getUid())
                .collection("friends")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Map<String, Object>> friendList = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                friendList.add(document.getData());
                            }
                            Map<String, Object> friend = friendList.get(position);
                            String userId = (String) friend.get("UserId");
                            goToProfileActivity(userId);
                        }
                    }
                });
    }

    public void goToProfileActivity(String userId) {
        Intent intent = new Intent(FriendsActivity.this, MemoraeProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }
}


