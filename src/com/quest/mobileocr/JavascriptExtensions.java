package com.quest.mobileocr;

import android.webkit.JavascriptInterface;


/**
 * Created by Connie on 07-Dec-14.
 */
public class JavascriptExtensions {
    @JavascriptInterface
    public void detect(){
        MainActivity.getInstance().startCameraPreview();
    }
    
    @JavascriptInterface
    public void setSafeToTakePicture(String safe){
        PreviewActivity.getInstance().setSafeTotakePicture(safe);
    }
    
    @JavascriptInterface
    public void resetScreen() {
        PreviewActivity.getInstance().resetScreen();
    }
    
    @JavascriptInterface
    public void cancelOCR() {
        PreviewActivity.getInstance().cancelOCR();
    }
    
    @JavascriptInterface
    public void startActionActivity(String actionType, String detectedText) {
        PreviewActivity.getInstance().startActionActivity(actionType, detectedText);
    }
    
    @JavascriptInterface
    public void saveRecord(String json){
       ActionActivity.getInstance().saveRecord(json);
    }


}
