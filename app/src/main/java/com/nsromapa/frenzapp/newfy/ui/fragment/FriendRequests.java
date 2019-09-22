package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.FriendRequestAdapter;
import com.nsromapa.frenzapp.newfy.models.FriendRequest;
import com.nsromapa.frenzapp.newfy.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

/**
 * Created by SAY on 30/8/19.
 */

public class FriendRequests extends Fragment {

    View mView;
    private List<FriendRequest> requestList;
    private FriendRequestAdapter requestAdapter;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private SwipeRefreshLayout refreshLayout;
    private StringUtils stringUtils;

    public void getUsers() {
        requestList.clear();
        requestAdapter.notifyDataSetChanged();

        stringUtils = new StringUtils();

        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        refreshLayout.setRefreshing(true);

        mFirestore.collection("Users")
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .collection("Friend_Requests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()) {

                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {
                                FriendRequest friendRequest = doc.getDocument().toObject(FriendRequest.class)
                                        .withId(doc.getDocument().getId());
                                requestList.add(friendRequest);
                                requestAdapter.notifyDataSetChanged();
                                refreshLayout.setRefreshing(false);
                            }

                        }

                        if(requestList.isEmpty()) {
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
                    Toast.makeText(mView.getContext(), stringUtils.getTechnical_error(), Toast.LENGTH_SHORT).show();
                    Log.w("Error", "listen:error", e);

                });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_friend_req, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView mRequestView = mView.findViewById(R.id.recyclerView);
        refreshLayout=mView.findViewById(R.id.refreshLayout);

        requestList = new ArrayList<>();
        requestAdapter = new FriendRequestAdapter(requestList, view.getContext());

        mRequestView.setItemAnimator(new DefaultItemAnimator());
        mRequestView.setLayoutManager(new LinearLayoutManager(view.getContext(), VERTICAL, false));
        mRequestView.addItemDecoration(new DividerItemDecoration(view.getContext(),DividerItemDecoration.VERTICAL));
        mRequestView.setHasFixedSize(true);
        mRequestView.setAdapter(requestAdapter);

        refreshLayout.setOnRefreshListener(this::getUsers);

        getUsers();

    }
}
