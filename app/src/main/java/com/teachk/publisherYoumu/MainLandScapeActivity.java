package com.teachk.publisherYoumu;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.takusemba.rtmppublisher.Publisher;
import com.takusemba.rtmppublisher.PublisherListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;

public class MainLandScapeActivity extends AppCompatActivity implements PublisherListener {


    Publisher publisher;
    Button SubButton,SwitchCamera,SettingButton,CloseButton,FlashLightButton,ScreenShotButton;
    StreamLiveCameraView GlanceView;
    String LiveURL;
    ImageView LiveIcon;
    AlphaAnimation TwinkleLive=new AlphaAnimation(0.1f,1.0f);
    int Width,Height,TextureID;
    boolean IsShow;
    File ShotPic;

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
        SwitchCamera=findViewById(R.id.SwitchCameraButton);
        GlanceView=findViewById(R.id.SurfaceView);
        SettingButton=findViewById(R.id.SettingButton);
        CloseButton=findViewById(R.id.CloseButton);
        FlashLightButton=findViewById(R.id.FlashLightButton);
        ScreenShotButton=findViewById(R.id.ScreenShotButton);
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
                Width=1080;
                Height=1920;
                break;

            case 2:
                Width=720;
                Height=1280;
                break;

            case 4:
                Width=720;
                Height=480;
                break;
        }

/*
        publisher=new Publisher.Builder(this)
                .setGlView(GlanceView)
                .setUrl(LiveURL)
                .setSize(Width, Height)
                .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
                .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
                .setCameraMode(Publisher.Builder.DEFAULT_MODE)
                .setListener(this)
                .build();
*/
        //LiveURL ="rtmp://39.106.194.43:1935/live360/trystream";
        final StreamAVOption streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl =LiveURL;
        streamAVOption.cameraIndex=Camera.CameraInfo.CAMERA_FACING_BACK;
        streamAVOption.previewHeight=1080;
        streamAVOption.previewWidth=1920;
        streamAVOption.videoHeight=1080;
        streamAVOption.videoWidth=1920;
        streamAVOption.videoFramerate=30;
        streamAVOption.videoBitrate=8000000;
        GlanceView.init(this, streamAVOption);

        SettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (IsShow==false){
                    SwitchCamera.setVisibility(View.VISIBLE);
                    FlashLightButton.setVisibility(View.VISIBLE);
                    ScreenShotButton.setVisibility(View.VISIBLE);
                    IsShow=true;
                }
                else{
                    SwitchCamera.setVisibility(View.INVISIBLE);
                    FlashLightButton.setVisibility(View.INVISIBLE);
                    ScreenShotButton.setVisibility(View.INVISIBLE);
                    IsShow=false;
                }
            }
        });

        FlashLightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlanceView.toggleFlashLight();
            }
        });

        SubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GlanceView.isStreaming()){
                    GlanceView.startStreaming(LiveURL);
                    SubButton.setText("结束");
                    SettingButton.setVisibility(View.VISIBLE);
                    CloseButton.setVisibility(View.VISIBLE);
                    SubButton.setVisibility(View.INVISIBLE);
                    LiveIcon.setVisibility(View.VISIBLE);
                    SwitchCamera.setVisibility(View.INVISIBLE);
                    FlashLightButton.setVisibility(View.INVISIBLE);
                    ScreenShotButton.setVisibility(View.INVISIBLE);
                    LiveIcon.setAnimation(TwinkleLive);
                    IsShow=false;
                }
                else GlanceView.stopStreaming();
            }
        });

        CloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlanceView.stopStreaming();
                SubButton.setText("开始");
                SubButton.setVisibility(View.VISIBLE);
                SettingButton.setVisibility(View.INVISIBLE);
                CloseButton.setVisibility(View.INVISIBLE);
                SwitchCamera.setVisibility(View.VISIBLE);
                FlashLightButton.setVisibility(View.VISIBLE);
                ScreenShotButton.setVisibility(View.VISIBLE);
                TwinkleLive.cancel();
                LiveIcon.setVisibility(View.INVISIBLE);
            }
        });

        ScreenShotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlanceView.takeScreenShot(new RESScreenShotListener() {
                    @Override
                    public void onScreenShotResult(Bitmap bitmap) {
                        String Root=Environment.getExternalStorageDirectory().getAbsolutePath();
                        String DirName="Youmu";
                        File Dir=new File(Root,DirName);
                        if (!Dir.exists()){
                            Dir.mkdirs();
                        }
                        long TStamp=System.currentTimeMillis();
                        SimpleDateFormat Form=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String Former=Form.format(new Date(TStamp));
                        String PicName=Former+".jpg";
                        ShotPic=new File(Dir,PicName);
                        try{
                            FileOutputStream OStream=new FileOutputStream(ShotPic);
                            bitmap.compress(Bitmap.CompressFormat.JPEG,100,OStream);
                            OStream.flush();
                            MainLandScapeActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(ShotPic.getPath()))));
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                        finally {
                            Toast.makeText(MainLandScapeActivity.this,"截图成功！",Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        SwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlanceView.swapCamera();
                //textView.setText(Width+" "+Height);
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
