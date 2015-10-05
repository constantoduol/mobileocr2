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

}
