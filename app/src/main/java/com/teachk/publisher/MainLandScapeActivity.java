package com.teachk.publisher;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;

import com.takusemba.rtmppublisher.Publisher;
import com.takusemba.rtmppublisher.PublisherListener;

public class MainLandScapeActivity extends AppCompatActivity implements PublisherListener {


    GLSurfaceView GlanceView;
    Publisher publisher;
    Button SubButton,SwitchCamera,SettingButton,CloseButton;
    String LiveURL;
    ImageView LiveIcon;
    AlphaAnimation TwinkleLive=new AlphaAnimation(0.1f,1.0f);
    int Width,Height,TextureID;
    boolean IsShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getRequestedOrientation();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        IsShow=false;
        LiveIcon=findViewById(R.id.imageView);
        SubButton=findViewById(R.id.StartButton);
        GlanceView=findViewById(R.id.surfaceView);
        SwitchCamera=findViewById(R.id.SwitchCameraButton);
        CloseButton=findViewById(R.id.CloseButton);
        SettingButton=findViewById(R.id.SettingButton);
        LiveURL="rtmp://39.106.194.43:1935/live360/trystream";
        TextureID=0;
        Intent intent=getIntent();

        LiveURL=intent.getStringExtra("URL");
        TextureID=intent.getIntExtra("Texture",0);

        LiveIcon.setVisibility(View.INVISIBLE);
        TwinkleLive.setDuration(2000);
        TwinkleLive.setRepeatCount(Animation.INFINITE);
        TwinkleLive.setRepeatMode(Animation.RESTART);

        switch (TextureID){
            case 0:
                Width=1920;
                Height=1080;
                break;

            case 2:
                Width=1280;
                Height=720;
                break;

            case 4:
                Width=720;
                Height=480;
                break;
        }


        publisher=new Publisher.Builder(this)
                .setGlView(GlanceView)
                .setUrl(LiveURL)
                .setSize(Width, Height)
                .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
                .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
                .setCameraMode(Publisher.Builder.DEFAULT_MODE)
                .setListener(this)
                .build();


        CloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publisher.stopPublishing();
            }
        });

        SettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IsShow==false){
                    SwitchCamera.setVisibility(View.VISIBLE);
                    IsShow=true;
                }
                else{
                    SwitchCamera.setVisibility(View.INVISIBLE);
                    IsShow=false;
                }
            }
        });

        SubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!publisher.isPublishing()){
                    publisher.startPublishing();

                }
                else publisher.stopPublishing();
            }
        });

        SwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publisher.switchCamera();
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d("SetPSWActivity", "onFailedToConnect: 5");
    }

    @Override
    public void onResume(){

        super.onResume();
    }
    @Override
    public void onStarted(){
        SubButton.setText("结束");
        SettingButton.setVisibility(View.VISIBLE);
        CloseButton.setVisibility(View.VISIBLE);
        SubButton.setVisibility(View.INVISIBLE);
        LiveIcon.setVisibility(View.VISIBLE);
        SwitchCamera.setVisibility(View.INVISIBLE);
        LiveIcon.setAnimation(TwinkleLive);
        IsShow=false;
    }
    @Override
    public void onStopped(){
        SubButton.setText("开始");
        SubButton.setVisibility(View.VISIBLE);
        SettingButton.setVisibility(View.INVISIBLE);
        CloseButton.setVisibility(View.INVISIBLE);
        SwitchCamera.setVisibility(View.VISIBLE);
        TwinkleLive.cancel();
        LiveIcon.setVisibility(View.INVISIBLE);
    }
    @Override
    public void onDisconnected(){
        Log.d("SetPSWActivity", "onFailedToConnect: 2");
    }
    @Override
    public void onFailedToConnect(){
        Log.d("SetPSWActivity", "onFailedToConnect: 1");
    }

}
