package com.example.ex5_v1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LauncherActivity extends AppCompatActivity {

    public static final String SP_IS_REGISTERED = "is_registered";
    public static final String SP_USERNAME = "username";

    EditText username;
    Button skipButton, registerButton;

    SharedPreferences sp;
    SharedPreferences.Editor editor;

    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        username = (EditText) findViewById(R.id.editText_UserName);
        skipButton = (Button) findViewById(R.id.buttonSkip);
        registerButton = (Button) findViewById(R.id.buttonRegisterUser);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sp.edit();

        if (sp.getBoolean(SP_IS_REGISTERED, false))
        {
            Intent navigate = new Intent(LauncherActivity.this, MainActivity.class);
            navigate.putExtra(MainActivity.NAVIGATION_CODE, MainActivity.NOT_FIRST_LAUNCH);
            navigate.putExtra(MainActivity.USERNAME, sp.getString(SP_USERNAME, ""));
            startActivity(navigate);
            finish();
        }

        FirebaseApp.initializeApp(LauncherActivity.this);
        db = FirebaseFirestore.getInstance();

        new checkRemoteDataBase().execute();

        registerButton.setVisibility(View.INVISIBLE);

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                return;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 0)
                    registerButton.setVisibility(View.INVISIBLE);
                else
                    registerButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                return;
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent skipIntent = new Intent(LauncherActivity.this, MainActivity.class);
                skipIntent.putExtra(MainActivity.NAVIGATION_CODE, MainActivity.SKIPPED);
                startActivity(skipIntent);
                finish();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean(SP_IS_REGISTERED, true);
                editor.putString(SP_USERNAME, username.getText().toString());
                editor.apply();
                new updateUser().execute(username.getText().toString());
                Intent registerIntent = new Intent(LauncherActivity.this, MainActivity.class);
                registerIntent.putExtra(MainActivity.NAVIGATION_CODE, MainActivity.REGISTERED);
                registerIntent.putExtra(MainActivity.USERNAME, username.getText().toString());
                startActivity(registerIntent);
                finish();
            }
        });
    }

    public void checkName()
    {
        DocumentReference docRef = db.collection(MyAdapter.COLLECTION_NAME).
                document(MainActivity.FIRESTORE_USER_ID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String user = document.getData().get(MainActivity.FIRESTORE_USER_FIELD) + "";
                        redirect(user);
                    } else {
                        Log.d("", "No such document");
                    }
                } else {
                    Log.d("", "get failed with ", task.getException());
                }
            }
        });
    }

    public void registerToRemoteDB(String user)
    {
        DocumentReference washingtonRef = db.collection(MyAdapter.COLLECTION_NAME).
                document(MainActivity.FIRESTORE_USER_ID);

        washingtonRef
                .update(MainActivity.FIRESTORE_USER_FIELD, user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("", "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("", "Error updating document", e);
                    }
                });
    }

    private class updateUser extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            registerToRemoteDB(strings[0]);
            return null;
        }
    }

    public class checkRemoteDataBase extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            checkName();
            return null;
        }
    }
    public void redirect(String user)
    {
        if(!user.equals(""))
        {
            editor.putBoolean(SP_IS_REGISTERED, true);
            editor.putString(SP_USERNAME, user);
            editor.apply();
            Intent registerIntent = new Intent(LauncherActivity.this, MainActivity.class);
            registerIntent.putExtra(MainActivity.NAVIGATION_CODE, MainActivity.NOT_FIRST_LAUNCH);
            registerIntent.putExtra(MainActivity.USERNAME, user);
            startActivity(registerIntent);
            finish();
        }
    }

}
