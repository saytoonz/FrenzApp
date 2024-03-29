package com.nsromapa.frenzapp.newfy.adapters.searchFriends;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.models.Friends;
import com.nsromapa.frenzapp.newfy.ui.activities.friends.FriendProfile;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.marcoscg.dialogsheet.DialogSheet;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by SAY on 30/8/19.
 */

public class SearchFriendAdapter extends RecyclerView.Adapter<SearchFriendAdapter.ViewHolder> {

    private List<Friends> usersList;
    private Context context;
    private ViewHolder holderr;
    private View view;
    private HashMap<String, Object> userMap;
    private boolean exist;

    public SearchFriendAdapter(List<Friends> usersList, Context context, View view) {
        this.usersList = usersList;
        this.context = context;
        this.view = view;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder,int position) {

        holderr = holder;

        checkIfReqSent(holder);

        holder.listenerText.setText("Add as friend");

        holder.name.setText(usersList.get(position).getName());
        holder.username.setText("@"+usersList.get(position).getUsername());


        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                .load(usersList.get(position).getImage())
                .into(holder.image);

        holder.mView.setOnClickListener(view -> FriendProfile.startActivity(context,usersList.get(holder.getAdapterPosition()).userId));

        holder.exist_icon.setOnClickListener(view -> {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle("Information");
            dialog.setMessage("This icon indicates that you have send friend request to this user already.");
            dialog.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss()).setIcon(R.drawable.ic_call_made_black_24dp).show();
        });

        holder.friend_icon.setOnClickListener(view -> {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle("Information");
            dialog.setMessage("This icon indicates that this user is already your friend.");
            dialog.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss()).setIcon(R.drawable.ic_friend).show();
        });

    }

    private void checkIfReqSent(final ViewHolder holder) {

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(usersList.get(holder.getAdapterPosition()).userId)
                .collection("Friends")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.exist_icon.setVisibility(View.GONE);
                        holder.friend_icon.setVisibility(View.VISIBLE);
                    } else {
                        FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(usersList.get(holder.getAdapterPosition()).userId)
                                .collection("Friend_Requests")
                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .get().addOnSuccessListener(documentSnapshot1 -> {
                                    if (documentSnapshot1.exists()) {
                                        holder.friend_icon.setVisibility(View.GONE);
                                        holder.exist_icon.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.exist_icon.setVisibility(View.GONE);
                                        holder.friend_icon.setVisibility(View.GONE);
                                    }
                                });
                    }
                });

    }



    @Override
    public int getItemCount() {
        return usersList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void removeItem(final int position, final Snackbar snackbar, final int deletedIndex, final Friends deletedItem) {

        new DialogSheet(context)
                .setTitle("Add Friend")
                .setMessage("Do you want to add " + usersList.get(position).getName() + " to your friend list?")
                .setPositiveButton("Yes", v -> addUser(position, deletedIndex, deletedItem))
                .setNegativeButton("No", v -> notifyDataSetChanged())
                .setColoredNavigationBar(true)
                .setRoundedCorners(true)
                .show();

    }

    private void addUser(final int position, final int deletedIndex, final Friends deletedItem) {

        FirebaseFirestore.getInstance().collection("Users").document(usersList.get(position).userId)
                .collection("Friend_Requests").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        executeFriendReq(deletedIndex, deletedItem);
                    } else {
                        Snackbar.make(view, "Friend request has been sent already", Snackbar.LENGTH_LONG).show();
                        notifyDataSetChanged();
                    }
                });

    }

    private void executeFriendReq(final int deletedIndex, final Friends deletedItem) {

        userMap = new HashMap<>();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    final String email=documentSnapshot.getString("email");

                    userMap.put("name",  documentSnapshot.getString("name"));
                    userMap.put("id",    documentSnapshot.getString("id"));
                    userMap.put("email", email);
                    userMap.put("image", documentSnapshot.getString("image"));
                    userMap.put("tokens", documentSnapshot.get("token_ids"));
                    userMap.put("notification_id", String.valueOf(System.currentTimeMillis()));
                    userMap.put("timestamp",       String.valueOf(System.currentTimeMillis()));

                    //Add to user
                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(deletedItem.userId)
                            .collection("Friend_Requests")
                            .document(Objects.requireNonNull(documentSnapshot.getString("id")))
                            .set(userMap).addOnSuccessListener(aVoid -> {

                                //Add for notification data
                                FirebaseFirestore.getInstance()
                                        .collection("Notifications")
                                        .document(deletedItem.userId)
                                        .collection("Friend_Requests")
                                        .document(email)
                                        .set(userMap).addOnSuccessListener(aVoid1 -> {

                                            Snackbar.make(view, "Friend request sent to " + deletedItem.getName(), Snackbar.LENGTH_LONG).show();
                                            notifyDataSetChanged();

                                        }).addOnFailureListener(e -> Log.e("Error",e.getMessage()));


                            }).addOnFailureListener(e -> Log.e("Error",e.getMessage()));

                });

    }



    public class ViewHolder extends RecyclerView.ViewHolder {

        public CircleImageView image;
        View mView;
        TextView name, listenerText,username;
        RelativeLayout viewBackground, viewForeground;
        ImageView exist_icon, friend_icon;

        ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            image = mView.findViewById(R.id.image);
            name =  mView.findViewById(R.id.name);
            username= mView.findViewById(R.id.username);
            viewBackground = mView.findViewById(R.id.view_background);
            viewForeground = mView.findViewById(R.id.view_foreground);
            listenerText =  mView.findViewById(R.id.view_foreground_text);
            exist_icon = mView.findViewById(R.id.exist_icon);
            friend_icon = mView.findViewById(R.id.friend_icon);

        }
    }
}
