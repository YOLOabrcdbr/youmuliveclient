package com.teachk.publisherYoumu;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
//import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.takusemba.rtmppublisher.Publisher;
import com.takusemba.rtmppublisher.PublisherListener;

//import com.teachk.publisherYoumu.tflitecamerademo4.ImageSegmentorFloatMobileUnet;

import org.opencv.android.OpenCVLoader;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageToneCurveFilter;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.tf.ImageSegmentor;
import me.lake.librestreaming.tf.ImageSegmentorFloatMobileUnet;
import me.lake.librestreaming.ws.AspectTextureView;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;

public class MainLandScapeActivity extends AppCompatActivity implements PublisherListener {

    Publisher publisher;
    Button SubButton,SwitchCamera,SettingButton,CloseButton,FlashLightButton,ScreenShotButton,testBtn;
    StreamLiveCameraView GlanceView;
    String LiveURL;
    ImageView LiveIcon;
    AlphaAnimation TwinkleLive=new AlphaAnimation(0.1f,1.0f);
    int Width,Height,TextureID;
    boolean IsShow;
    File ShotPic;



    //背景分割

    int flag = 1;
    boolean isFilter = false;
    /** Tag for the {@link Log}. */
    private static final String TAG = "TfLiteCameraDemo";

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String HANDLE_THREAD_NAME = "CameraBackground";

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private final Object lock = new Object();
    private boolean runsegmentor = false;
    private boolean checkedPermissions = false;
    private TextView textView;
    //  private NumberPicker np;
    public ImageSegmentor segmentor;
//       private ListView deviceView;
    private ListView filterView;
//    private LinearLayout deviceLayout;
//    private HorizontalListView BGView;

    public SeekBar seekBar;

    public GPUImageToneCurveFilter curve_filter;
    InputStream is=null;
    public Bitmap bgd, bgd1, bgd2, bgd3 = null;
    public HashMap<Integer, Bitmap> extra_filters = new HashMap<>();
    public Boolean init=false;
    public int filter_idx=0;
    public static int mskthresh=50;
    public static Net net;
//    public static int tvwidth, tvheight;
    public static int record_flag = -1;
    public static int frame_count = 0;
    public static boolean dw_flag = false;

    //Renderscript
//    private static ScriptC_saturation saturation;
    private static  android.support.v8.renderscript.RenderScript rs;

    /** Max preview width that is guaranteed by Camera2 API */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /** Max preview height that is guaranteed by Camera2 API */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    //用于存储用于转化成视频的所有bitmap
    private ArrayList<Bitmap> mapList = new ArrayList<Bitmap>();




    // Model parameter constants.
    private String gpu;
    private String cpu;
    private String nnApi;
    private String videoBokeh;
    private String portraitSeg;
    private String colorTrans;
    private String renderMerge;

    private String new1;
    private String new2;
    private String new3;
    private String new4;
    private String new5;
    private DownloadManager downloader;
    private Uri uri;
    private long reference;




    /** An {AutoFitTextureView} for camera preview. */
    //AutoFitTextureView修改为AspectTextureView
    public static AspectTextureView textureView;


    private ArrayList<String> deviceStrings = new ArrayList<String>();
    private ArrayList<String> filterStrings = new ArrayList<String>();

    private ArrayList<Button> buttonList = new ArrayList<Button>();

    /** Current indices of device and model. */
    int currentDevice = -1;

    int currentFilter = -1;

    int currentNumThreads = -1;

    int mode = 1;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;
    private boolean istrans = false;



    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }






    public static Bitmap renderSmooth(Bitmap bgd, Bitmap fgd, Bitmap msk) {


        Bitmap output = Bitmap.createBitmap(bgd.getWidth(), bgd.getHeight(), bgd.getConfig());

        android.support.v8.renderscript.Allocation bgdAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs,bgd);
        android.support.v8.renderscript.Allocation fgdAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs,fgd);
        android.support.v8.renderscript.Allocation mskAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs,msk);
        android.support.v8.renderscript.Allocation outputAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs,output);

