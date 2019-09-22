package com.nsromapa.frenzapp.newfy.ui.views;


import android.animation.ObjectAnimator;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.nsromapa.frenzapp.R;
import com.ohoussein.playpause.PlayPauseView;

import java.io.IOException;


public class FrenzAppVideoView  extends AppCompatActivity {
    TextureView textureView;
    static MediaPlayer mediaPlayer;
    ArcSeekBar seekBar;
    PlayPauseView playPauseView;
    boolean seekbarTracking=false;
    LinearLayout seekbarLL;
    boolean showSeekbarLL=false;
    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(getSupportActionBar()!=null) {
            getSupportActionBar().hide();
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frenzapp_videoview);

        handler = new Handler();
        textureView = findViewById(R.id.textureView);
        mediaPlayer = new MediaPlayer();
        seekBar = findViewById(R.id.seekArc);
        playPauseView = findViewById(R.id.play_pause_view);
        seekbarLL = findViewById(R.id.seekbarLL);

        showSeekBarLL();

        textureView.setOnClickListener(view -> showSeekBarLL());


        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                prepareVideo(surfaceTexture);
                adjustAspectRatio(textureView,mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight());

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                adjustAspectRatio(textureView,mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight());
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                adjustAspectRatio(textureView,mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight());
            }
        });
        seekBar.setOnStartTrackingTouch(i -> {
            setProgressWithAnimation(i);
            seekbarTracking=true;
            showSeekbarLL=true;

        });


        seekBar.setOnStopTrackingTouch(i -> {
            setProgressWithAnimation(i);
            seekbarTracking=false;
            showSeekbarLL=false;


        });

        seekBar.setOnProgressChangedListener(i -> {
            if(seekbarTracking){
                mediaPlayer.seekTo(i);
            }
        });
        if(playPauseView.isPlay()) {
            playPauseView.toggle(true);
        }

        playPauseView.setOnClickListener(view -> {
            if(playPauseView.isPlay()){
                showSeekbarLL=false;
                showSeekBarLL();
                playPauseView.toggle(true);
                mediaPlayer.start();
            }
            else{
                playPauseView.toggle(true);
                showSeekbarLL=true;
                seekbarLL.setVisibility(View.VISIBLE);
                mediaPlayer.pause();
            }
        });

        final Handler handler = new Handler();
        FrenzAppVideoView.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer!=null) {
                    if(!seekbarTracking) {
                        setProgressWithAnimation(mediaPlayer.getCurrentPosition());
                    }
                }
                handler.postDelayed(this,1000);
            }
        });



    }




    public void prepareVideo(SurfaceTexture t)
    {

        mediaPlayer.setSurface(new Surface(t));

        try {
            mediaPlayer.setDataSource(getIntent().getStringExtra("videoURI"));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mediaPlayer.start();
                seekBar.setMaxProgress(mediaPlayer.getDuration());
            });

        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e1) {
            e1.printStackTrace();
        }

    }

    private void adjustAspectRatio(TextureView m_TextureView,int videoWidth, int videoHeight) {
        int viewWidth = m_TextureView.getWidth();
        int viewHeight = m_TextureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v("Video Player", "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        m_TextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        m_TextureView.setTransform(txform);
    }

    @Override
    public void onBackPressed() {
        //To support reverse transitions when user clicks the device back button

        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(volume_level-(volume_level/2),volume_level-(volume_level/2));
        supportFinishAfterTransition();

    }

    @Override
    protected void onStop() {
        super.onStop();

        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(volume_level-(volume_level/2),volume_level-(volume_level/2));
        mediaPlayer.reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(volume_level-(volume_level/2),volume_level-(volume_level/2));;

        mediaPlayer.reset();
    }

    public void showSeekBarLL(){

        if(showSeekbarLL) {
            seekbarLL.setVisibility(View.VISIBLE);
            showSeekbarLL=false;
        }
        else {
            seekbarLL.setVisibility(View.GONE);
            showSeekbarLL=true;
        }


    }

    public void setProgressWithAnimation(int progress){
        ObjectAnimator objectAnimator;
        objectAnimator = ObjectAnimator.ofInt(seekBar, "progress", progress);
        objectAnimator.start();

    }
}
