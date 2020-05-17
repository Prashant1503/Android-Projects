package com.example.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private AppCompatEditText noteTitle, noteDescription;
    public AppCompatButton saveBtn, readNoteBtn,googleSignInWithBtn;
    private ProgressBar progressBar;
    public AppCompatTextView dataTv;

    private static final String TAG = "Main Activity";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";

//    google sign in fields..
    private GoogleSignInClient mOnGoogleSignInClient;
    private FirebaseAuth mAuth;
    private int RC_SIGNIN = 1;

    String id;
    private FirebaseFirestore db;
    private CollectionReference mCollectionRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        db = FirebaseFirestore.getInstance();
        mCollectionRef = db.collection("NOTEBOOK");
        mAuth = FirebaseAuth.getInstance();

        saveBtn = findViewById(R.id.saveNoteBtn);
        progressBar = findViewById(R.id.progressBar);
        noteTitle = findViewById(R.id.note_titleEdt);
        noteDescription = findViewById(R.id.note_descriptionEdt);
        dataTv = findViewById(R.id.dataTv);
        readNoteBtn = findViewById(R.id.readNoteBtn);

        googleSignInWithBtn = findViewById(R.id.signInWithGoogleBtn);

        saveBtn.setOnClickListener(new SaveClickListner());
        readNoteBtn.setOnClickListener(new OnLoadListner());
        googleSignInWithBtn.setOnClickListener(new OnGoogleListner());


    }

    private class OnGoogleListner implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            signInWithGoogle();

        }
    }

    private class SaveClickListner implements View.OnClickListener {


        @Override
        public void onClick(View v) {
            saveNote();

        }
    }

    private class OnLoadListner implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            loadNote();
        }
    }


    public void signInWithGoogle() {

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mOnGoogleSignInClient = GoogleSignIn.getClient(this,googleSignInOptions);

        Intent signInIntent = mOnGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_SIGNIN);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode  == RC_SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Toast.makeText(getApplicationContext(),"Signed in successfull",Toast.LENGTH_SHORT).show();
            FirebaseGoogleAuth(account);

        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
            FirebaseGoogleAuth(null);
        }

    }
    private void FirebaseGoogleAuth(GoogleSignInAccount account){
        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        mAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(),"Successfull",Toast.LENGTH_SHORT).show();
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),task.getException().toString(),Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void updateUI(FirebaseUser user) {
        GoogleSignInAccount account=GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account!=null) {
            String personName = account.getDisplayName();
            String email = account.getEmail();

            Log.d(TAG, "Firebase User: " + user);
            Toast.makeText(getApplicationContext(),"Personal Name : " + personName + "Email : " + email,Toast.LENGTH_SHORT).show();

        }
        else {
            Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }

    }

    public void saveNote() {
        String title = noteTitle.getText().toString();
        String description = noteDescription.getText().toString();

        Map<String, Object> note = new HashMap<>();
        note.put(KEY_TITLE, title);
        note.put(KEY_DESCRIPTION, description);
        progressBar.setVisibility(View.VISIBLE);

        id = db.collection("NOTEBOOK").document().getId();
        Log.d(TAG, "Note id: " + id);

        db.collection("NOTEBOOK").document(id).set(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {

                    @Override
                    public void onSuccess(Void aVoid) {

                        Toast.makeText(getApplicationContext(), "Note Saved", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.INVISIBLE);
                        noteDescription.setText(null);
                        noteTitle.setText(null);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);

            }
        });
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void loadNote() {
        mCollectionRef = db.collection("NOTEBOOK");

        mCollectionRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                 for (DocumentSnapshot ds : queryDocumentSnapshots) {

                     Log.d(TAG, "Response: " + ds.getData());

                 }

            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });

    }



}

