
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
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
    
    public ProgressDialog showProgress(){
       dialog = ProgressDialog.show(this, "","Loading. Please wait...", true);
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
            dialog = showProgress();
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
    
    public void saveRecord(String json){
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.optString("type");
            JSONArray names = obj.optJSONArray("prop_names");
            JSONArray values = obj.optJSONArray("prop_values");
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void resolveColumnChanges(Database db, String table, JSONObject currentColData, String[] expectedColData) {
        //alter the table to change the data type of the column
        //alter the table to insert the extra columns
        List currentColNames = listToUpperCase(currentColData.optJSONArray("Field").toList());
        List currentDataTypes = listToUpperCase(currentColData.optJSONArray("Type").toList());
        ArrayList<String> alterRegister = new ArrayList();//keeps track of columns that have been altered
        for (int x = 0; x < expectedColData.length; x++) {
            //separate the column and type
            String colAndType = expectedColData[x];
            String[] vals = colAndType.replaceAll("\\s+", " ").split(" ");
            String expectColName = vals[0].toUpperCase();
            String expectType = vals[1].toUpperCase();
            //we check whether this value exists in current data
            int currentIndex = currentColNames.indexOf(expectColName);
            //if it exists, we just need to verify that the data type is the same
            if (currentIndex > -1) {
                //this column currently exists so verify data type
                String currentType = currentDataTypes.get(currentIndex).toString();
                String currentCol = currentColNames.get(currentIndex).toString();
                if (!currentType.equals(expectType) && !currentType.contains(expectType)) {
                    //this means that the datatype for this column has changed so change it
                    db.execute("ALTER TABLE " + table + " MODIFY " + currentCol + " " + expectType + "");
                }
                //ALTER TABLE tablename MODIFY columnname INTEGER;
            } else {
                //if it does not exist it means someone introduced a new column
                //ALTER TABLE Employees CHANGE COLUMN empName empName VARCHAR(50) AFTER department;
                //the strategy is to find the first column that exists before or after and use it as
                //a reference for the column insert
                //int expectedIndex = x; //this is where we hope the column to exist
                //we use the after strategy
                // boolean backwards = false;
                if (x == 0) { //this means this is the first column and its new
                    db.execute("ALTER TABLE " + table + " ADD " + expectColName + " " + expectType + " FIRST");
                    alterRegister.add(expectColName);
                } else {
                    for (int y = (x - 1); y >= 0; y--) { //backwards
                        String prev = expectedColData[y];
                        String[] prevVals = prev.replaceAll("\\s+", " ").split(" ");
                        String prevColName = prevVals[0].toUpperCase();
                        int prevIndex = currentColNames.indexOf(prevColName); //if prev index > -1 
                        //incase this value is in current columns or we have already added it to the columns
                        if ((prevIndex > -1 || alterRegister.contains(prevColName)) && prevIndex < x) {
                            //this value is the first column directly before
                            db.execute("ALTER TABLE " + table + " ADD " + expectColName + " " + expectType + " AFTER " + prevColName + "");
                            alterRegister.add(expectColName);
                            break;
                        }
                    }
                }
            }
        }
        //do the column alterations first
        //["TRAN_FLAG TEXT","NARRATION TEXT"]
        //["TRAN_FLAG TEXT","TRAN_TYPE TINYINT","NARRATION TEXT"]
    }
    
}
