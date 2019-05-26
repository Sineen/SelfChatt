package com.example.ex5_v1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity implements MyAdapter.recItemOnLongClick{

    public static final String SP_DATA_SIZE = "data_size";
    public static final String SP_DATA_LIST = "sent_messages";
    public static final String SP_SYNC_FLAG = "first_launch";

    public static final String FIRESTORE_USER_ID = "User7m6ztx0eqyPDc7tIQlHe";
    public static final String FIRESTORE_USER_FIELD = "User";

    public static final String NAVIGATION_CODE = "navigation_code";
    public static final String SKIPPED = "skip";
    public static final String REGISTERED = "register";
    public static final String DELETE = "delete";
    public static final String NOT_FIRST_LAUNCH = "not_first_launch";

    public static final String USERNAME = "username";
    public static final String DELETE_MESSAGE_CODE = "pos";

    private Button button;
    private EditText editText;
    private TextView helloMessage;
    private RecyclerView recyclerView;

    private MyAdapter myAdapter;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private FirebaseFirestore db;
    private CollectionReference reference;

    private String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button3);
        editText = (EditText) findViewById(R.id.editText2);
        recyclerView = (RecyclerView) findViewById(R.id.rec1);
        helloMessage = (TextView) findViewById(R.id.textView_HelloMessage);

        FirebaseApp.initializeApp(MainActivity.this);
        db = FirebaseFirestore.getInstance();
        reference = db.collection(MyAdapter.COLLECTION_NAME);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sp.edit();

        int data_size = sp.getInt(SP_DATA_SIZE, 0);

        myAdapter = new MyAdapter(data_size, sp, editor, db);
        myAdapter.setClickListener(this);

        new getFireBaseId().execute();

        recyclerView.setAdapter(myAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        if(data_size != 0 ) { myAdapter.loadData(); }
        else if (sp.getBoolean(SP_SYNC_FLAG, true))
        {
//            new syncLocalToRemoteFireBase().execute();
            editor.putBoolean(SP_SYNC_FLAG, false);
            editor.apply();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = editText.getText().toString();
                editText.setText("");
                if(message.equals(""))
                {
                    Toast.makeText(getApplicationContext(),
                            "you can't send an empty message, oh silly!", Toast.LENGTH_LONG).show();
                    return;
                }
                new insertDataToFireBase().execute(message);
            }
        });

        Bundle extras = getIntent().getExtras();
        manage_navigation(extras);

    }

    public void manage_navigation(Bundle extras)
    {
        String code = extras.getString(NAVIGATION_CODE);
        if(code == null)
            return;

        if(code.equals(SKIPPED)) {
            helloMessage.setVisibility(View.INVISIBLE);
            return;
        }

        if(code.equals(REGISTERED) || code.equals(NOT_FIRST_LAUNCH)) {
            String username = extras.getString(USERNAME);
            this.username = username;
            helloMessage.setText("Hello " + username);
            return;
        }

        if(code.equals(DELETE)) {
            myAdapter.deleteMessage(extras.getInt(DELETE_MESSAGE_CODE));
            username = extras.getString(USERNAME);
            helloMessage.setText("Hello " + username);
            return;
        }

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        myAdapter.data = new ArrayList<Message>();
        reference.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                @Nullable FirebaseFirestoreException e) {
                if(e != null)
                {
                    return;
                }
                for(DocumentChange documentChange: queryDocumentSnapshots.getDocumentChanges())
                {
                    DocumentSnapshot documentSnapshot = documentChange.getDocument();
                    String id = documentSnapshot.getId();
                    boolean isDocumentDeleted = documentChange.getOldIndex() != -1;
                    boolean isDocumentAdded = documentChange.getNewIndex() != -1;
                    if (isDocumentDeleted)
                    {
                        for(int index = 0 ; index < myAdapter.data.size(); index++)
                            if (myAdapter.data.get(index).getId().equals(id)) {
                                myAdapter.deleteMessage(index);
                                break;
                            }
                    }

                    else if(isDocumentAdded
                            && !documentSnapshot.getId().equals(MyAdapter.DOCUMENT_ID)
                            && !documentSnapshot.getId().equals(FIRESTORE_USER_ID))
                    {
                        Map<String, Object> new_doc_data = documentSnapshot.getData();
                        String Id = new_doc_data.get(MyAdapter.MESSAGE_ID)+"";
                        String content = new_doc_data.get(MyAdapter.MESSAGE_CONTENT)+"";
                        String timestamp = new_doc_data.get(MyAdapter.MESSAGE_TIMESTAMP) + "";
                        String device = new_doc_data.get(MyAdapter.MESSAGE_DEVICE) + "";
                        myAdapter.addMessage(Id, timestamp, content, device);
                    }
                }
            }
        });
    }

    @Override
    public void itemLongClick(View view, final int position) {
        Message message = myAdapter.data.get(position);
        Intent intent = new Intent(MainActivity.this, Message.MessageDetails.class);
        intent.putExtra(Message.MessageDetails.INTENT__KEY_MESSAGE_CONTENT, message.getText());
        intent.putExtra(Message.MessageDetails.INTENT__KEY_MESSAGE_ID, message.getId());
        intent.putExtra(Message.MessageDetails.INTENT__KEY_MESSAGE_TIMESTAMP, message.getTimeStamp());
        intent.putExtra(Message.MessageDetails.INTENT__KEY_MESSAGE_DEVICE, message.getDevice());
        intent.putExtra(DELETE_MESSAGE_CODE, position);
        intent.putExtra(USERNAME, username);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("wrote_message", editText.getText().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String wrote_message = savedInstanceState.getString("wrote_message");
        editText.setText(wrote_message);
        myAdapter.loadData();
        myAdapter.supportConfigurationChange();
    }

    private class getFireBaseId extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            myAdapter.getGlobalId();
            return null;
        }
    }

    private class insertDataToFireBase extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            myAdapter.addToRemoteFireBase(strings[0]);
            return null;
        }
    }

    public class syncLocalToRemoteFireBase extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            myAdapter.loadDataFromRemoteFireBase();
            return null;
        }
    }
}

