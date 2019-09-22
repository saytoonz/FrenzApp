package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.UsersAdapter;
import com.nsromapa.frenzapp.newfy.models.Users;
import com.nsromapa.frenzapp.newfy.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

/**
 * Created by SAY on 30/8/19.
 */

public class SendMessage extends Fragment {

    private View mView;
    private List<Users> usersList;
    private UsersAdapter usersAdapter;
    private FirebaseFirestore firestore;
    private SwipeRefreshLayout refreshLayout;
    private StringUtils stringUtils;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.send_message_fragment, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();

        RecyclerView mRecyclerView = mView.findViewById(R.id.messageList);
        refreshLayout=mView.findViewById(R.id.refreshLayout);

        stringUtils = new StringUtils();

        usersList = new ArrayList<>();
        usersAdapter = new UsersAdapter(usersList, view.getContext());
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(usersAdapter);

        refreshLayout.setOnRefreshListener(() -> {
            usersList.clear();
            usersAdapter.notifyDataSetChanged();
            startListening();
        });
        usersList.clear();
        usersAdapter.notifyDataSetChanged();
        startListening();

    }

    public void startListening() {
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        refreshLayout.setRefreshing(true);

        firestore.collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .collection("Friends")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                            if (doc.getType() == DocumentChange.Type.ADDED) {
                                Users users = doc.getDocument().toObject(Users.class).withId(doc.getDocument().getId());
                                usersList.add(users);
                                usersAdapter.notifyDataSetChanged();
                                refreshLayout.setRefreshing(false);
                            }
                        }

                        if(usersList.isEmpty()){
                            refreshLayout.setRefreshing(false);
                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }

                    }else{
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        refreshLayout.setRefreshing(false);
                    }

                })
                .addOnFailureListener(e -> {

                    Toasty.error(mView.getContext(),  stringUtils.getTechnical_error(), Toasty.LENGTH_SHORT,true).show();
                    refreshLayout.setRefreshing(false);
                    Log.w("Error", "listen:error", e);

                });
    }

}
