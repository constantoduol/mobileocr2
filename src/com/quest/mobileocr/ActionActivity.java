
package com.quest.mobileocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author conny
 */
public class ActionActivity extends Activity {

    private static WebView wv = null;

    private static ActionActivity activity;
    
    private static final String SEARCH_URL = "https://www.google.com/search?q="; //default search url
    
    private static ProgressDialog dialog;
    
    private static String currentAction;
    
    private static String currentText;
    
    private static Database db;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_action);
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
        db = new Database(this);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //when the page finishes loading dismiss the progress window for web search
                if(currentAction.equals("web_search_action")){
                    dismissProgress();
                    //what to do after the page finishes loading
                }
                else if(currentAction.equals("ussd_action")){
                    wv.loadUrl("javascript:app.loadUSSDField('"+Uri.encode(currentText)+"')");
                }
                else if(currentAction.equals("associate_action")){
                    wv.loadUrl("javascript:app.loadRecordField('"+Uri.encode(currentText)+"')");
                    wv.loadUrl("javascript:app.populateExisting('"+Uri.encode(currentText)+"')");
                    //check if we have an existing record
                    
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        activity = this; 
        performRequiredAction();
    }
    
    public ProgressDialog showProgress(String msg){
       dialog = ProgressDialog.show(this, "",msg, true);
       dialog.show();
       return dialog;
    }
    
    public void dismissProgress(){
        if(dialog != null){
            dialog.dismiss();
        }
    }
    
    public void performRequiredAction(){
        Intent intent = getIntent();
        String type = intent.getStringExtra("action_type"); //the default action, this could be AI selected or user selected
        String text = intent.getStringExtra("detected_text");
        currentAction = type;
        currentText = text;
        if(type.equals("web_search_action")){
            dialog = showProgress("Loading. Please wait...");
            wv.loadUrl(SEARCH_URL + text); //perform a search action based on the detected text
        }
        else if(type.equals("ussd_action")){
            //launch a ussd interface
            wv.loadUrl("file:///android_asset/ussd_interface.html");
        }
        else if(type.equals("associate_action")){
            //launch a properties editor interface
            wv.loadUrl("file:///android_asset/props_interface.html");
        }
    }
    
    
    
    public static ActionActivity getInstance() {
        return activity;
    }
    
    public void toast(String msg){
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }
    
    public String getExistingData(String content){
        ArrayList<ArrayList<String>> data = Database.doSelect(
                new String[]{"*"}, 
                new String[]{"OCR_DATA"}, 
                new String[]{"primary_key_value = '"+content.trim()+"'"});
        Log.i("app", data.toString());
        return data.toString();
    }
    
    public void saveRecord(String json){
        try {
            JSONObject obj = new JSONObject(json);
            String category = obj.optString("category");
            String primaryKeyValue = obj.optString("primary_key_value");
            String primaryKeyName = obj.optString("primary_key_name");
            String properties = obj.optString("properties");
            //category, primary_key_name, primary_key_value, properties
            //if category, primaryKeyValue, primaryKeyName already exist in the
            //database we dont insert but do an update
            boolean exists = Database.exists("OCR_DATA",
                    new String[]{"category","primary_key_name","primary_key_value"},
                    new String[]{category,primaryKeyName,primaryKeyValue});
            if(exists){
                Database.update(new String[]{"OCR_DATA"}, 
                            new String[]{"properties = '"+properties+"'"},
                            new String[]{"category = '"+category+"'", 
                                         "primary_key_name = '"+primaryKeyName+"'", 
                                         "primary_key_value = '"+primaryKeyValue+"'"});
            }
            else {
                Database.doInsert("OCR_DATA",
                    new String[]{"category","primary_key_name","primary_key_value","properties"}, 
                    new String[]{category,primaryKeyName,primaryKeyValue,properties});
            
            }
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
