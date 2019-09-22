package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.MessageImageAdapter;
import com.nsromapa.frenzapp.newfy.adapters.MessageImageReplyAdapter;
import com.nsromapa.frenzapp.newfy.adapters.MessageTextAdapter;
import com.nsromapa.frenzapp.newfy.adapters.MessageTextReplyAdapter;
import com.nsromapa.frenzapp.newfy.models.Message;
import com.nsromapa.frenzapp.newfy.models.MessageReply;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.nsromapa.frenzapp.newfy.utils.StringUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

/**
 * Created by SAY on 30/8/19.
 */

public class MessageHistory extends Fragment {

    private View mView;
    private List<Message> messages;
    private List<MessageReply> messageReplies;
    private MessageImageAdapter messageImageAdapter;
    private MessageImageReplyAdapter messageImageReplyAdapter;
    private MessageTextAdapter messageTextAdapter;
    private MessageTextReplyAdapter messageTextReplyAdapter;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout refreshLayout;
    private String selected;
    private StringUtils stringUtils;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_messages_history, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messages=new ArrayList<>();
        messageReplies=new ArrayList<>();
        messageTextAdapter=new MessageTextAdapter(messages,mView.getContext());
        messageImageAdapter=new MessageImageAdapter(messages,mView.getContext());
        messageImageReplyAdapter=new MessageImageReplyAdapter(messageReplies,mView.getContext());
        messageTextReplyAdapter=new MessageTextReplyAdapter(messageReplies,mView.getContext());

        stringUtils = new StringUtils();

        mRecyclerView = mView.findViewById(R.id.messageList);
        TextView tab_1 = mView.findViewById(R.id.text);
        TextView tab_2 = mView.findViewById(R.id.text_reply);
        TextView tab_3 = mView.findViewById(R.id.image);
        TextView tab_4 = mView.findViewById(R.id.image_reply);
        refreshLayout=mView.findViewById(R.id.refreshLayout);

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        selected="null";

        tab_1.setOnClickListener(v -> {
            if(!selected.equals("text")) {
                messages.clear();
                messageTextAdapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(messageTextAdapter);
                getTextMessage();

                refreshLayout.setOnRefreshListener(() -> {
                    messages.clear();
                    messageTextAdapter.notifyDataSetChanged();
                    getTextMessage();
                });
                selected="text";
            }
        });

        tab_2.setOnClickListener(v -> {
            if(!selected.equals("reply")) {
                messageReplies.clear();
                messageTextReplyAdapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(messageTextReplyAdapter);
                getTextReplyMessage();

                refreshLayout.setOnRefreshListener(() -> {
                    messageReplies.clear();
                    messageTextReplyAdapter.notifyDataSetChanged();
                    getTextReplyMessage();
                });
                selected="reply";
            }

        });

        tab_3.setOnClickListener(v -> {
            if(!selected.equals("image")) {
                messages.clear();
                messageImageAdapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(messageImageAdapter);
                getImageMessage();

                refreshLayout.setOnRefreshListener(() -> {
                    messages.clear();
                    messageImageAdapter.notifyDataSetChanged();
                    getImageMessage();
                });
                selected="image";
            }
        });

        tab_4.setOnClickListener(v -> {
            if(!selected.equals("img_reply")) {
                messageReplies.clear();
                messageImageReplyAdapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(messageImageReplyAdapter);
                getImageReplyMessage();

                refreshLayout.setOnRefreshListener(() -> {
                    messageReplies.clear();
                    messageImageReplyAdapter.notifyDataSetChanged();
                    getImageReplyMessage();
                });
                selected="img_reply";
            }
        });

        tab_1.performClick();

    }

    public void getTextMessage(){

        refreshLayout.setRefreshing(true);
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        mFirestore.collection("Users")
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .collection("Notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {

                    if(e!=null){
                        refreshLayout.setRefreshing(false);
                        Toasty.error(mView.getContext(), stringUtils.getTechnical_error(),
                                Toasty.LENGTH_SHORT, true).show();
                        Log.w("error","listen",e);
                        return;
                    }

                    assert queryDocumentSnapshots != null;
                    if(!queryDocumentSnapshots.isEmpty()){

                        for(DocumentChange doc:queryDocumentSnapshots.getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                Message message = doc.getDocument().toObject(Message.class).withId(doc.getDocument().getId());
                                messages.add(message);
                                messageTextAdapter.notifyDataSetChanged();
                                refreshLayout.setRefreshing(false);

                            }

                        }

                        if(messages.isEmpty()){
                            refreshLayout.setRefreshing(false);
                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }

                    }else {
                        refreshLayout.setRefreshing(false);
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                    }

                });

    }

    public void getTextReplyMessage(){

        refreshLayout.setRefreshing(true);
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        mFirestore.collection("Users")
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .collection("Notifications_reply")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        if(!queryDocumentSnapshots.isEmpty()){

                            for(DocumentChange doc:queryDocumentSnapshots.getDocumentChanges()){

                                if(doc.getType()== DocumentChange.Type.ADDED){

                                    MessageReply messageReply=doc.getDocument().toObject(MessageReply.class)
                                            .withId(doc.getDocument().getId());
                                    messageReplies.add(messageReply);
                                    messageTextReplyAdapter.notifyDataSetChanged();
                                    refreshLayout.setRefreshing(false);
                                }


                            }

                            if(messageReplies.isEmpty()){
                                refreshLayout.setRefreshing(false);
                                mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                            }

                        }else {
                            refreshLayout.setRefreshing(false);
                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }


                    }
                })
                .addOnFailureListener(e -> {
                    refreshLayout.setRefreshing(false);
                    Toasty.error(mView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT, true).show();
                    Log.w("error","listen",e);
                });

    }

    public void getImageMessage(){

        refreshLayout.setRefreshing(true);
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        mFirestore.collection("Users")
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .collection("Notifications_image")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()){

                        for(DocumentChange doc:queryDocumentSnapshots.getDocumentChanges()){

                            if(doc.getType()== DocumentChange.Type.ADDED) {

                                Message message = doc.getDocument().toObject(Message.class)
                                        .withId(doc.getDocument().getId());
                                messages.add(message);
                                messageImageAdapter.notifyDataSetChanged();
                                refreshLayout.setRefreshing(false);
                            }

                        }
                        if(messages.isEmpty()){
                            refreshLayout.setRefreshing(false);
                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }

                    }else {
                        refreshLayout.setRefreshing(false);
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    refreshLayout.setRefreshing(false);
                    Toasty.error(mView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT, true).show();
                    Log.w("error","listen",e);
                });

    }

    public void getImageReplyMessage(){

        refreshLayout.setRefreshing(true);
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        mFirestore.collection("Users")
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .collection("Notifications_reply_image")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()){

                        for(DocumentChange doc:queryDocumentSnapshots.getDocumentChanges()){

                            if(doc.getType()== DocumentChange.Type.ADDED){

                                MessageReply messageReply=doc.getDocument().toObject(MessageReply.class)
                                        .withId(doc.getDocument().getId());
                                messageReplies.add(messageReply);
                                messageImageReplyAdapter.notifyDataSetChanged();
                                refreshLayout.setRefreshing(false);
                            }


                        }
                        if(messageReplies.isEmpty()){
                            refreshLayout.setRefreshing(false);
                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }

                    }else {
                        refreshLayout.setRefreshing(false);
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    refreshLayout.setRefreshing(false);
                    Toasty.error(mView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT, true).show();
                    Log.w("error","listen",e);

                });

    }


}
