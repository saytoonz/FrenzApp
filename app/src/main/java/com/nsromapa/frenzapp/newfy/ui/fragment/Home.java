package com.nsromapa.frenzapp.newfy.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.nsromapa.frenzapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import es.dmoral.toasty.Toasty;

import static com.nsromapa.frenzapp.newfy.ui.activities.MainActivity.add_post;
import static com.nsromapa.frenzapp.newfy.ui.activities.MainActivity.showFragment;
import static com.nsromapa.frenzapp.newfy.ui.activities.MainActivity.toolbar;

/**
 * Created by SAY on 30/8/19.
 */

public class Home extends Fragment implements BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener{


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       return inflater.inflate(R.layout.frag_dashboard, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView bottomNavigationView=view.findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setOnNavigationItemReselectedListener(this);

        loadfragment(new Feeds());
        checkFriendRequest();

    }

    public void loadfragment(androidx.fragment.app.Fragment fragment) {
        ((AppCompatActivity)getActivity()).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.action_home:
                add_post.setVisible(true);
                loadfragment(new Feeds());
                break;

            case R.id.action_discover:
                add_post.setVisible(false);
                loadfragment(new Discover());
                break;

        }
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.action_home:
                break;

            case R.id.action_discover:
                break;

        }
    }
    private CardView request_alert;
    private TextView request_alert_text;


    private void checkFriendRequest(){

        request_alert= Objects.requireNonNull(getView()).findViewById(R.id.friend_req_alert);
        request_alert_text=getView().findViewById(R.id.friend_req_alert_text);

        FirebaseFirestore.getInstance().collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .collection("Friend_Requests")
                .addSnapshotListener(getActivity(), (queryDocumentSnapshots, e) -> {

                    if(e!=null){
                        e.printStackTrace();
                        return;
                    }

                    assert queryDocumentSnapshots != null;
                    if(!queryDocumentSnapshots.isEmpty()){
                        try {
                            request_alert_text.setText(String.format(getString(R.string.you_have_d_new_friend_request_s), queryDocumentSnapshots.size()));
                            request_alert.setVisibility(View.VISIBLE);
                            request_alert.setAlpha(0.0f);

                            request_alert.animate()
                                    .setDuration(300)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .alpha(1.0f)
                                    .start();

                            request_alert.setOnClickListener(v -> {
                                    toolbar.setTitle("Manage Friends");
                                    try {
                                        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle("Manage Friends");
                                    } catch (Exception e12) {
                                        Log.e("Error", e12.getMessage());
                                    }
                                    showFragment(FriendsFragment.newInstance("request"));

                            });
                        }catch (Exception e1){
                            e1.printStackTrace();
                        }
                    }

                });

    }

}
