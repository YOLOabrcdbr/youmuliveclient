package com.teachk.publisher;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.WatermarkFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

public class Practise extends AppCompatActivity {

    StreamLiveCameraView mLiveCameraView;
    StreamLiveCameraView liveCameraView;
    Button button=null;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practise);

        mLiveCameraView=findViewById(R.id.stream_previewView);
        button=findViewById(R.id.StartBtn);
        url="rtmp://39.106.194.43:1935/live360/trystream";
        final StreamAVOption streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl =url;
        streamAVOption.cameraIndex=Camera.CameraInfo.CAMERA_FACING_BACK;
        streamAVOption.previewHeight=1080;
        streamAVOption.previewWidth=1920;
        streamAVOption.videoHeight=1080;
        streamAVOption.videoWidth=1920;
        mLiveCameraView.init(this, streamAVOption);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mLiveCameraView.isStreaming()){
                    mLiveCameraView.startStreaming(url);
                }
            }
        });
    }


}
