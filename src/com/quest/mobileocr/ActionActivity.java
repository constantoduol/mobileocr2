package com.quest.mobileocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
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
import java.util.HashMap;
import java.util.Iterator;
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
    
    private static String currentCategory;

    private static Database db;

    private static String specialChars = "-_/\\,.!@#$%^&*()+=[]{}`~|?<>';:";

    //private static String alphaDigitMap = "abcdefghijklmnopqrstuvwxyz0123456789";
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
                if (currentAction.equals("web_search_action")) {
                    dismissProgress();
                    //what to do after the page finishes loading
                } else if (currentAction.equals("ussd_action")) {
                    wv.loadUrl("javascript:app.loadUSSDField('" + Uri.encode(currentText) + "','"+currentCategory+"')");
                } else if (currentAction.equals("associate_action")) {
                    wv.loadUrl("javascript:app.loadRecordField('" + Uri.encode(currentText) + "','"+currentCategory+"')");
                    //wv.loadUrl("javascript:app.populateExisting('"+Uri.encode(currentText)+"')");
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

    public ProgressDialog showProgress(String msg) {
        dialog = ProgressDialog.show(this, "", msg, true);
        dialog.show();
        return dialog;
    }

    public void dismissProgress() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void performRequiredAction() {
        try {
            Intent intent = getIntent();
            String type = intent.getStringExtra("action_type"); //the default action, this could be AI selected or user selected
            String text = intent.getStringExtra("detected_text");
            String category = intent.getStringExtra("category");
            currentAction = type;
            currentText = text;
            currentCategory = category;
            if (type.equals("web_search_action")) {
                dialog = showProgress("Loading. Please wait...");
                wv.loadUrl(SEARCH_URL + text); //perform a search action based on the detected text
                saveRecordModel("__web_search__",text,type);
            } else if (type.equals("ussd_action")) {
                //launch a ussd interface
                wv.loadUrl("file:///android_asset/ussd_interface.html");
            } else if (type.equals("associate_action")) {
                //launch a properties editor interface
                wv.loadUrl("file:///android_asset/props_interface.html");
            }  else if (type.equals("search_action")) {
                //launch a properties editor interface
                wv.loadUrl("file:///android_asset/search_interface.html");
            }
        } catch (Exception e) {

        }
    }

    public static ActionActivity getInstance() {
        return activity;
    }

    public void toast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public String getExistingData(String content) {
        ArrayList<ArrayList<String>> data = Database.doSelect(
                new String[]{"*"},
                new String[]{"OCR_DATA"},
                new String[]{"primary_key_value = '" + content.trim() + "'"});
        JSONArray arr = data.size() > 0 ? new JSONArray(data.get(0)) : new JSONArray();//get the first value
        return arr.toString();
    }
    
    public String getCategoryProperties(String category){
        JSONObject obj = new JSONObject();
        try {
            ArrayList<ArrayList<String>> cats = Database.doSelect(new String[]{"*"},new String[]{"OCR_DATA"},new String[]{"category = '"+category+"'"});
            if(cats.isEmpty()) return new JSONObject().toString();
            ArrayList<String> list = cats.get(0);
            JSONObject props = new JSONObject(list.get(3));
            for(int x = 0; x < props.names().length(); x++){
                String key = props.names().optString(x);
                props.put(key, "");
            }
            obj.put("props", props.toString());
            obj.put("primary_key_name",list.get(1));
            return obj.toString();
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
            return obj.toString();
        }
    }

    public void saveRecord(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String category = obj.optString("category");
            String primaryKeyValue = obj.optString("primary_key_value");
            String primaryKeyName = obj.optString("primary_key_name");
            String properties = obj.optString("properties");
            String action = obj.optString("action");
            //category, primary_key_name, primary_key_value, properties
            //if category, primaryKeyValue, primaryKeyName already exist in the
            //database we dont insert but do an update
            boolean exists = Database.exists("OCR_DATA",
                    new String[]{"category", "primary_key_name", "primary_key_value"},
                    new String[]{category, primaryKeyName, primaryKeyValue});
            if (exists) {
                Database.update(new String[]{"OCR_DATA"},
                        new String[]{"properties = '" + properties + "'"},
                        new String[]{"category = '" + category + "'",
                            "primary_key_name = '" + primaryKeyName + "'",
                            "primary_key_value = '" + primaryKeyValue + "'"});
            } else {
                Database.doInsert("OCR_DATA",
                        new String[]{"category", "primary_key_name", "primary_key_value", "properties","timestamp"},
                        new Object[]{category, primaryKeyName, primaryKeyValue, properties,System.currentTimeMillis()});

            }
            saveRecordModel(category, primaryKeyValue, action);
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveRecordModel(String category, String primaryKeyValue, String action) {
        // String ad_map = alphabetMap(primaryKeyValue);
        //input is the char map and output is the action
        String charMap = characterMap(primaryKeyValue);
        //we will save the alphaDigitmap, the character map
        //the length of the string for a given data category
        //there can be more than one pattern data for a given category
        //check whether we have the specific pattern data before inserting
        boolean exists = Database.exists("PATTERN_DATA",
                new String[]{"category", "char_map", "action"},
                new Object[]{category, charMap, action});
        if (!exists) {
            Database.doInsert("PATTERN_DATA",
                    new String[]{"category", "char_map", "action","timestamp"},
                    new Object[]{category, charMap, action,System.currentTimeMillis()});
        }
    }

    public String getRecordModel(String primaryKeyValue) {
        try {
            //String ad_map = alphabetMap(primaryKeyValue);
            String charMap = characterMap(primaryKeyValue);
            JSONObject obj = new JSONObject();
            obj.put("char_map", charMap);
            //obj.put("alpha_digit_map", ad_map);
            return obj.toString();
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject().toString();
        }
    }

    public static String characterMap(String input) {
        //e.g KAG 564M is lllsdddl
        String map = "";
        char[] arr = input.toCharArray();
        for (int x = 0; x < arr.length; x++) {
            Character ch = (Character) arr[x];
            if (Character.isLetter(ch)) {
                map += "l";
            } else if (Character.isDigit(ch)) {
                map += "d";
            } else if (Character.isSpaceChar(ch)) {
                map += "s";
            } else {
                //special character
                int index = specialChars.indexOf(ch.toString());
                if (index == -1) {
                    map += "x";
                } else {
                    map += index;
                }
            }
        }
        return map;
    }

//    public String alphabetMap(String input){
//        //26 is for the 26 alphabet letters
//        //10 is for the digits 0 to 9
//        int [] map = new int[36]; //26 + 10
//        for(int x = 0; x < map.length; x++){
//            //count the occurrence of each letter and digit in the string and store them
//            //in the array of ints
//            map[x] = charOccurrences(input, alphaDigitMap.charAt(x));
//        }
//        String adMap = "";
//        for (int a : map) {
//            adMap += a;
//        }
//        return adMap;
//    }
//    private int charOccurrences(String str, char toFind){
//        char [] arr = str.toCharArray();
//        int count = 0;
//        for(int y = 0; y < arr.length; y++){
//            if(arr[y] == toFind){
//                count++;
//            }
//        }
//        return count;
//    }


    public void performUSSD(String prefix, String postfix) {
        Log.i("app", "perform ussd called");
        String ussdCode = "*" + prefix + "*" + postfix + Uri.encode("#");
        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:" + ussdCode)));
    }
    
    public String search(String json){
        try {
            JSONObject data = new JSONObject(json);
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT ").append(data.optString("column")).append(" FROM ").append(data.optString("table")); //tables
            builder = data.optString("where").length() > 0 ? builder.append(" WHERE ").append(data.optString("where")) : builder; 
            builder = data.optString("orderby").length() > 0 ? builder.append(" ORDER BY ").append(data.optString("orderby")) : builder;
            builder = data.optString("limit").length() > 0 ? builder.append(" LIMIT ").append(data.optString("limit")) : builder;
            Log.i("sql:search", builder.toString());
            JSONObject query = db.query(builder.toString());
            Log.i("sql:search:result", query.toString());
            return query.toString();
        } catch (JSONException ex) {
            Logger.getLogger(ActionActivity.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject().toString();
        }
    }
}
