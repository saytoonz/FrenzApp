package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.PostsAdapter;
import com.nsromapa.frenzapp.newfy.models.Post;
import com.nsromapa.frenzapp.newfy.ui.activities.MainActivity;
import com.nsromapa.frenzapp.newfy.ui.activities.notification.ImagePreview;
import com.nsromapa.frenzapp.newfy.utils.AnimationUtil;
import com.nsromapa.frenzapp.newfy.utils.StringUtils;
import com.nsromapa.frenzapp.newfy.utils.database.UserHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;
import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

/**
 * Created by SAY on 30/8/19.
 */

public class ProfileFragment extends Fragment {

    View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_profile_view, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadFragment(new ProfileFragment.AboutFragment());

        BottomNavigationView bottomNavigationView=mView.findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_posts:
                    loadFragment(new PostsFragment());
                    break;
                case R.id.action_saved:
                    loadFragment(new SavedFragment());
                    break;
                case R.id.action_edit:
                    loadFragment(new EditFragment());
                    break;
                case R.id.action_profile:
                default:
                    loadFragment(new AboutFragment());

            }
            return true;
        });

        bottomNavigationView.setOnNavigationItemReselectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_profile:
                    break;
                case R.id.action_posts:
                    break;
                case R.id.action_saved:
                    break;
                case R.id.action_edit:
                    break;

            }
        });


    }

    private void loadFragment(Fragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit();
    }

    public static class PostsFragment extends Fragment {

        List<Post> postList;
        private SwipeRefreshLayout refreshLayout;
        private PostsAdapter mAdapter_v19;
        private View rootView;
        private StringUtils stringUtils;

        public PostsFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.main_drawer, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            View statsheetView = getActivity().getLayoutInflater().inflate(R.layout.stat_bottom_sheet_dialog, null);
            BottomSheetDialog mmBottomSheetDialog = new BottomSheetDialog(rootView.getContext());
            mmBottomSheetDialog.setContentView(statsheetView);
            mmBottomSheetDialog.setCanceledOnTouchOutside(true);

            refreshLayout=rootView.findViewById(R.id.refreshLayout);

            postList=new ArrayList<>();

            RecyclerView mRecyclerView = rootView.findViewById(R.id.recyclerView);

            stringUtils = new StringUtils();


            TextView title = rootView.findViewById(R.id.default_title);
            TextView text = rootView.findViewById(R.id.default_text);
            ImageView image = rootView.findViewById(R.id.imageview);

            image.setImageDrawable(getResources().getDrawable(R.drawable.ic_photo_camera_black_24dp));
            title.setText(stringUtils.no_post_found());
            text.setText(stringUtils.add_some_post_to_see());

            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext(), VERTICAL, false));
            mRecyclerView.setHasFixedSize(true);

            mAdapter_v19 = new PostsAdapter(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext(),DividerItemDecoration.VERTICAL));
            mRecyclerView.setAdapter(mAdapter_v19);

            refreshLayout.setOnRefreshListener(() -> {
                postList.clear();
                mAdapter_v19.notifyDataSetChanged();
                getPosts();
            });
            getPosts();

        }

        private void getPosts() {
            refreshLayout.setRefreshing(true);
            rootView.findViewById(R.id.default_item).setVisibility(View.GONE);
            getCurrentUsersPosts();
        }



        private void getCurrentUsersPosts() {

            FirebaseFirestore.getInstance().collection("Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        if(queryDocumentSnapshots.isEmpty()){

                            refreshLayout.setRefreshing(false);
                            rootView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);

                        }else{

                            for (DocumentChange documentChange : queryDocumentSnapshots.getDocumentChanges()) {
                                if (Objects.equals(documentChange.getDocument().getString("userId"),
                                        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())) {

                                    Post post = documentChange.getDocument().toObject(Post.class)
                                            .withId(documentChange.getDocument().getId());
                                    postList.add(post);
                                    refreshLayout.setRefreshing(false);
                                    mAdapter_v19.notifyDataSetChanged();

                                }
                            }

                            if (postList.isEmpty()) {
                                rootView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                                refreshLayout.setRefreshing(false);
                            }

                        }

                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                                Toasty.LENGTH_SHORT,true).show();
                        Log.w("Error", "listen:error", e);
                    });


        }

    }

    public static class SavedFragment extends Fragment {

        List<Post> postList;
        private SwipeRefreshLayout refreshLayout;
        private PostsAdapter mAdapter_v19;
        private View rootView;
        private StringUtils stringUtils;

        public SavedFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.main_drawer, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            postList=new ArrayList<>();

            View statsheetView = getActivity().getLayoutInflater().inflate(R.layout.stat_bottom_sheet_dialog, null);
            BottomSheetDialog mmBottomSheetDialog = new BottomSheetDialog(rootView.getContext());
            mmBottomSheetDialog.setContentView(statsheetView);
            mmBottomSheetDialog.setCanceledOnTouchOutside(true);
            refreshLayout=rootView.findViewById(R.id.refreshLayout);

            TextView title = rootView.findViewById(R.id.default_title);
            TextView text = rootView.findViewById(R.id.default_text);
            ImageView image = rootView.findViewById(R.id.imageview);

            stringUtils = new StringUtils();

            image.setImageDrawable(getResources().getDrawable(R.drawable.ic_bookmark_black_24dp));
            title.setText(stringUtils.no_saved_post_found());
            text.setText(stringUtils.all_your_saved_post());

            RecyclerView mRecyclerView = rootView.findViewById(R.id.recyclerView);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext(), VERTICAL, false));
            mRecyclerView.setHasFixedSize(true);

            mAdapter_v19 = new PostsAdapter(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext(),DividerItemDecoration.VERTICAL));
            mRecyclerView.setAdapter(mAdapter_v19);

            refreshLayout.setOnRefreshListener(() -> {

                postList.clear();
                mAdapter_v19.notifyDataSetChanged();
                getPosts();

            });

            getPosts();

        }

        private void getPosts() {

            rootView.findViewById(R.id.default_item).setVisibility(View.GONE);
            refreshLayout.setRefreshing(true);

            FirebaseFirestore.getInstance().collection("Users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("Saved_Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {

                        if(!querySnapshot.isEmpty()){
                            for(final DocumentChange doc:querySnapshot.getDocumentChanges()){

                                FirebaseFirestore.getInstance().collection("Posts")
                                        .document(doc.getDocument().getId())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if(documentSnapshot.exists()){
                                                Post post = doc.getDocument()
                                                        .toObject(Post.class)
                                                        .withId(doc.getDocument().getId());
                                                postList.add(post);
                                                mAdapter_v19.notifyDataSetChanged();
                                                refreshLayout.setRefreshing(false);
                                            }else{
                                                FirebaseFirestore.getInstance().collection("Users")
                                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                        .collection("Saved_Posts")
                                                        .document(doc.getDocument().getId())
                                                        .delete()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                refreshLayout.setRefreshing(false);
                                                                if(postList.isEmpty()) {
                                                                    rootView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                                                                }
                                                                Log.e("Saved_users","Post not available");
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                refreshLayout.setRefreshing(false);
                                                                Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                                                                        Toasty.LENGTH_SHORT, true).show();
                                                                Log.e("Error",e.getMessage());
                                                            }
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            refreshLayout.setRefreshing(false);
                                            Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                                                    Toasty.LENGTH_SHORT, true).show();
                                            Log.e("Error",e.getMessage());
                                        });

                            }
                        }else{
                            refreshLayout.setRefreshing(false);
                            rootView.findViewById(R.id.default_item).setVisibility(View.VISIBLE);
                        }

                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                                Toasty.LENGTH_SHORT, true).show();
                        Log.e("Error",e.getMessage());
                    });

        }



    }

    public static class AboutFragment extends Fragment {

        private TextView post;
        private TextView friend;

        public AboutFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.frag_about_profile, container, false);


            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

            CircleImageView profile_pic = rootView.findViewById(R.id.profile_pic);
            TextView name = rootView.findViewById(R.id.name);
            TextView username = rootView.findViewById(R.id.username);
            TextView email = rootView.findViewById(R.id.email);
            TextView location = rootView.findViewById(R.id.location);
            post=rootView.findViewById(R.id.posts);
            friend=rootView.findViewById(R.id.friends);
            TextView bio = rootView.findViewById(R.id.bio);

            rootView.findViewById(R.id.frame).setVisibility(View.GONE);

            StringUtils stringUtils = new StringUtils();

            mFirestore.collection("Users")
                    .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                    .collection("Friends")
                    .get()
                    .addOnSuccessListener(documentSnapshots -> {
                        //Total Friends
                        friend.setText(String.format(Locale.ENGLISH,"Total Friends : %d",
                                documentSnapshots.size()));
                    });

            UserHelper userHelper = new UserHelper(rootView.getContext());

            Cursor rs = userHelper.getData(1);
            rs.moveToFirst();

            String usernam=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_USERNAME));
            String nam = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_NAME));
            String emai = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_EMAIL));
            final String imag = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_IMAGE));
            String loc=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_LOCATION));
            String bi=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_BIO));

            if (!rs.isClosed()) {
                rs.close();
            }
            username.setText(String.format(Locale.ENGLISH,"@%s", usernam));
            name.setText(nam);
            email.setText(emai);
            location.setText(loc);
            bio.setText(bi);

            Glide.with(rootView.getContext())
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                    .load(imag)
                    .into(profile_pic);

            profile_pic.setOnLongClickListener(v -> {
                rootView.getContext().startActivity(new Intent(rootView.getContext(),ImagePreview.class)
                        .putExtra("url",imag));
                return false;
            });

            FirebaseFirestore.getInstance().collection("Posts")
                    .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                            post.setText(String.format(Locale.ENGLISH,"Total Posts : %d",querySnapshot.size())));


            return rootView;
        }



    }

    public static class EditFragment extends Fragment {

        private FirebaseAuth mAuth;
        private FirebaseFirestore mFirestore;
        private UserHelper userHelper;

        private TextInputEditText name,username,email,bio,location;
        private CircleImageView profile_pic;
        private AuthCredential credential;
        private static final int PICK_IMAGE =100 ;

        private Uri imageUri=null;
        private View rootView;
        private StringUtils stringUtils;

        public EditFragment() {
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.frag_edit_profile, container, false);

            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();

            stringUtils = new StringUtils();

            name=rootView.findViewById(R.id.name);
            username=rootView.findViewById(R.id.username);
            email=rootView.findViewById(R.id.email);
            bio=rootView.findViewById(R.id.bio);
            location=rootView.findViewById(R.id.location);
            profile_pic=rootView.findViewById(R.id.profile_pic);
            TextView updatebtn = rootView.findViewById(R.id.update);
            TextView remove_image = rootView.findViewById(R.id.remove_image);
            TextView updatepassbtn = rootView.findViewById(R.id.change_password);
            TextView updatepicture = rootView.findViewById(R.id.picture);

            userHelper = new UserHelper(rootView.getContext());

            Cursor rs = userHelper.getData(1);
            rs.moveToFirst();

            final String usernam=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_USERNAME));
            final String nam = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_NAME));
            final String emai = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_EMAIL));
            final String imag = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_IMAGE));
            final String bi = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_BIO));
            final String loc = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_LOCATION));
            final String pass = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_PASS));

            if (!rs.isClosed()) {
                rs.close();
            }


            remove_image.setOnClickListener(view->{
                if(isOnline()){
                    final ProgressDialog dialog=new ProgressDialog(getActivity());
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setMessage(stringUtils.removing_profile_image());
                    dialog.show();

                    Map<String,Object> map=new HashMap<>();
                    map.put("image"," ");

                    mFirestore.collection("Users")
                            .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                            .update(map)
                            .addOnSuccessListener(aVoid -> {
                                profile_pic.setImageURI(imageUri);
                                MainActivity.imageView.setImageURI(imageUri);

                                Map<String,Object> map2=new HashMap<>();
                                map2.put("profile_pic_url","");
                                FirebaseDatabase.getInstance()
                                        .getReference().child("users")
                                        .child(mAuth.getCurrentUser().getUid())
                                        .updateChildren(map2);

                                profile_pic.setImageResource(R.drawable.default_user_art_g_2);

                                userHelper.updateContactImage(1,"");
                                dialog.dismiss();
                                Log.i("Update","success");
                            })
                            .addOnFailureListener(e -> {
                                Log.i("Update","failed: "+e.getMessage());
                                Toasty.error(rootView.getContext(), stringUtils.couldnt_remove_image(), Toasty.LENGTH_SHORT, true).show();
                                dialog.dismiss();
                            });
                }else{
                    Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT, true).show();
                }
            });


            updatepicture.setOnClickListener(v -> {

                if(isOnline()){

                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE);

                }else{
                    Toasty.error(rootView.getContext(), stringUtils.getTechnical_error(),
                            Toasty.LENGTH_SHORT, true).show();
                }

            });

            updatepassbtn.setOnClickListener(v -> {

                if(isOnline()) {

                    new MaterialDialog.Builder(rootView.getContext())
                            .title("Change Password")
                            .content("Enter your old password.")
                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                            .input("Old password", "", (mdialog, input) -> {

                                if (!input.toString().equals(pass)) {
                                    mdialog.dismiss();
                                    Toasty.error(rootView.getContext(), stringUtils.invalid_password(),
                                            Toasty.LENGTH_SHORT,true).show();
                                } else {

                                    new MaterialDialog.Builder(rootView.getContext())
                                            .title("Change Password")
                                            .content("Enter new password.")
                                            .inputType(InputType.TYPE_CLASS_TEXT |
                                                    InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                            .input("New password", "", (mdialog1, input1) -> {

                                                if(TextUtils.isEmpty(input1.toString())){
                                                    mdialog1.dismiss();
                                                    Toasty.error(rootView.getContext(),
                                                            stringUtils.invalid_new_password(), Toasty.LENGTH_SHORT,true).show();
                                                }else if(input1.toString().length()<6){
                                                    Toasty.error(rootView.getContext(),
                                                            stringUtils.password_length(), Toasty.LENGTH_SHORT,true).show();
                                                }else{

                                                    final ProgressDialog dialog=new ProgressDialog(rootView.getContext());
                                                    dialog.setMessage("Please wait...");
                                                    dialog.setIndeterminate(true);
                                                    dialog.setCancelable(false);
                                                    dialog.setCanceledOnTouchOutside(false);
                                                    dialog.show();

                                                    Objects.requireNonNull(mAuth.getCurrentUser()).updatePassword(input1.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            dialog.dismiss();
                                                            mdialog1.dismiss();
                                                            userHelper.updateContactPassword(1, input1.toString());
                                                            Toasty.success(rootView.getContext(), stringUtils.pass_updated(), Toasty.LENGTH_SHORT,true).show();
                                                        }
                                                    }).addOnFailureListener(e -> {
                                                        dialog.dismiss();
                                                        mdialog1.dismiss();
                                                        Toasty.error(rootView.getContext(), "Error updating password: "+e.getMessage(), Toasty.LENGTH_SHORT,true).show();
                                                        Log.e("password error",e.getLocalizedMessage());
                                                    });
                                                }

                                            }).show();


                                }
                            }
                            )
                            .show();

                }else{
                    Toasty.info(rootView.getContext(), stringUtils.go_online_to_change_pass(),
                            Toasty.LENGTH_SHORT,true).show();
                }
            });

            if(!isOnline()){
                rootView.findViewById(R.id.h_username).animate()
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                rootView.findViewById(R.id.h_username).setVisibility(View.GONE);
                            }
                        }).start();

                rootView.findViewById(R.id.h_email).animate()
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                rootView.findViewById(R.id.h_email).setVisibility(View.GONE);
                            }
                        }).start();

            }

            username.setText(usernam);
            name.setText(nam);
            email.setText(emai);
            bio.setText(bi);
            location.setText(loc);

            Glide.with(rootView.getContext())
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                    .load(imag)
                    .into(profile_pic);

            profile_pic.setOnLongClickListener(v -> {
                rootView.getContext().startActivity(new Intent(rootView.getContext(),ImagePreview.class)
                        .putExtra("url",imag));
                return false;
            });

            updatebtn.setOnClickListener(v -> {

                final ProgressDialog dialog=new ProgressDialog(getActivity());
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);

                if(isOnline()){

                    final DocumentReference userDocument = mFirestore.collection("Users").document(mAuth.getCurrentUser().getUid());
                    final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
                    if(imageUri!=null){

                        dialog.setMessage("Updating Details....");
                        dialog.show();

                        final String userUid = mAuth.getCurrentUser().getUid();
                        final StorageReference user_profile = FirebaseStorage.getInstance().getReference().child("images").child(userUid + ".png");
                        user_profile.putFile(imageUri).addOnCompleteListener(task -> {

                            if(task.isSuccessful()){

                                user_profile.getDownloadUrl().addOnSuccessListener(uri -> {
                                    Map<String,Object> map=new HashMap<>();
                                    map.put("image",uri.toString());

                                    userDocument.update(map)
                                            .addOnSuccessListener(aVoid -> {
                                                profile_pic.setImageURI(imageUri);
                                                MainActivity.imageView.setImageURI(imageUri);

                                                Map<String,Object> map2=new HashMap<>();
                                                map2.put("profile_pic_url",uri.toString());
                                                userRef.updateChildren(map2);

                                                userHelper.updateContactImage(1,uri.toString());
                                                dialog.dismiss();
                                                Log.i("Update","success");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.i("Update","failed: "+e.getMessage());
                                                dialog.dismiss();
                                            });
                                }).addOnFailureListener(e -> Log.e("Error","listen",e));

                            }else{
                                Log.e("Error","listen",task.getException());
                            }

                        });

                    }

                    if(!Objects.requireNonNull(email.getText()).toString().equals(emai)){
                        dialog.setMessage("Updating Details....");

                        new MaterialDialog.Builder(rootView.getContext())
                                .title("Email changed")
                                .content("It seems that you have changed your email, re-enter your password to change.")
                                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                .input("Password", "", (mdialog, input) -> {
                                    if(!input.toString().equals(pass)){
                                        dialog.dismiss();
                                        mdialog.show();
                                        Toasty.error(rootView.getContext(), stringUtils.invalid_password(), Toasty.LENGTH_SHORT,true).show();
                                    }else{

                                        mdialog.dismiss();
                                        final FirebaseUser currentuser=mAuth.getCurrentUser();

                                        credential = EmailAuthProvider
                                                .getCredential(Objects.requireNonNull(currentuser.getEmail()), input.toString());

                                        currentuser.reauthenticate(credential)
                                                .addOnCompleteListener(task -> currentuser.updateEmail(email.getText().toString()).addOnCompleteListener(task1 -> {

                                                    if (task1.isSuccessful()) {

                                                        currentuser.sendEmailVerification().addOnSuccessListener(aVoid -> {

                                                            Map<String, Object> userMap = new HashMap<>();
                                                            userMap.put("email", email.getText().toString());

                                                            FirebaseFirestore.getInstance().collection("Users")
                                                                    .document(mAuth.getCurrentUser().getUid())
                                                                    .update(userMap)
                                                                    .addOnSuccessListener(aVoid13 -> {
                                                                        dialog.dismiss();
                                                                        userHelper.updateContactEmail(1,  email.getText().toString());
                                                                        Toasty.success(rootView.getContext(),"Verification email sent.",
                                                                                Toasty.LENGTH_SHORT,true).show();
                                                                        dialog.dismiss();
                                                                    }).addOnFailureListener(e -> {
                                                                        dialog.dismiss();
                                                                        Log.e("Update","failed: "+e.getLocalizedMessage());
                                                                    });

                                                        }).addOnFailureListener(e -> {
                                                            dialog.dismiss();
                                                            Log.e("Error",e.getLocalizedMessage());
                                                            dialog.dismiss();
                                                        });

                                                    } else {

                                                        Log.e("Update email error", Objects.requireNonNull(task1.getException()).getMessage() + "..");
                                                        dialog.dismiss();

                                                    }

                                                }));

                                    }
                                })
                                .positiveText("Done")
                                .onPositive((mdialog, which) -> {
                                    dialog.show();
                                    mdialog.dismiss();
                                })
                                .negativeText("Don't change my email")
                                .onNegative((mdialog, which) -> {
                                    dialog.dismiss();
                                    mdialog.dismiss();
                                })
                                .cancelable(false)
                                .canceledOnTouchOutside(false)
                                .show();



                    }

                    if(!Objects.requireNonNull(name.getText()).toString().equals(nam)){

                        dialog.setMessage("Updating Details....");
                        dialog.show();

                        Map<String,Object> map=new HashMap<>();
                        map.put("name",name.getText().toString());

                        userDocument.update(map)
                                .addOnSuccessListener(aVoid -> {
                                    MainActivity.username.setText(nam);

                                    Map<String,Object> map2=new HashMap<>();
                                    map2.put("full_name", name.getText().toString());
                                    userRef.updateChildren(map2);

                                    userHelper.updateContactName(1,name.getText().toString());
                                    dialog.dismiss();
                                    Log.i("Update","success");
                                })
                                .addOnFailureListener(e -> {
                                    Log.i("Update","failed: "+e.getMessage());
                                    dialog.dismiss();
                                });

                    }

                    if(!Objects.requireNonNull(username.getText()).toString().equals(usernam)){

                        dialog.setMessage("Updating Details....");
                        dialog.show();

                        mFirestore.collection("Usernames")
                                .document(username.getText().toString())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if(!documentSnapshot.exists()){

                                        mFirestore.collection("Usernames")
                                                .document(usernam)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {

                                                    Map<String,Object> map=new HashMap<>();
                                                    map.put("username",username.getText().toString());

                                                    mFirestore.collection("Usernames")
                                                            .document(username.getText().toString())
                                                            .set(map)
                                                            .addOnSuccessListener(aVoid12 -> {

                                                                Map<String,Object> map1 =new HashMap<>();
                                                                map1.put("username",username.getText().toString());

                                                                userDocument.update(map1)
                                                                        .addOnSuccessListener(aVoid1 -> {
                                                                            dialog.dismiss();


                                                                            Map<String,Object> map2=new HashMap<>();
                                                                            map2.put("name", username.getText().toString());
                                                                            userRef.updateChildren(map2);

                                                                            userHelper.updateContactUserName(1,username.getText().toString());
                                                                            Log.i("Update","success");
                                                                        })
                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                dialog.dismiss();
                                                                                Log.i("Update","failed: "+e.getMessage());

                                                                            }
                                                                        });

                                                            })
                                                            .addOnFailureListener(e -> {
                                                                dialog.dismiss();
                                                                Log.i("error","failed: "+e.getMessage());
                                                            });

                                                })
                                                .addOnFailureListener(e -> {
                                                    dialog.dismiss();
                                                    Log.i("error","failed: "+e.getMessage());

                                                });



                                    }else{

                                        dialog.dismiss();
                                        Toasty.error(rootView.getContext(), stringUtils.username_exists(),
                                                Toasty.LENGTH_SHORT,true).show();
                                        AnimationUtil.shakeView(username,rootView.getContext());

                                    }
                                })
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Log.e("error",""+e.getLocalizedMessage());
                                });

                    }

                    if(!Objects.requireNonNull(bio.getText()).toString().equals(bi)){

                        dialog.setMessage("Updating Details....");
                        dialog.show();

                        Map<String,Object> map=new HashMap<>();
                        map.put("bio",bio.getText().toString());

                        userDocument.update(map)
                                .addOnSuccessListener(aVoid -> {
                                    dialog.dismiss();

                                    Map<String,Object> map2=new HashMap<>();
                                    map2.put("bio", bio.getText().toString());
                                    userRef.updateChildren(map2);

                                    userHelper.updateContactBio(1,bio.getText().toString());
                                    Log.i("Update","success");
                                })
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Log.i("Update","failed: "+e.getMessage());

                                });

                    }

                    if(!Objects.requireNonNull(location.getText()).toString().equals(loc)){

                        dialog.setMessage("Updating Details....");
                        dialog.show();

                        Map<String,Object> map=new HashMap<>();
                        map.put("location",location.getText().toString());

                        userDocument.update(map)
                                .addOnSuccessListener(aVoid -> {
                                    dialog.dismiss();

                                    Map<String,Object> map2=new HashMap<>();
                                    map2.put("city", location.getText().toString());
                                    userRef.updateChildren(map2);

                                    userHelper.updateContactLocation(1,location.getText().toString());
                                    Log.i("Update","success");

                                })
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Log.i("Update","failed: "+e.getMessage());

                                });

                    }


                }else{


                    if(!Objects.requireNonNull(name.getText()).toString().equals(nam))
                        userHelper.updateContactName(1, name.getText().toString());

                    if(!Objects.requireNonNull(bio.getText()).toString().equals(bi))
                        userHelper.updateContactBio(1, bio.getText().toString());

                    if(!Objects.requireNonNull(location.getText()).toString().equals(loc))
                        userHelper.updateContactLocation(1, location.getText().toString());

                    Toasty.info(rootView.getContext(),stringUtils.will_be_updated_when_online(),
                            Toasty.LENGTH_LONG).show();


                }

            });

            return rootView;
        }

        public boolean isOnline() {
            ConnectivityManager cm =
                    (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if(requestCode==PICK_IMAGE){
                if(resultCode==RESULT_OK){
                    imageUri=data.getData();
                    // start crop activity
                    UCrop.Options options = new UCrop.Options();
                    options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                    options.setCompressionQuality(100);
                    options.setShowCropGrid(true);

                    UCrop.of(imageUri, Uri.fromFile(new File(rootView.getContext().getCacheDir(),
                            "frenzapp_user_profile_picture.png")))
                            .withAspectRatio(1, 1)
                            .withOptions(options)
                            .start(getActivity());

                }
            }
            if (requestCode == UCrop.REQUEST_CROP) {
                if (resultCode == RESULT_OK) {
                    try {
                        File compressedFile= new Compressor(rootView.getContext())
                                .setCompressFormat(Bitmap.CompressFormat.PNG)
                                .setQuality(70)
                                .setMaxHeight(96)
                                .setMaxWidth(96)
                                .compressToFile(new File(Objects.requireNonNull(UCrop.getOutput(data)).getPath()));
                        profile_pic.setImageURI(Uri.fromFile(compressedFile));
						imageUri=Uri.fromFile(compressedFile);
                        Toasty.info(rootView.getContext(),
                                stringUtils.profile_pic_uploaded(),
                                Toasty.LENGTH_LONG,true).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toasty.info(rootView.getContext(),
                                "Profile photo updated click Save details to apply but unable to compress: "+e.getLocalizedMessage(),
                                Toasty.LENGTH_SHORT,true).show();
                        profile_pic.setImageURI(imageUri);
                        imageUri = UCrop.getOutput(data);
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    Log.e("Error", "Crop error:" + Objects.requireNonNull(UCrop.getError(data)).getMessage());
                }
            }

        }
    }

}
