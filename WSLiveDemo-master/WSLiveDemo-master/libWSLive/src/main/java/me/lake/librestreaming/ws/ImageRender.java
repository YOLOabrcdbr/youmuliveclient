package me.lake.librestreaming.ws;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.GLTextureView;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;
//去掉继承Camera.PreviewCallback;
public class ImageRender implements  GLTextureView.Renderer , Camera.PreviewCallback {
    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";
    private static final String TAG = "ImageRender";
    private static final int NO_IMAGE = -1;
    //绘制坐标范围
    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public final Object surfaceChangedWaiter = new Object();

    private int glTextureId = NO_IMAGE;
    private SurfaceTexture surfaceTexture = null;
    //为分配内存坐标
    private final FloatBuffer glCubeBuffer;
    private final FloatBuffer glTextureBuffer;

    private IntBuffer glRgbBuffer;

    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;
    private int addedPadding;

    private final Queue<Runnable> runOnDraw;
    private final Queue<Runnable> runOnDrawEnd;
    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;

    private float backgroundRed = 0;
    private float backgroundGreen = 0;
    private float backgroundBlue = 0;

    public ImageRender() {
        this.vertexShader = NO_FILTER_VERTEX_SHADER;
        this.fragmentShader = NO_FILTER_FRAGMENT_SHADER;

//        this.filter = filter;
        runOnDraw = new LinkedList<>();
        runOnDrawEnd = new LinkedList<>();

        //为坐标分配地址
        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);

        runOnDrawlist = new LinkedList<>();
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
        glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position");
        glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture");
        glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate");
        isInitialized = true;
    }
    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        setRotation(rotation);
    }
    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
        adjustImageScaling();
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        runOnDraw( new Runnable() {

            @Override
            public void run() {
                Bitmap resizedBitmap = null;
                if (bitmap.getWidth() % 2 == 1) {
                    resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas can = new Canvas(resizedBitmap);
                    can.drawARGB(0x00, 0x00, 0x00, 0x00);
                    can.drawBitmap(bitmap, 0, 0, null);
                    addedPadding = 1;
                } else {
                    addedPadding = 0;
                }

                glTextureId = OpenGlUtils.loadTexture(
                        resizedBitmap != null ? resizedBitmap : bitmap, glTextureId, recycle);
                if (resizedBitmap != null) {
                    resizedBitmap.recycle();
                }
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();

                adjustImageScaling();
            }
        });


    }
    private void adjustImageScaling() {
        float outputWidth = this.outputWidth;
        float outputHeight = this.outputHeight;
        if (rotation == Rotation.ROTATION_270 || rotation == Rotation.ROTATION_90) {
            outputWidth = this.outputHeight;
            outputHeight = this.outputWidth;
        }

        float ratio1 = outputWidth / imageWidth;
        float ratio2 = outputHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        if (scaleType == GPUImage.ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        glCubeBuffer.clear();
        glCubeBuffer.put(cube).position(0);
        glTextureBuffer.clear();
        glTextureBuffer.put(textureCords).position(0);
    }
    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        Log.d(TAG,"camera preview");
        final Camera.Size previewSize = camera.getParameters().getPreviewSize();
        onPreviewFrame(data, previewSize.width, previewSize.height);
    }

    public void onPreviewFrame(final byte[] data, final int width, final int height) {
        if (glRgbBuffer == null) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }
        if (runOnDraw.isEmpty()) {
            Log.d(TAG,"runOnDraw is load");
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
                    glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                    if (imageWidth != width) {
                        imageWidth = width;
                        imageHeight = height;
                        adjustImageScaling();
                    }
                }
            });
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        ifNeedInit();
    }
    public int getProgram() {
        return glProgId;
    }
    public void onOutputSizeChanged(final int width, final int height) {
        outputWidth = width;
        outputHeight = height;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        outputWidth = width;
        outputHeight = height;
        GLES20.glViewport(0, 0, width, height);
       GLES20.glUseProgram(getProgram());
        onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized (surfaceChangedWaiter) {
            surfaceChangedWaiter.notifyAll();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG,"on frame");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(runOnDraw);
        onDraw(glTextureId, glCubeBuffer, glTextureBuffer);
        runAll(runOnDrawEnd);
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }
    }
    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }
    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }
    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (runOnDrawEnd) {
            runOnDrawEnd.add(runnable);
        }
    }

    private int glProgId;
    private LinkedList<Runnable> runOnDrawlist;
    private int glAttribPosition;
    private int glUniformTexture;
    private int glAttribTextureCoordinate;
    private boolean isInitialized;
    private  String vertexShader;
    private  String fragmentShader;
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {

            Log.d(TAG,"glProgId is:"+glProgId);
        GLES20.glUseProgram(glProgId);
        runPendingOnDrawTasks();
        if (!isInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(glUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        Log.d(TAG,"draw finish");
    }
    protected void runPendingOnDrawTasks() {
        while (!runOnDrawlist.isEmpty()) {
            runOnDrawlist.removeFirst().run();
        }
    }
    protected void onDrawArraysPre() {
    }
    protected void runOnDrawlist(final Runnable runnable) {
        synchronized (runOnDrawlist) {
            runOnDrawlist.addLast(runnable);
        }
    }
    public void ifNeedInit() {
        if (!isInitialized) initf();
    }

    private void initf() {
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
        glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position");
        glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture");
        glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate");
        isInitialized = true;
    }
}
