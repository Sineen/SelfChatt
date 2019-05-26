package com.example.ex5_v1;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    int CounterId = 0;

    public static final String TIME_FORMAT = "kk:mm";
    public static final String DOCUMENT_ID = "oLi5oPOG4Q44q9nbekLE";
    public static final String PROJECT_ID = "project_id";
    public static final String COLLECTION_NAME = "messages";

    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_ID = "id";
    public static final String MESSAGE_TIMESTAMP = "timestamp";
    public static final String MESSAGE_DEVICE = "device";

    public ArrayList<Message> data;

    private recItemOnLongClick clickedMessage;
    public int data_size;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private FirebaseFirestore dataBase;

    public interface recItemOnLongClick {
        void itemLongClick(View view, final int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
        TextView textView;
        TextView timestamp;
        public MyViewHolder(View view){
            super(view);
            textView = view.findViewById(R.id.textView_one_message_template);
            timestamp = view.findViewById(R.id.timestamp);
            view.setOnLongClickListener(this);
        }

        public boolean onLongClick(View view){
            if (clickedMessage != null) {
                clickedMessage.itemLongClick(view, getAdapterPosition());
            }
            return true;
        }
    }

    public MyAdapter(int size, SharedPreferences other_sp, SharedPreferences.Editor other_editor,
                     FirebaseFirestore db){
        data = new ArrayList<Message>();
        data_size = size;
        sharedPreferences = other_sp;
        editor = other_editor;
        gson = new Gson();
        dataBase = db;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.interior_one_message, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String message = data.get(position).getText();
        String timestamp = data.get(position).getTimeStamp();
        holder.textView.setText(message);
        holder.timestamp.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        if(data == null)
        {
            data = new ArrayList<>();
            return 0;
        }
        return data.size();
    }

    public void addMessage(String id, String timestamp, String message, String device){
        this.data.add(new Message(id, timestamp, message, device));
        data_size += 1;
        saveEditedData();
        notifyDataSetChanged();
    }

    public void deleteMessage(int position){
        new DeleteDataFromFireBase().execute(this.data.get(position).getId());
        this.data.remove(position);
        data_size -= 1;
        saveEditedData();
        notifyItemRemoved(position);
    }

    public void saveEditedData() {
        editor.putInt(MainActivity.SP_DATA_SIZE, this.data_size);
        String wjson = gson.toJson(this.data);
        editor.putString(MainActivity.SP_DATA_LIST, wjson);
        editor.apply();
    }

    public void setClickListener(recItemOnLongClick itemClick) {
        this.clickedMessage = itemClick;
    }

    public void loadData() {
        String rjson = sharedPreferences.getString(MainActivity.SP_DATA_LIST, "");
        Type type = new TypeToken<List<Message>>() {
        }.getType();
        this.data = gson.fromJson(rjson, type);
    }

    public static String getTime() {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
        return dateFormat.format(new Date());
    }

    public void supportConfigurationChange()
    {
        notifyDataSetChanged();
    }



    public void addToRemoteFireBase(final String message)
    {
        String currentTime = getTime();
        String device = Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE;
        incrementGlobalId();
        addDocument(CounterId, message, currentTime, device);
    }

    public void addDocument(final int id, final String message, String currentTime, String device)
    {
        Map<String, Object> sent_message = new HashMap<>();

        sent_message.put(MESSAGE_CONTENT, message);
        sent_message.put(MESSAGE_TIMESTAMP,currentTime);
        sent_message.put(MESSAGE_ID, id);
        sent_message.put(MESSAGE_DEVICE, device);

        dataBase.collection(COLLECTION_NAME)
                .document(id + "")
                .set(sent_message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(" ", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(" ", "Error writing document", e);
                    }
                });
    }

    public void incrementGlobalId()
    {
        CounterId++;
        DocumentReference washingtonRef = dataBase.collection(COLLECTION_NAME).
                document(DOCUMENT_ID);

        washingtonRef
                .update(PROJECT_ID, CounterId)
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

    public void getGlobalId()
    {
        DocumentReference docRef = dataBase.collection(COLLECTION_NAME).
                document(DOCUMENT_ID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        final String id = document.getData().get(PROJECT_ID) + "";
                        CounterId = Integer.parseInt(id);
                    } else {
                        Log.d("", "No such document");
                    }
                } else {
                    Log.d("", "get failed with ", task.getException());
                }
            }
        });
    }

    public void deleteDocument(String doc_id)
    {
        dataBase.collection(COLLECTION_NAME).document(doc_id)
                .delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(" ", "Error deleting document", e);
                    }
                });
    }

    public void loadDataFromRemoteFireBase()
    {
        final ArrayList<Message> d = new ArrayList<Message>();
        dataBase.collection(COLLECTION_NAME)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            String id, timestamp, content, device;
                            Map<String, Object> one_message;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if(!document.getId().equals(DOCUMENT_ID) &&
                                        !document.getId().equals(MainActivity.FIRESTORE_USER_ID))
                                {
                                    one_message = document.getData();
                                    id = one_message.get(MESSAGE_ID) + "";
                                    timestamp = one_message.get(MESSAGE_TIMESTAMP) + "";
                                    content = one_message.get(MESSAGE_CONTENT) + "";
                                    device = one_message.get(MESSAGE_DEVICE) + "";
                                    d.add(new Message(id, timestamp, content, device));
                                }
                            }

                            for (Message m: d)
                                addMessage(m.getId(), m.getTimeStamp(), m.getText(), m.getDevice());

                        } else {
                            Log.d(" ", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private class DeleteDataFromFireBase extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            deleteDocument(strings[0]);
            return null;
        }
    }
}

