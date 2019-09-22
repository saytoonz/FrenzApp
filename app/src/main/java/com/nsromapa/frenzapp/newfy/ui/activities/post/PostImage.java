package com.nsromapa.frenzapp.newfy.ui.activities.post;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.marcoscg.dialogsheet.DialogSheet;
import com.nsromapa.frenzapp.R;
import com.nsromapa.frenzapp.newfy.adapters.PagerPhotosAdapter;
import com.nsromapa.frenzapp.newfy.models.Images;
import com.nsromapa.frenzapp.newfy.service.UploadService;
import com.nsromapa.frenzapp.newfy.utils.AnimationUtil;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import es.dmoral.toasty.Toasty;
import id.zelory.compressor.Compressor;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

import static android.view.View.GONE;

public class PostImage extends AppCompatActivity {

    private FirebaseUser mCurrentUser;
    private EditText mEditText;
    List<Images> imagesList;
    ViewPager pager;
    PagerPhotosAdapter adapter;
    private DotsIndicator indicator;
    private RelativeLayout indicator_holder;
    private int selectedIndex;
    private SharedPreferences sharedPreferences;
    private int serviceCount;
    ArrayList<String> uploadedImagesUrl=new ArrayList<>();


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, PostImage.class);
        context.startActivity(intent);
    }

    @NonNull
    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title("Discard")
                .content("Do you want to go back?")
                .positiveText("Yes")
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .onPositive((dialog, which) -> finish())
                .negativeText("No")
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/bold.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
        setContentView(R.layout.activity_post_image);

        imagesList=getIntent().getParcelableArrayListExtra("imagesList");

        if(imagesList.isEmpty()){
            finish();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("New Image Post");

        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle("New Image Post");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pager=findViewById(R.id.pager);
        indicator=findViewById(R.id.indicator);
        indicator_holder=findViewById(R.id.indicator_holder);

        indicator.setDotsClickable(true);
        adapter=new PagerPhotosAdapter(this,imagesList);
        pager.setAdapter(adapter);

        if(imagesList.size()>1){
            indicator_holder.setVisibility(View.VISIBLE);
            indicator.setViewPager(pager);
        }else{
            indicator_holder.setVisibility(GONE);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        sharedPreferences=getSharedPreferences("uploadservice",MODE_PRIVATE);
        serviceCount=sharedPreferences.getInt("count",0);

        mEditText = findViewById(R.id.text);

        Compressor compressor = new Compressor(this)
                .setQuality(85)
                .setCompressFormat(Bitmap.CompressFormat.PNG);

        ProgressDialog mDialog = new ProgressDialog(this);
        StorageReference mStorage = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_image_post, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_post) {
            if (TextUtils.isEmpty(mEditText.getText().toString()))
                AnimationUtil.shakeView(mEditText, PostImage.this);
            else {
                //uploadImages(0);

                new DialogSheet(this)
                        .setRoundedCorners(true)
                        .setColoredNavigationBar(true)
                        .setPositiveButton("Yes", v -> {

                            sharedPreferences.edit().putInt("count", ++serviceCount).apply();

                            Intent intent = new Intent(PostImage.this, UploadService.class);
                            intent.putExtra("count", serviceCount);
                            intent.putStringArrayListExtra("uploadedImagesUrl", uploadedImagesUrl);
                            intent.putParcelableArrayListExtra("imagesList", (ArrayList<? extends Parcelable>) imagesList);
                            intent.putExtra("notification_id", (int) System.currentTimeMillis());
                            intent.putExtra("current_id", mCurrentUser.getUid());
                            intent.putExtra("description", mEditText.getText().toString());
                            intent.setAction(UploadService.ACTION_START_FOREGROUND_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                            Toasty.info(PostImage.this, "Uploading images..",
                                    Toasty.LENGTH_SHORT, true).show();
                            finish();

                        })
                        .setNegativeButton("No", v -> {
                        })
                        .setTitle("Upload")
                        .setMessage("Is this the content you want to upload?")
                        .show();

            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void deleteItem(View view) {

        new MaterialDialog.Builder(this)
                .title("Remove")
                .content("Do you want to remove this image?")
                .positiveText("Yes")
                .onPositive((dialog, which) -> {

                    if(imagesList.size()==1) {
                        finish();
                        return;
                    }

                    imagesList.remove(pager.getCurrentItem());

                    adapter=new PagerPhotosAdapter(PostImage.this,imagesList);
                    pager.setAdapter(adapter);
                    indicator.setViewPager(pager);

                    if(imagesList.size()>1){
                        indicator_holder.setVisibility(View.VISIBLE);
                        indicator.setViewPager(pager);
                    }else{
                        indicator_holder.setVisibility(GONE);
                    }

                })
                .negativeText("No")
                .show();
    }

    public void openCropItem(View view) {

        selectedIndex=pager.getCurrentItem();
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        options.setCompressionQuality(75);
        options.setShowCropGrid(true);


        UCrop.of(Uri.fromFile(new File(imagesList.get(selectedIndex).getOg_path())), Uri.fromFile(new File(getCacheDir(), imagesList.get(selectedIndex).getName() + "_" + random() + "_edit.png")))
                .withAspectRatio(1, 1)
                .withOptions(options)
                .start(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==UCrop.REQUEST_CROP && resultCode==RESULT_OK){

            long old_id=imagesList.get(selectedIndex).getId();
            String old_name=imagesList.get(selectedIndex).getName();
            String old_path=imagesList.get(selectedIndex).getOg_path();

            imagesList.remove(selectedIndex);
            assert data != null;
            imagesList.add(selectedIndex,new Images(old_name,old_path, Objects.requireNonNull(UCrop.getOutput(data)).getPath(),old_id));
            adapter=new PagerPhotosAdapter(this,imagesList);
            pager.setAdapter(adapter);
            indicator.setViewPager(pager);
            adapter.notifyDataSetChanged();
            pager.setCurrentItem(selectedIndex,true);

        }else if(resultCode==UCrop.RESULT_ERROR){
            assert data != null;
            Throwable throwable=UCrop.getError(data);
            assert throwable != null;
            throwable.printStackTrace();
            Toasty.error(this, "Error cropping : "+throwable.getMessage(), Toasty.LENGTH_SHORT,true).show();
        }

    }

}
