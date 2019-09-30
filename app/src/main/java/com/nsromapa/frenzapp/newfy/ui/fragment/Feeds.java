package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.PostsAdapter;
import com.nsromapa.frenzapp.newfy.models.Post;
import com.nsromapa.frenzapp.newfy.utils.StringUtils;
import com.nsromapa.frenzapp.saytalk.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

/**
 * Created by SAY on 30/8/19.
 */

public class Feeds extends Fragment {

    private List<Post> mPostsList;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;
    private View mView;
    private SwipeRefreshLayout refreshLayout;
    private PostsAdapter mAdapter_v19;
    private StringUtils stringUtils;
    private FloatingActionButton open_my_posts;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_home, container, false);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter_v19.notifyDataSetChanged();

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mFirestore = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        stringUtils = new StringUtils();

        refreshLayout = view.findViewById(R.id.refreshLayout);
        open_my_posts = view.findViewById(R.id.open_my_posts);
        View statsheetView = Objects.requireNonNull(getActivity())
                .getLayoutInflater().inflate(R.layout.stat_bottom_sheet_dialog, null);
        BottomSheetDialog mmBottomSheetDialog = new BottomSheetDialog(view.getContext());
        mmBottomSheetDialog.setContentView(statsheetView);
        mmBottomSheetDialog.setCanceledOnTouchOutside(true);
        RecyclerView mPostsRecyclerView = view.findViewById(R.id.posts_recyclerview);

        mPostsList = new ArrayList<>();

        mAdapter_v19 = new PostsAdapter(mPostsList, view.getContext(), getActivity(),
                mmBottomSheetDialog, statsheetView, false);
        mPostsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mPostsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPostsRecyclerView.setHasFixedSize(true);
        mPostsRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(),DividerItemDecoration.VERTICAL));
        mPostsRecyclerView.setAdapter(mAdapter_v19);

        refreshLayout.setOnRefreshListener(() -> {

            mPostsList.clear();
            mAdapter_v19.notifyDataSetChanged();
            getAllPosts();

        });

        open_my_posts.setOnClickListener(v->{
            Toast.makeText(getContext(), "open my posts", Toast.LENGTH_SHORT).show();
        });

        getAllPosts();
    }

    private void getAllPosts() {

        mView.findViewById(R.id.default_item).setVisibility(View.GONE);
        refreshLayout.setRefreshing(true);

        mFirestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {

                        for (final DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

//                                if (!Objects.equals(doc.getDocument().get("userId"),
//                                        Objects.requireNonNull(FirebaseUtils.INSTANCE.getUid()))){
                                    mFirestore.collection("Users")
                                            .document(currentUser.getUid())
                                            .collection("Friends")
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {

                                                if (!querySnapshot.isEmpty()) {

                                                    for (DocumentChange documentChange : querySnapshot.getDocumentChanges()) {
                                                        if (documentChange.getDocument().getId()
                                                                .equals(doc.getDocument().get("userId"))) {

                                                            Post post = doc.getDocument().toObject(Post.class)
                                                                    .withId(doc.getDocument().getId());
                                                            mPostsList.add(post);
                                                            refreshLayout.setRefreshing(false);
                                                            mAdapter_v19.notifyDataSetChanged();
                                                        }
                                                    }

                                                }

                                            })
                                            .addOnFailureListener(e -> {
                                                refreshLayout.setRefreshing(false);
                                                Toasty.error(mView.getContext(), stringUtils.getTechnical_error(), Toasty.LENGTH_SHORT,true).show();
                                                Log.w("Error", "listen:error", e);
                                            });

//                                }else{
//                                    Post post = doc.getDocument().toObject(Post.class).
//                                            withId(FirebaseUtils.INSTANCE.getUid());
//                                    mPostsList.add(post);
//                                    refreshLayout.setRefreshing(false);
//                                    mAdapter_v19.notifyDataSetChanged();
//                                }

                            }
                        }

//                        if (mPostsList.size()<1){
//                            if (refreshLayout.isRefreshing())
//                                refreshLayout.setRefreshing(false);
//                            mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
//                        }

                    }else{
                        refreshLayout.setRefreshing(false);
                        mView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);

                    }

                })
                .addOnFailureListener(e -> {
                    refreshLayout.setRefreshing(false);
                    Toasty.error(mView.getContext(),  stringUtils.getTechnical_error(), Toasty.LENGTH_SHORT,true).show();
                    Log.w("Error", "listen:error", e);
                });



    }


}
