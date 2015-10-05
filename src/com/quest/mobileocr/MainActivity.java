package com.quest.mobileocr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class MainActivity extends Activity{

    private static WebView wv = null;

    private static AbbyyPlugin plugin;

    private static MainActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wv = (WebView) findViewById(R.id.html_viewer);
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabasePath("/data/data/" + wv.getContext().getPackageName() + "/databases/");
        WebChromeClient webChrome = new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.i("JCONSOLE", consoleMessage.lineNumber()
                        + ": " + consoleMessage.message());
                return true;
            }
        };
        wv.addJavascriptInterface(new JavascriptExtensions(), "jse");
        wv.setWebChromeClient(webChrome);
        wv.loadUrl("file:///android_asset/index.html");
        plugin = new AbbyyPlugin(this);
        activity = this;
        

    }


    public static MainActivity getInstance(){
        return activity;
    }

    public AbbyyPlugin getPlugin(){
        return plugin;
    }

    public void startCameraPreview(){
        Intent intent = new Intent(this,PreviewActivity.class);
        startActivity(intent);
    }

//    public void takePhoto(){
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            try {
//                Bundle extras = data.getExtras();
//                Bitmap imageBitmap = (Bitmap) extras.get("data");
//                //File image = new File(plugin.getFileStorage(), "img.jpg");
//                //image.createNewFile();
//                //FileOutputStream out = new FileOutputStream(image);
//                //imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                //String finalPath = "file://"+image.getAbsolutePath();
//                String resp = plugin.recogniseText(imageBitmap);
//                resp = resp.replaceAll("[^\\w\\s]","");
//                wv.loadUrl("javascript:callback('"+resp+"')");
//                delayedCall("javascript:callback('"+resp+"')"); //this was added because the page takes time
//                //wv.loadUrl("javascript:test()");
//                //to load and sometimes the javascript has not loaded
//                Log.i("OCR", resp);
//            }
//            catch(Exception e){
//               e.printStackTrace();
//            }
//        }
//    }









}



