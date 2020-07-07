package me.lake.librestreaming.client;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

import com.teachk.particlesystempublisher.ParticleSystem;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.List;

import me.lake.librestreaming.core.CameraHelper;
import me.lake.librestreaming.core.RESHardVideoCore;
import me.lake.librestreaming.core.RESSoftVideoCore;
import me.lake.librestreaming.core.RESVideoCore;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.encoder.MediaVideoEncoder;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.tf.ImageSegmentor;
import me.lake.librestreaming.tf.ImageSegmentorFloatMobileUnet;
import me.lake.librestreaming.tools.BuffSizeCalculator;
import me.lake.librestreaming.tools.LogTools;
import me.lake.librestreaming.ws.ImageRender;


public class RESVideoClient {
    RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private Camera camera;
    public SurfaceTexture camTexture;
    private int cameraNum;
    private int currentCameraIndex;
    private RESVideoCore videoCore;
    private boolean isStreaming;
    private boolean isPreviewing;
    private WeakReference<Activity> mActivity;



    private ImageSegmentor segmentor;
    Bitmap bgd = null;
    private boolean istrans = false;

    public static int tCopy;

    public RESVideoClient(RESCoreParameters parameters,WeakReference<Activity> activity) {
        resCoreParameters = parameters;
        cameraNum = Camera.getNumberOfCameras();
        currentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        isStreaming = false;
        isPreviewing = false;

            this.mActivity = activity;

    }
    public RESVideoClient(RESCoreParameters parameters) {
        resCoreParameters = parameters;
        cameraNum = Camera.getNumberOfCameras();
        currentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        isStreaming = false;
        isPreviewing = false;

    }

    public boolean prepare(RESConfig resConfig) {
        synchronized (syncOp) {
            if ((cameraNum - 1) >= resConfig.getDefaultCamera()) {
                currentCameraIndex = resConfig.getDefaultCamera();
            }
            if (null == (camera = createCamera(currentCameraIndex))) {
                LogTools.e("can not open camera");
                return false;
            }
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.selectCameraPreviewWH(parameters, resCoreParameters, resConfig.getTargetPreviewSize());
            CameraHelper.selectCameraFpsRange(parameters, resCoreParameters);
            if (resConfig.getVideoFPS() > resCoreParameters.previewMaxFps / 1000) {
                resCoreParameters.videoFPS = resCoreParameters.previewMaxFps / 1000;
            } else {
                resCoreParameters.videoFPS = resConfig.getVideoFPS();
            }
            resoveResolution(resCoreParameters, resConfig.getTargetVideoSize());
            if (!CameraHelper.selectCameraColorFormat(parameters, resCoreParameters)) {
                LogTools.e("CameraHelper.selectCameraColorFormat,Failed");
                resCoreParameters.dump();
                return false;
            }
            if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                LogTools.e("CameraHelper.configCamera,Failed");
                resCoreParameters.dump();
                return false;
            }
            switch (resCoreParameters.filterMode) {
                case RESCoreParameters.FILTER_MODE_SOFT:
                    videoCore = new RESSoftVideoCore(resCoreParameters);
                    break;
                case RESCoreParameters.FILTER_MODE_HARD:
                    videoCore = new RESHardVideoCore(resCoreParameters);
                    break;
            }
            if (!videoCore.prepare(resConfig)) {
                return false;
            }
            videoCore.setCurrentCamera(currentCameraIndex);
            prepareVideo();
            return true;
        }
    }

    private Camera createCamera(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setDisplayOrientation(0);
        } catch (SecurityException e) {
            LogTools.trace("no permission", e);
            return null;
        } catch (Exception e) {
            LogTools.trace("camera.open()failed", e);
            return null;
        }
        return camera;
    }

    private boolean prepareVideo() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
            camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
        }
        return true;
    }

