package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.viewFriends.RecyclerViewTouchHelper;
import com.nsromapa.frenzapp.newfy.adapters.viewFriends.ViewFriendAdapter;
import com.nsromapa.frenzapp.newfy.models.ViewFriends;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
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

public class Friends extends Fragment {

    View mView;
    private List<ViewFriends> usersList;
    private ViewFriendAdapter usersAdapter;
    private FirebaseFirestore firestore;
    private SwipeRefreshLayout refreshLayout;
    private StringUtils stringUtils;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_view_friends, container, false);
        return mView;
    }

    public void startListening() {
        usersList.clear();
        usersAdapter.notifyDataSetChanged();
        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        refreshLayout.setRefreshing(true);

        stringUtils = new StringUtils();

        firestore.collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .collection("Friends")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                            if (doc.getType() == DocumentChange.Type.ADDED) {
                                ViewFriends users = doc.getDocument().toObject(ViewFriends.class);
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
                        refreshLayout.setRefreshing(false);
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    refreshLayout.setRefreshing(false);
                    Toasty.error(mView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT,true).show();
                    Log.w("Error", "listen:error", e);

                });
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        RecyclerView mRecyclerView = mView.findViewById(R.id.recyclerView);
        refreshLayout=mView.findViewById(R.id.refreshLayout);

        usersList = new ArrayList<>();
        usersAdapter = new ViewFriendAdapter(usersList, view.getContext());

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new RecyclerViewTouchHelper(0, ItemTouchHelper.LEFT, (viewHolder, direction, position) -> {
                    if (viewHolder instanceof ViewFriendAdapter.ViewHolder) {

                        usersAdapter.removeItem(viewHolder.getAdapterPosition());

                    }
                });

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(usersAdapter);

        refreshLayout.setOnRefreshListener(this::startListening);

        startListening();

    }
}
