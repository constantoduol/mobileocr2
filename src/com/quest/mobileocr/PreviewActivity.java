package com.quest.mobileocr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.util.Timer;
import java.util.TimerTask;


public class PreviewActivity extends Activity {

    private Camera mCamera;

    private CameraPreview mPreview;


    private FrameLayout preview;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Accelerometer meter;

    private final int CAMERA_FREQUENCY = 300;

    private final int FUTURE_PICTURE_DELAY = 3000;

    private WebView infoView;
    
    private FrameLayout rootView;
    
    private ViewGroup.LayoutParams initialInfoViewLayout;
    
    private static PreviewActivity previewActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        setCameraDisplayOrientation(this,Camera.CameraInfo.CAMERA_FACING_BACK,mCamera);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        rootView =  (FrameLayout) findViewById(R.id.root_view);

        initWebView();

        preview.addView(mPreview);
        takeFuturePicture();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        meter = new Accelerometer();
        mSensorManager.registerListener(meter, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//prevent screen from dimming
        
        previewActivity = this;
        
    }


    private void initWebView(){
        infoView = (WebView) findViewById(R.id.info_view);
        initialInfoViewLayout = infoView.getLayoutParams();
        infoView.setBackgroundColor(0x00000000);
        WebSettings settings = infoView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabasePath("/data/data/" + infoView.getContext().getPackageName() + "/databases/");
        WebChromeClient webChrome = new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.i("JCONSOLE", consoleMessage.lineNumber()
                        + ": " + consoleMessage.message());
                return true;
            }
        };
        infoView.addJavascriptInterface(new JavascriptExtensions(), "jse");
        infoView.setWebChromeClient(webChrome);
        infoView.loadUrl("file:///android_asset/template.html");
    }


    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }



    private void takeFuturePicture() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                //dont take pictures when we are moving
                //take pictures only when we are still
                if (mPreview.getSafeToTakePicture() && meter.isStill()) {
                    mCamera.takePicture(null, null, mPicture);
                    mPreview.setSafeToTakePicture(false);
                }
            }
        };
        timer.schedule(task, FUTURE_PICTURE_DELAY, CAMERA_FREQUENCY);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, final Camera camera) {
            Thread th = new Thread(){
                @Override
                public void run(){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    String resp = MainActivity.getInstance().getPlugin().recogniseText(bitmap);
                    Log.i("apppp",resp);
                    extractMeaning(resp);
                    //put the text in the webview
                    camera.startPreview(); //start preview to take the next picture
                }
            };
            infoView.loadUrl("javascript:app.startLoad()");
            th.start();
        }

    };





    /**
     *  detection strategies
     *    0. remove any garbage characters, any extra characters that are space separated qualify as garbage
     *       a number or letter cannot contain more than one special character e.g a number may be
     *    1. check for the existence of letters or digits or both
     *    2. if t
     */

    private void extractMeaning(String text){
       // final String cleanText = text.replaceAll("[^\\w\\s]","");
        final String cleanText = text;
        Log.i("apppp", cleanText);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoView.loadUrl("javascript:app.stopLoad()");
                String encoded = Uri.encode(cleanText);
                //we need to increase the size of infoview to show what we found
                infoView.setLayoutParams(rootView.getLayoutParams());
                infoView.loadUrl("javascript:app.loadContent('"+encoded+"')");//display the text
                
                infoView.loadUrl("javascript:app.loadUserAgree()");
            }
        });

    }
    
    public void setSafeTotakePicture(final String safe){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isSafe = Boolean.parseBoolean(safe);
                infoView.loadUrl("javascript:app.loadImages('')");//clear the images
                infoView.loadUrl("javascript:app.loadContent('')");//clear the text
                infoView.setLayoutParams(initialInfoViewLayout);
                mPreview.setSafeToTakePicture(isSafe);
            }
        });
    }
    
    public static PreviewActivity getInstance() {
        return previewActivity;
    }



}



