//    private boolean startVideo() {
//        camTexture = new SurfaceTexture(RESVideoCore.OVERWATCH_TEXTURE_ID);
//        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
//            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//                @Override
//                public void onPreviewFrame(byte[] data, Camera camera) {
//                    synchronized (syncOp) {
//                        if (videoCore != null && data != null) {
////                            Bitmap bgd = null;
////                            //Bitmap  bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
////                            ImageSegmentor segmentor;
////                            segmentor = new ImageSegmentorFloatMobileUnet(mActivity);
////                            Bitmap fgd = BitmapFactory.decodeByteArray(data,0,data.length);
////
////                            bgd=Bitmap.createScaledBitmap(
////                                    bgd,resCoreParameters.videoWidth,resCoreParameters.videoHeight, false);
////
////                            segmentor.segmentFrame(bitmap, mode, fgd, bgd);
//
//                            ((RESSoftVideoCore) videoCore).queueVideo(data);
//                        }
//                        camera.addCallbackBuffer(data);
//                    }
//                }
//            });
//        } else {
//            camTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
//                @Override
//                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//
//                    synchronized (syncOp) {
//
//                        if (videoCore != null) {
//                            ((RESHardVideoCore) videoCore).onFrameAvailable();
//                        }
//                    }
//                }
//            });
//        }
//        try {
//
//            camera.setPreviewTexture(camTexture);
//
//        } catch (IOException e) {
//            LogTools.trace(e);
//            camera.release();
//            return false;
//        }
//        camera.startPreview();
//        return true;
//    }

    public boolean startPreview(Bitmap bgd,ImageSegmentor segmentor,SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        synchronized (syncOp) {
            if (!isStreaming && !isPreviewing) {
                this.segmentor = segmentor;
                this.bgd = bgd;
                istrans = true;
                if (!startVideo()) {
                    resCoreParameters.dump();
                    LogTools.e("RESVideoClient,start(),failed");
                    return false;
                }
                videoCore.updateCamTexture(camTexture);
            }
            videoCore.startPreview(surfaceTexture, visualWidth, visualHeight);
            isPreviewing = true;
            return true;
        }
    }
    public boolean startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        synchronized (syncOp) {
            if (!isStreaming && !isPreviewing) {
               istrans = false;
                if (!startVideo()) {
                    resCoreParameters.dump();
                    LogTools.e("RESVideoClient,start(),failed");
                    return false;
                }
                videoCore.updateCamTexture(camTexture);
            }
            videoCore.startPreview(surfaceTexture, visualWidth, visualHeight);
            isPreviewing = true;
            return true;
        }
    }

    public void updatePreview(int visualWidth, int visualHeight) {
        videoCore.updatePreview(visualWidth, visualHeight);
    }

    public void updatePreviewbgd(Bitmap bgd,ImageSegmentor segmentor) {
        if(segmentor!=null)
        { this.bgd = bgd;
        this.segmentor = segmentor;
        istrans = true;
        }
        else
        {
            istrans = false;
        }
    }

    public boolean stopPreview(boolean releaseTexture) {
        synchronized (syncOp) {
            if (isPreviewing) {
                videoCore.stopPreview(releaseTexture);
                if (!isStreaming) {
                    camera.stopPreview();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isPreviewing = false;
            return true;
        }
    }

    public boolean startStreaming(RESFlvDataCollecter flvDataCollecter) {
        synchronized (syncOp) {
            if (!isStreaming && !isPreviewing) {
                if (!startVideo()) {
                    resCoreParameters.dump();
                    LogTools.e("RESVideoClient,start(),failed");
                    return false;
                }
                videoCore.updateCamTexture(camTexture);
            }
            videoCore.startStreaming(flvDataCollecter);
            isStreaming = true;
            return true;
        }
    }

    public boolean stopStreaming() {
        synchronized (syncOp) {
            if (isStreaming) {
                videoCore.stopStreaming();
                if (!isPreviewing) {
                    camera.stopPreview();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isStreaming = false;
            return true;
        }
    }


    public boolean destroy() {
        synchronized (syncOp) {
            camera.release();
            videoCore.destroy();
            videoCore = null;
            camera = null;
            return true;
        }
    }

    public boolean swapCamera() {
        synchronized (syncOp) {
            LogTools.d("RESClient,swapCamera()");
            camera.stopPreview();
            camera.release();
            camera = null;
            if (null == (camera = createCamera(currentCameraIndex = (++currentCameraIndex) % cameraNum))) {
                LogTools.e("can not swap camera");
                return false;
            }
            videoCore.setCurrentCamera(currentCameraIndex);
            CameraHelper.selectCameraFpsRange(camera.getParameters(), resCoreParameters);
            if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                camera.release();
                return false;
            }
            prepareVideo();
            camTexture.release();
            videoCore.updateCamTexture(null);
            startVideo();
            videoCore.updateCamTexture(camTexture);
            return true;
        }
    }

    public boolean toggleFlashLight() {
        synchronized (syncOp) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                List<String> flashModes = parameters.getSupportedFlashModes();
                String flashMode = parameters.getFlashMode();
                if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(parameters);
                        return true;
                    }
                } else if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        return true;
                    }
                }
            } catch (Exception e) {
                LogTools.d("toggleFlashLight,failed" + e.getMessage());
                return false;
            }
            return false;
        }
    }

    public boolean setZoomByPercent(float targetPercent) {
        synchronized (syncOp) {
            targetPercent = Math.min(Math.max(0f, targetPercent), 1f);
            Camera.Parameters p = camera.getParameters();
            p.setZoom((int) (p.getMaxZoom() * targetPercent));
            camera.setParameters(p);
            return true;
        }
    }

    public void reSetVideoBitrate(int bitrate) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.reSetVideoBitrate(bitrate);
            }
        }
    }

    public int getVideoBitrate() {
        synchronized (syncOp) {
            if (videoCore != null) {
                return videoCore.getVideoBitrate();
            } else {
                return 0;
            }
        }
    }

    public void reSetVideoFPS(int fps) {
        synchronized (syncOp) {
            int targetFps;
            if (fps > resCoreParameters.previewMaxFps / 1000) {
                targetFps = resCoreParameters.previewMaxFps / 1000;
            } else {
                targetFps = fps;
            }
            if (videoCore != null) {
                videoCore.reSetVideoFPS(targetFps);
            }
        }
    }

    public boolean reSetVideoSize(Size targetVideoSize) {
        synchronized (syncOp) {
            RESCoreParameters newParameters = new RESCoreParameters();
            newParameters.isPortrait = resCoreParameters.isPortrait;
            newParameters.filterMode = resCoreParameters.filterMode;
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.selectCameraPreviewWH(parameters, newParameters, targetVideoSize);
            resoveResolution(newParameters, targetVideoSize);
            boolean needRestartCamera = (newParameters.previewVideoHeight != resCoreParameters.previewVideoHeight
                    || newParameters.previewVideoWidth != resCoreParameters.previewVideoWidth);
            if (needRestartCamera) {
                newParameters.previewBufferSize = BuffSizeCalculator.calculator(resCoreParameters.previewVideoWidth,
                        resCoreParameters.previewVideoHeight, resCoreParameters.previewColorFormat);
                resCoreParameters.previewVideoWidth = newParameters.previewVideoWidth;
                resCoreParameters.previewVideoHeight = newParameters.previewVideoHeight;
                resCoreParameters.previewBufferSize  = newParameters.previewBufferSize;
                if ((isPreviewing || isStreaming)) {
                    LogTools.d("RESClient,reSetVideoSize.restartCamera");
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    if (null == (camera = createCamera(currentCameraIndex))) {
                        LogTools.e("can not createCamera camera");
                        return false;
                    }
                    if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                        camera.release();
                        return false;
                    }
                    prepareVideo();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                    startVideo();
                    videoCore.updateCamTexture(camTexture);
                }
            }
            videoCore.reSetVideoSize(newParameters);
            return true;
        }
    }

    public BaseSoftVideoFilter acquireSoftVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            return ((RESSoftVideoCore) videoCore).acquireVideoFilter();
        }
        return null;
    }

    public void releaseSoftVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            ((RESSoftVideoCore) videoCore).releaseVideoFilter();
        }
    }

    public void setSoftVideoFilter(BaseSoftVideoFilter baseSoftVideoFilter) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            ((RESSoftVideoCore) videoCore).setVideoFilter(baseSoftVideoFilter);
        }
    }

    public BaseHardVideoFilter acquireHardVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            return ((RESHardVideoCore) videoCore).acquireVideoFilter();
        }
        return null;
    }

    public void releaseHardVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            ((RESHardVideoCore) videoCore).releaseVideoFilter();
        }
    }

    public void setHardVideoFilter(BaseHardVideoFilter baseHardVideoFilter) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            ((RESHardVideoCore) videoCore).setVideoFilter(baseHardVideoFilter);
        }
    }

    public void takeScreenShot(RESScreenShotListener listener) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.takeScreenShot(listener);
            }
        }
    }

    public void setVideoChangeListener(RESVideoChangeListener listener) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.setVideoChangeListener(listener);
            }
        }
    }

    public float getDrawFrameRate() {
        synchronized (syncOp) {
            return videoCore == null ? 0 : videoCore.getDrawFrameRate();
        }
    }

    private void resoveResolution(RESCoreParameters resCoreParameters, Size targetVideoSize) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoHeight = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoWidth = resCoreParameters.previewVideoHeight;
            } else {
                resCoreParameters.videoWidth = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoHeight = resCoreParameters.previewVideoHeight;
            }
        } else {
            float pw, ph, vw, vh;
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoHeight = targetVideoSize.getWidth();
                resCoreParameters.videoWidth = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoHeight;
                ph = resCoreParameters.previewVideoWidth;
            } else {
                resCoreParameters.videoWidth = targetVideoSize.getWidth();
                resCoreParameters.videoHeight = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoWidth;
                ph = resCoreParameters.previewVideoHeight;
            }
            vw = resCoreParameters.videoWidth;
            vh = resCoreParameters.videoHeight;
            float pr = ph / pw, vr = vh / vw;
            if (pr == vr) {
                resCoreParameters.cropRatio = 0.0f;
            } else if (pr > vr) {
                resCoreParameters.cropRatio = (1.0f - vr / pr) / 2.0f;
            } else {
                resCoreParameters.cropRatio = -(1.0f - pr / vr) / 2.0f;
            }
        }
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        videoCore.setVideoEncoder(encoder);
    }

    public void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror) {
        videoCore.setMirror(isEnableMirror,isEnablePreviewMirror,isEnableStreamMirror);
    }
    public void setNeedResetEglContext(boolean bol){
        videoCore.setNeedResetEglContext(bol);
    }

    //添加


    private int k = 0,c = 0;
    private boolean startVideo() {
        camTexture = new SurfaceTexture(RESVideoCore.OVERWATCH_TEXTURE_ID);

        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    synchronized (syncOp) {
                        if (videoCore != null && data != null) {
                            if(istrans) {
                                int pwidth = resCoreParameters.previewVideoWidth;
                                int phight = resCoreParameters.previewVideoHeight;

//                            byte[] kdata = ImageUtils.rotateYUV420Degree180(ImageUtils.rotateYUV420Degree90(data,pwidth, phight),pwidth, phight);
                                YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, pwidth, (int) (phight), null);//20、20分别是图的宽度与高度
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                yuvimage.compressToJpeg(new Rect(0, 0, pwidth, phight), 50, baos);//80--JPG图片的质量[0-100],100最高
                                byte[] jdata = baos.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);


                                int width = bitmap.getWidth();
                                int height = bitmap.getHeight();
                                float scaleWidth = ((float) 128) / width;
                                float scaleHeight = ((float) 128) / height;
                                // CREATE A MATRIX FOR THE MANIPULATION
                                Matrix matrix = new Matrix();
                                // RESIZE THE BIT MAP
                                matrix.postScale(scaleWidth, scaleHeight);

                                // "RECREATE" THE NEW BITMAP
                                Bitmap resizedBitmap = Bitmap.createBitmap(
                                        bitmap, 0, 0, width, height, matrix, false);


//                            Bitmap fgd = bitmap;


                                bgd = Bitmap.createScaledBitmap(
                                        bgd, pwidth, phight, false);
//                          bgd = ImageUtils.adjustPhotoRotation(bgd,90);
                                segmentor.segmentFrame(resizedBitmap, 1, bitmap, bgd);

//                            Mat trans2  = new Mat();
//
//                            Utils.bitmapToMat(segmentor.result,trans2);
//                            Mat result = new Mat();
//                            Imgproc.cvtColor(trans2,result,Imgproc.COLOR_YUV2BGR_I420);
//                            byte[] data_result = new byte[resCoreParameters.videoWidth*resCoreParameters.videoHeight];
//                            result.get(0,0,data_result);
//                            data = data_result;

                                byte[] data_result = ImageUtils.bitmapToNv21(segmentor.result, pwidth, phight);
                                data = data_result;
//                           data_result = ImageUtils.rotateYUV420Degree180(ImageUtils.rotateYUV420Degree90(data_result,pwidth, phight),pwidth, phight);
//                            if(k<15) {
//                                saveBitmap(segmentor.result,Environment.getExternalStorageDirectory().getAbsolutePath()+"/Pictures/ddd"+k+".jpg");
//                                k++;
//                            }
                              ;
                            Mat mat = new Mat((int)(resCoreParameters.videoHeight*1.5),resCoreParameters.videoWidth,CvType.CV_8UC1);
                            mat.put(0,0,data);
                            Mat rgb_i420 = new Mat();
                            Imgproc.cvtColor(mat , rgb_i420, Imgproc.COLOR_YUV2RGB_I420);
                            ParticleSystem.runSystem(rgb_i420, null, tCopy);
                            Mat result = new Mat();
                            Imgproc.cvtColor(rgb_i420, result , Imgproc.COLOR_RGB2YUV_I420);//转换回YUV420p格式
                            byte[] data_result2 = new byte[(int)(resCoreParameters.videoHeight*resCoreParameters.videoWidth*1.5)];
                            result.get(0,0,data_result);

                            data=data_result2;


                            }

                            ((RESSoftVideoCore) videoCore).queueVideo(data);

//                            ((RESSoftVideoCore) videoCore).queueVideo(data);
                        }
                            camera.addCallbackBuffer(data);


                    }
                }
            });

        }
        try {

            camera.setPreviewTexture(camTexture);

        } catch (IOException e) {
            LogTools.trace(e);
            camera.release();
            return false;
        }
        camera.startPreview();
        return true;
    }
    public static void saveBitmap(Bitmap bitmap,String path) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            savePath = path;
        } else {
            Log.e("tag", "saveBitmap failure : sdcard not mounted");
            return;
        }
        try {
            filePic = new File(savePath);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e("tag", "saveBitmap: " + e.getMessage());
            return;
        }
        Log.i("tag", "saveBitmap success: " + filePic.getAbsolutePath());
    }


}
