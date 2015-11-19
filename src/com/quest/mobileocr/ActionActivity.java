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
                    wv.loadUrl("javascript:app.loadUSSDField('" + Uri.encode(currentText) + "')");
                } else if (currentAction.equals("associate_action")) {
                    wv.loadUrl("javascript:app.loadRecordField('" + Uri.encode(currentText) + "')");
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
            currentAction = type;
            currentText = text;
            JSONObject obj = new JSONObject();
            if (type.equals("web_search_action")) {
                dialog = showProgress("Loading. Please wait...");
                wv.loadUrl(SEARCH_URL + text); //perform a search action based on the detected text
                obj.put("properties", new JSONObject().toString());
                obj.put("category", "__web_search__");//a custom category
                obj.put("primary_key_value", text);
                obj.put("primary_key_name", "__web_search_string__"); //a custom primary key name
                obj.put("action", type); //action to take is web_search_action
                saveRecord(obj.toString());
            } else if (type.equals("ussd_action")) {
                //launch a ussd interface
                wv.loadUrl("file:///android_asset/ussd_interface.html");
            } else if (type.equals("associate_action")) {
                //launch a properties editor interface
                wv.loadUrl("file:///android_asset/props_interface.html");
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
                        new String[]{"category", "primary_key_name", "primary_key_value", "properties"},
                        new String[]{category, primaryKeyName, primaryKeyValue, properties});

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
                    new String[]{"category", "char_map", "action"},
                    new String[]{category, charMap, action});
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

    public String characterMap(String input) {
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
    //this function is our gamma in the naive bayes model
    //where gamma(input) = class
    public String getCategoryFromModels(String inputData) {
        //we make sense of the model by providing input text
        //based on the pattern data we have we try to find the 
        //closest match to our input string
        //get length of the string, charmap and most frequent class
        Integer newLength = inputData.length();
        String newCharMap = characterMap(inputData);
        //String newAdMap = alphabetMap(inputData);
        //use the maps to get the relevant model
        //we will fetch existing maps from the database and compare
        //them with the maps for this input string. we employ the
        //naive bayes form of supervised machine learning to classify
        //the input string
        ArrayList<ArrayList<String>> data = Database.doSelect(
                new String[]{"*"},
                new String[]{"PATTERN_DATA"},
                new String[]{""});
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
        double maxCharMapScore = 0.0;
        for (ArrayList<String> list : data) { //iterate through the category data
            String category = list.get(0);
            String char_map = list.get(1);
            String action = list.get(2); //this is the action to take based on the detected text
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
        double maxLengthScore = 0.0;
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
        //we now get the category assigned
        //if any of the strings are blank it means it is unassigned
        //so we return a blank string
        JSONObject obj = new JSONObject();
        try {
            if (charMapSelectedCategory.isEmpty() || freqSelectedCategory.isEmpty() || lengthSelectedCategory.isEmpty()) {
                obj.put("category", "");
                obj.put("action", "");
                return obj.toString();
            } else if (charMapSelectedCategory.equals(freqSelectedCategory) && freqSelectedCategory.equals(lengthSelectedCategory)) {
            //this is a unanimous vote
                //all three criteria agreed on one category
                obj.put("category", charMapSelectedCategory);
                obj.put("action", charMapSelectedAction);
                return obj.toString();
            } else if (charMapSelectedCategory.equals(freqSelectedCategory)) {
                obj.put("category", charMapSelectedCategory);
                obj.put("action", charMapSelectedAction);
                return obj.toString();
            } else if (freqSelectedCategory.equals(lengthSelectedCategory)) {
                obj.put("category", freqSelectedCategory);
                obj.put("action", freqSelectedAction);
                return obj.toString();
            } else if (charMapSelectedCategory.equals(lengthSelectedCategory)) {
                obj.put("category", lengthSelectedCategory);
                obj.put("action", lengthSelectedAction);
                return obj.toString();
            } else {
                obj.put("category", "");
                obj.put("action", "");
                return obj.toString(); //indecision
            }
        } catch (Exception e) {
            return obj.toString();
        }
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
        return equalityCount.doubleValue() / shorter.doubleValue();
    }

    private void performUSSD(String prefix, String postfix) {
        String ussdCode = "*" + prefix + "*" + postfix + Uri.encode("#");
        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:" + ussdCode)));
    }

}