//        saturation.set_fgd_alloc(fgdAllocation);
//        saturation.set_mask_alloc(mskAllocation);
//        saturation.forEach_saturation(bgdAllocation,outputAllocation);
        outputAllocation.copyTo(output);
        return output;
    }

    private void changeFilter(int my_mode, int bg_index){

        mode = my_mode;
        if(bg_index == 0){
            bgd = bgd1;
            mode = 0;
        }
        else if(bg_index == 1){
            bgd = bgd1;
        }
        else if(bg_index == 2){
            bgd = bgd2;
        }
        else if(bg_index == 3){
            bgd = bgd3;
        }
        else if(bg_index >= 4){
            bgd = extra_filters.get(bg_index);
        }
        GlanceView.setBgd(bgd);
    }
   //背景分割end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getRequestedOrientation();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main_land_scape);
        IsShow=false;
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCv", "Unable to load OpenCV");
        else
            Log.d("OpenCv", "OpenCV loaded");

        GlanceView=findViewById(R.id.SurfaceView);
        //背景分割start

        final StreamAVOption streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl =LiveURL;
        streamAVOption.cameraIndex=Camera.CameraInfo.CAMERA_FACING_BACK;
        streamAVOption.previewHeight=1080;
        streamAVOption.previewWidth=1920;
        streamAVOption.videoHeight=1080;
        streamAVOption.videoWidth=1920;
        streamAVOption.videoFramerate=30;
        streamAVOption.videoBitrate=8000000;
        try {
            segmentor = new ImageSegmentorFloatMobileUnet(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        GlanceView.init(this, streamAVOption);
//        GlanceView.init(BitmapFactory.decodeResource(getResources(),R.drawable.dock_vbig, options),segmentor,this, streamAVOption);
//        textureView = GlanceView.getTexture();

        testBtn = findViewById(R.id.BgButton);
        gpu = getString(R.string.gpu);
        cpu = getString(R.string.cpu);
        nnApi = getString(R.string.nnapi);
        videoBokeh = getString(R.string.videoBokeh);
        portraitSeg = getString(R.string.portraitSeg);
        colorTrans = getString(R.string.colorTrans);
        renderMerge = getString(R.string.renderMerge);
        new1 = "new1";
        new2 = "new2";
        new3 = "new3";
        new4 = "new4";
        new5 = "new5";



        ImageButton btn3 = (ImageButton) findViewById((R.id.img0));
        ImageButton btn4 = (ImageButton) findViewById((R.id.img1));
        ImageButton btn5 = (ImageButton) findViewById((R.id.img2));
        ImageButton btn6 = (ImageButton) findViewById((R.id.img3));
        filterView = (ListView)findViewById(R.id.model2);
        int defaultfilterIndex = 1;

        // 第一个元素
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = 0;
                changeFilter(0, currentFilter);
            }
        });
        // 第二个元素
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = 1;
                changeFilter(3, currentFilter);
            }
        });
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = 2;
                changeFilter(3, currentFilter);
            }
        });
        // 第三个元素
        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = 3;
                changeFilter(3, currentFilter);
            }
        });

        AssetManager as = this.getAssets();
        //is = null;
        curve_filter = new GPUImageToneCurveFilter();
        try {
            is = as.open("green.acv");
            curve_filter.setFromCurveFileInputStream(is);
            is.close();
            Log.e("MainActivity", "Success ACV Loaded");
        } catch (IOException e) {
            Log.e("MainActivity", "Error");
        }
        // Seek bar to control mask threshold

        seekBar=(SeekBar)findViewById(50);

        // Load background images
        bgd1= BitmapFactory.decodeResource(getResources(),R.drawable.dock_vbig, options);
        bgd2= BitmapFactory.decodeResource(getResources(),R.drawable.sunset_vbig, options);
        bgd3= BitmapFactory.decodeResource(getResources(),R.drawable.bupt1_vbig, options);
        bgd=bgd1;
//已经被取消的filter 和 device
        // Build list of models
        filterStrings.add(videoBokeh);
        filterStrings.add(portraitSeg);
        filterStrings.add(colorTrans);
        filterStrings.add(renderMerge);
//
//        // Build list of devices
//        int defaultfilterIndex = 1;
//        deviceStrings.add(cpu);
//        deviceStrings.add(gpu);
//        deviceStrings.add(nnApi);
//        deviceView.setAdapter(
//                new ArrayAdapter<String>(
//                        this, R.layout.listview_row, R.id.listview_row_text, deviceStrings));
//        deviceView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//        deviceView.setOnItemClickListener(
//                new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        updateActiveModel();
//                    }
//                });
//        deviceView.setItemChecked(0, true);
        filterView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ArrayAdapter<String> modelAdapter =
                new ArrayAdapter<>(
                        GlanceView.getContext(), R.layout.listview_row, R.id.listview_row_text, filterStrings);
        filterView.setAdapter(modelAdapter);
        filterView.setItemChecked(defaultfilterIndex, true);
        filterView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // 选择模型和替换的背景
                        int filterIndex = filterView.getCheckedItemPosition();
                        changeFilter(3,filterIndex);
                    }
                });
        // Load a caffe network.
        String proto = getPath("deploy_512.prototxt", GlanceView.getContext());
        String weights = getPath("harmonize_iter_200000.caffemodel", GlanceView.getContext());

        net = Dnn.readNetFromCaffe(proto);
        net.setPreferableTarget(Dnn.DNN_TARGET_OPENCL_FP16);
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        net.enableFusion(Boolean.TRUE);
//        Log.i(TAG, "Network loaded successfully");

//        tvheight=textureView.getHeight();
//        tvwidth=textureView.getWidth();

        //背景分割end

        LiveIcon=findViewById(R.id.imageView);
        SubButton=findViewById(R.id.StartButton);
        SwitchCamera=findViewById(R.id.SwitchCameraButton);
//        GlanceView=findViewById(R.id.SurfaceView);
        SettingButton=findViewById(R.id.SettingButton);
        CloseButton=findViewById(R.id.CloseButton);
        FlashLightButton=findViewById(R.id.FlashLightButton);
        ScreenShotButton=findViewById(R.id.ScreenShotButton);
        //LiveURL="rtmp://39.106.194.43:1935/live360/trystream";
//        LiveURL="rtmp://39.106.194.43:1935/live360/trystream";
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
        LiveURL ="rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11777756_9554883&key=facfb96d86434ef87543f8f22f5aaa25";


        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!istrans)
                {
                    GlanceView.setBgd(bgd);
                GlanceView.setSegmentor(segmentor);
                istrans = true;
                }
                else{
                    Log.d("istrans","set null");
                    GlanceView.setBgd(null);
                    GlanceView.setSegmentor(null);
                    istrans = false;
                }
            }
        });

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


        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

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
