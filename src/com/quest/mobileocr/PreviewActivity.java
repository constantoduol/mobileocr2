package com.quest.mobileocr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

public class PreviewActivity extends Activity {

    private Camera mCamera;

    private CameraPreview mPreview;

    private FrameLayout preview;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Accelerometer meter;

    private final int CAMERA_FREQUENCY = 100;

    private final int FUTURE_PICTURE_DELAY = 3000;

    private WebView infoView;

    private static PreviewActivity previewActivity;

    private static Database db;

    private static double CHAR_MAP_THRESHOLD = 0.5;

    private static double LENGTH_THRESHOLD = 0.5;
  
    private static ScheduledExecutorService ses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        
        setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        
        db = new Database(this);
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
    
    

    private void initWebView() {
        infoView = (WebView) findViewById(R.id.info_view);
        infoView.setBackgroundColor(0x00000000);;
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

    private int[] getScreenDims() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        return new int[]{width, height};
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        if (camera != null) {
            camera.setDisplayOrientation(result);
        }
    }
    
    
    
    private boolean hasAutoFocus(){
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }
    
    private void takeFuturePictureWithoutAutoFocus(){
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
    
    
    
    private void takeFuturePictureWithAutoFocus(){
        ses = Executors.newScheduledThreadPool(1);
        //final Timer timer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.i("app", "run called");
                final TimerTask tsk = this;
                if (mPreview.getSafeToTakePicture()) {
                    mCamera.autoFocus(new AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                mCamera.takePicture(null, null, mPicture);
                                mPreview.setSafeToTakePicture(false);
                            }
                            ses.schedule(tsk, CAMERA_FREQUENCY, TimeUnit.MILLISECONDS);
                        }
                    });
                }
                else {
                    //just schedule a future run
                    ses.schedule(tsk, CAMERA_FREQUENCY, TimeUnit.MILLISECONDS);
                }
            }
        };
        ses.schedule(task, FUTURE_PICTURE_DELAY, TimeUnit.MILLISECONDS);
        
    }
    

    private void takeFuturePicture() {
        if(hasAutoFocus()){
            takeFuturePictureWithAutoFocus();
        }
        else {
            takeFuturePictureWithoutAutoFocus();
        }
    }
    
    @Override
    public void onResume(){
        super.onResume();
        mPreview.setSafeToTakePicture(false);
        Log.i("app","RESUME safe to take : "+mPreview.getSafeToTakePicture());
        takeFuturePicture();
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        Log.i("app","RESTART safe to take : "+mPreview.getSafeToTakePicture());
        mPreview.setSafeToTakePicture(false);
        takeFuturePicture();
    }
    
    @Override
    public void onPause(){
        super.onPause();
        if(hasAutoFocus())
            ses.shutdownNow();
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
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }
    
  
    


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, final Camera camera) {
            Thread th = new Thread() {
                @Override
                public void run() {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Bitmap rotatedBitmap = rotateBitmap(bitmap);//rotate first then crop
                    Bitmap croppedBitmap = cropBitmap(rotatedBitmap);//crop the bitmap
                    saveImage(croppedBitmap);
                    String resp = MainActivity.getInstance().getPlugin().recogniseText(croppedBitmap);//recognize text in the image
                    Log.i("app", resp);
                    extractMeaning(resp);
                    //put the text in the webview
                    camera.startPreview(); //start preview to take the next picture
                }
            };
            infoView.loadUrl("javascript:app.startLoad()");
            th.start();
        }

    };

    private static String cleanText(String text) {
        char[] arr = text.toCharArray();
        boolean foundLetterOrDigit = false;
        for (int x = 0; x < arr.length; x++) {
            Character ch = (Character) arr[x];
            if (!Character.isDigit(ch) && !Character.isLetter(ch) && !foundLetterOrDigit) {
                arr[x] = ' ';
            } else {
                //replace non letters and non digits
                foundLetterOrDigit = true;
            }
        }

        foundLetterOrDigit = false;
        for (int x = (arr.length - 1); x >= 0; x--) {
            Character ch = (Character) arr[x];
            if (!Character.isDigit(ch) && !Character.isLetter(ch) && !foundLetterOrDigit) {
                arr[x] = ' ';
            } else {
                //replace non letters and non digits
                foundLetterOrDigit = true;
            }
        }
        return new String(arr).trim();
    }

    /**
     * detection strategies 0. remove any garbage characters, any extra
     * characters that are space separated qualify as garbage a number or letter
     * cannot contain more than one special character e.g a number may be 1.
     * check for the existence of letters or digits or both 2. if t
     */
    private void extractMeaning(String text) {
        if (text.contains("ABBYY")) text = "Error";
        
        final String cleanText = cleanText(text);
        //take care of the abbyy bug by cancelling the ocr process
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoView.loadUrl("javascript:app.stopLoad()");
                String encoded = Uri.encode(cleanText);
                //we need to increase the size of infoview to show what we found
                //infoView.setLayoutParams(rootView.getLayoutParams());
                infoView.loadUrl("javascript:app.loadContent('" + encoded + "')");//display the text

                infoView.loadUrl("javascript:app.loadUserAgree()");
            }
        });
    }

    public void cancelOCR() {
        resetScreen();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                setSafeTotakePicture("true");
            }
        };
        timer.schedule(task, FUTURE_PICTURE_DELAY);
        //say its safe to take a picture in future
    }

    public void setSafeTotakePicture(final String safe) {
        boolean isSafe = Boolean.parseBoolean(safe);
        mPreview.setSafeToTakePicture(isSafe);
    }

    public void resetScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoView.loadUrl("javascript:app.loadImages('')");//clear the images
                infoView.loadUrl("javascript:app.loadContent('!#__blanks__#!')");//clear the text
            }
        });
    }

    public static PreviewActivity getInstance() {
        return previewActivity;
    }

    private void saveImage(Bitmap imageBitmap) {
        try {
            File image = new File(MainActivity.getInstance().getPlugin().getFileStorage(), "img.jpg");
            image.createNewFile();
            FileOutputStream out = new FileOutputStream(image);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(PreviewActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //crops the given input bitmap
    private Bitmap cropBitmap(Bitmap imageBitmap) {
        Integer originalWidth = imageBitmap.getWidth();//480
        Integer originalHeight = imageBitmap.getHeight();//640
        int startX = 0;
        int startY = originalHeight * 3 / 7; //divide the screen into 7 parts
        Integer newWidth = originalWidth - 2 * startX;
        Integer newHeight = originalHeight - 2 * startY;
        return Bitmap.createBitmap(imageBitmap, startX, startY, newWidth, newHeight);
    }

    //rotates through 90 degrees
    private Bitmap rotateBitmap(Bitmap original) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, original.getWidth(), original.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    public void startActionActivity(String actionType, String detectedText, String category) {
        Intent intent = new Intent(this, ActionActivity.class);
        intent.putExtra("action_type", actionType);
        intent.putExtra("detected_text", detectedText);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    public void toast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    //this function is our gamma in the naive bayes model
    //where gamma(input) = class
    public String getCategoryFromModels(String inputData) {
        Log.i("app", "category models : " + inputData);
        //we make sense of the model by providing input text
        //based on the pattern data we have we try to find the 
        //closest match to our input string
        //get length of the string, charmap and most frequent class
        Integer newLength = inputData.length();
        String newCharMap = ActionActivity.characterMap(inputData);
        //String newAdMap = alphabetMap(inputData);
        //use the maps to get the relevant model
        //we will fetch existing maps from the database and compare
        //them with the maps for this input string. we employ the
        //naive bayes form of supervised machine learning to classify
        //the input string
        ArrayList<ArrayList<String>> data = Database.doSelect(
                new String[]{"*"},
                new String[]{"PATTERN_DATA"},
                new String[]{});
        //"category", "char_map",
        HashMap<String, Double> catAveLength = new HashMap();
        HashMap<String, Integer> catCount = new HashMap();
        HashMap<String, Integer> catLengthTotal = new HashMap();
        HashMap<String, String> actions = new HashMap();
        //remember for the pattern detection we employ two parameters
        //as voters, therefore we have 3 votes cast in deciding a pattern string category
        //the first vote is cast based on the length of the input string
        //the second vote is cast based on the charmap of the input string
        //the third vote is cast based on the most frequent category
        //if the three different voters select different categories then the system
        //is undecided
        String charMapSelectedCategory = "";
        String lengthSelectedCategory = "";
        String freqSelectedCategory = "";
        String charMapSelectedAction = "";
        String lengthSelectedAction = "";
        String freqSelectedAction = "";
        Double maxCharMapScore = 0.0;
        Integer totalCount = 0;
        for (ArrayList<String> list : data) { //iterate through the category data
            String category = list.get(0);
            String char_map = list.get(1);
            String action = list.get(2); //this is the action to take based on the detected text
            totalCount++;
            double charMapScore = compareCharMaps(newCharMap, char_map);
            if (charMapScore > maxCharMapScore) {
                maxCharMapScore = charMapScore;
                charMapSelectedCategory = category;
                charMapSelectedAction = action;
            }
            Integer clength = char_map.length();
            Integer currentCatCount = catCount.get(category);
            Integer currentCatTotal = catLengthTotal.get(category);
            if (currentCatCount == null) {
                catCount.put(category, 1); //the first time we encounter this category
                catAveLength.put(category, clength.doubleValue());
                catLengthTotal.put(category, clength);
                actions.put(category, action);
            } else {
                currentCatCount++;
                currentCatTotal += clength;
                catCount.put(category, currentCatCount); //subsequent times we encounter this just increase the count
                //so as we progress we will find how many times this category exists
                Double currentAveLength = currentCatTotal.doubleValue() / currentCatCount.doubleValue();
                catAveLength.put(category, currentAveLength);
                catLengthTotal.put(category, currentCatTotal);
            }
        }

        //to interpret the model we get the average length of each category
        //once we get the average length of each category, we divide the length of our input string
        //with the average from each category, the category that gives an answer closest one is picked
        Iterator<String> iter = catAveLength.keySet().iterator();
        Double maxLengthScore = 0.0;
        double maxCatFreq = 0;
        while (iter.hasNext()) {
            String category = iter.next();
            String action = actions.get(category);
            Double average = catAveLength.get(category);
            Integer catFreq = catCount.get(category);
            //we divide the smaller by the greater
            double prob = newLength.doubleValue() > average ? average / newLength.doubleValue() : newLength.doubleValue() / average;
            //this is the probability that this string belongs to the specific category
            if (prob > maxLengthScore) {
                maxLengthScore = prob;
                lengthSelectedCategory = category;
                lengthSelectedAction = action;
            }

            if (catFreq > maxCatFreq) {
                maxCatFreq = catFreq;
                freqSelectedCategory = category;
                freqSelectedAction = action;
            }
        }
        Double freqConf = totalCount == 0 ? 0.0 : maxCatFreq / totalCount.doubleValue();
        Log.i("char_map_confidence", maxCharMapScore.toString());
        Log.i("length_confidence", maxLengthScore.toString());
        Log.i("freq_confidence", freqConf.toString());
        toast(round(maxCharMapScore) + " : " + round(maxLengthScore) + " : " + round(freqConf));

        //we now get the category assigned
        //if any of the strings are blank it means it is unassigned
        //so we return a blank string
        //our selection of category is also based on confidence
        //the more confident we are about the data selected the more likely the selected category
        //for the input data
        JSONObject obj = new JSONObject();
        try {
            if (maxCharMapScore > CHAR_MAP_THRESHOLD && maxLengthScore > LENGTH_THRESHOLD) {
			//we use the frequency confidence to resolve a case where the category selected
                //by the char map and category selected by the length are different
                if (charMapSelectedCategory.equals(lengthSelectedCategory)) {
                    obj.put("category", charMapSelectedCategory);
                    obj.put("action", charMapSelectedAction);
                } else if (charMapSelectedCategory.equals(freqSelectedCategory)) {
                    obj.put("category", charMapSelectedCategory);
                    obj.put("action", charMapSelectedAction);
                } else {
                    obj.put("category", "");
                    obj.put("action", "");
                    //the three categories are different hence we are undecided
                }
            } else {
                //we did not meet the threshold so that means we are undecided
                obj.put("category", "");
                obj.put("action", "");
            }
            return obj.toString();
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private double round(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    private double compareCharMaps(String charMap1, String charMap2) {
        //here we compare two char maps for statistical closeness
        //charmap for e.g. KAG 564M is lllsdddl
        //equivalence of char maps is a matter of order
        //equal char maps have same char by char value
        //the aim of this method is to give a probability between 0 and 1
        //of the likelihood of charMap1 belonging to the same word as charmap 2
        if (charMap1.equals(charMap2)) {
            return 1.0;//this is a hundred percent likelihood
        }        //if the two strings are unequal we iterate on the shorter string
        Integer shorter = charMap1.length() < charMap2.length() ? charMap1.length() : charMap2.length();
        Integer longer = charMap1.length() > charMap2.length() ? charMap1.length() : charMap2.length();
        //now we go character by character inspecting the char maps
        //we disregard the remaining part of the longer string
        char[] arr1 = charMap1.toCharArray();
        char[] arr2 = charMap2.toCharArray();
        Integer equalityCount = 0;
        //the aim of this method is to test the integrity of the order
        //of a detected pattern string
        for (int x = 0; x < shorter; x++) {
            char ch1 = arr1[x];
            char ch2 = arr2[x];
            if (ch1 == ch2) {
                equalityCount++;
            }
        }
        return equalityCount.doubleValue() / longer.doubleValue();
    }

}
