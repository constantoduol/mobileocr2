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
    public void startActionActivity(String actionType, String detectedText,String category) {
        if(PreviewActivity.getInstance() == null){
            MainActivity.getInstance().startActionActivity(actionType, detectedText,category); 
        }
        else {
           PreviewActivity.getInstance().startActionActivity(actionType, detectedText,category); 
        }
    }
    
    @JavascriptInterface
    public void saveRecord(String json){
       ActionActivity.getInstance().saveRecord(json);
    }
    
    @JavascriptInterface
    public void saveRecordModel(String category, String primaryKeyValue, String action) {
        ActionActivity.getInstance().saveRecordModel(category,primaryKeyValue,action);
    }
    
    @JavascriptInterface
    public String getItem(String itemName){
        return Database.get(itemName);
    }
    
    @JavascriptInterface
    public void setItem(String itemName,String itemValue){
        Database.put(itemName, itemValue);
    }
    
    @JavascriptInterface
    public void removeItem(String itemName){
        Database.remove(itemName);
    }
    
    @JavascriptInterface
    public void startLoad(String msg) {
        ActionActivity.getInstance().showProgress(msg);
    }
    
    @JavascriptInterface
    public void stopLoad() {
        ActionActivity.getInstance().dismissProgress();
    }
    
    @JavascriptInterface
    public void toast(String msg) {
        ActionActivity.getInstance().toast(msg);
    }
    
    @JavascriptInterface
    public String getExistingData(String detectedText){
        return ActionActivity.getInstance().getExistingData(detectedText);
    }
    
    @JavascriptInterface
    public String getRecordModel(String detectedText) {
        return ActionActivity.getInstance().getRecordModel(detectedText);
    }
    
    @JavascriptInterface
    public String getCategoryFromModels(String detectedText) {
        return PreviewActivity.getInstance().getCategoryFromModels(detectedText);
    }
    
    @JavascriptInterface
    public void performUSSD(String prefix, String postfix) {
        ActionActivity.getInstance().performUSSD(prefix, postfix);
    }
    
    @JavascriptInterface
    public String getCategoryProperties(String category) {
        return ActionActivity.getInstance().getCategoryProperties(category);
    }
    
    @JavascriptInterface
    public String search(String json) {
        return ActionActivity.getInstance().search(json);
    }
}
