package com.quest.mobileocr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME="QUEST_OCR";

    private static SQLiteDatabase dbRead;
    private static SQLiteDatabase dbWrite;

    public Database(Context cxt){
        super(cxt,DATABASE_NAME,null,1);
        dbRead = this.getReadableDatabase();
        dbWrite = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create the tables we will need currently
        addTable(db, "LOCAL_DATA",
                new String[]{"KEY_STORAGE","VALUE_STORAGE"}, 
                new String[]{"TEXT","TEXT"});
        //category, primary_key_name, primary_key_value, properties
        addTable(db, "OCR_DATA",
                new String[]{"category", "primary_key_name","primary_key_value","properties","timestamp"},
                new String[]{"TEXT", "TEXT","TEXT","TEXT","LONG"});
        
        addTable(db, "PATTERN_DATA",
                new String[]{"category", "char_map", "action","timestamp"},
                new String[]{"TEXT", "TEXT", "TEXT","LONG"});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }
    
    public void addTable(SQLiteDatabase db,String name,String [] cols,String [] dataTypes){
        String sql = "create table "+name+" (";
        for(int x = 0; x < cols.length; x++){
            String col = cols[x];
            String type = dataTypes[x];
            sql = x == cols.length - 1 ? sql + col + " " + type + ")" : sql + col + " " + type + ",";
        }
       db.execSQL(sql);
    }

    public static boolean ifValueExists(String value, String column, String table){
        Cursor cs = dbRead.rawQuery("SELECT " + column + " FROM " + table + " WHERE " + column + "='" + value + "'", null);
        String exists = null;
        try {
            while(cs.moveToNext()){
                exists = cs.getString(cs.getColumnIndex(column));
            }
            cs.close();
            return exists != null && exists.length() > 0;
        }
        catch(Exception e){
            return false;
        }
    }
    
    /**
     * this method checks whether the data specified exists in a single row for 
     * the specified columns
     * @param table
     * @param cols
     * @param data
     * @return 
     */
    public static boolean exists(String table,String [] cols, Object [] data){
        StringBuilder sql = new StringBuilder("SELECT * FROM "+table+" WHERE ");
        for(int x = 0; x < cols.length; x++){
            String col = cols[x];
            String value = "'"+data[x]+"'";
            sql = x == cols.length - 1 ? sql.append(col).append(" = ").append(value) : sql.append(col).append(" = ").append(value).append(" AND ");
        }
        Log.i("sql:exists",sql.toString());
        Cursor cs = dbRead.rawQuery(sql.toString(), null);
        int count = 0;
        try {
            while (cs.moveToNext()) {
                count++;
                if(count > 0) return true;
            }
            cs.close();
        }
        catch(Exception e){
            
        }
        return false;
    }
    

    public static void update(String[] tableNames, String[] values, String[] conditions) {
        StringBuilder builder = new StringBuilder("UPDATE");
        for (int x = 0; x < tableNames.length - 1; x++) {
            builder.append(" ").append(tableNames[x]).append(" ,");
        }
        builder.append(" ").append(tableNames[tableNames.length - 1]).append(" ");
        builder.append("SET ");

        if (values.length > 0) {
            for (int x = 0; x < values.length - 1; x++) {
                builder.append(" ").append(values[x]).append(" AND");
            }
            builder.append(" ").append(values[values.length - 1]);
        }

        if (conditions.length > 0) {
            builder.append(" WHERE");
            for (int x = 0; x < conditions.length - 1; x++) {
                builder.append(" ").append(conditions[x]).append(" AND");
            }
            builder.append(" ").append(conditions[conditions.length - 1]);
        }
        Log.i("sql:update",builder.toString());
        dbWrite.execSQL(builder.toString());
    }
    
    public static void doInsert(final String table,final String [] cols,final Object [] values){
        Callback cb = new Callback() {
            @Override
            public Object doneInBackground() {
                ContentValues data = new ContentValues();
                for(int x = 0; x < cols.length; x++){
                    String col = cols[x];
                    Object value = values[x];
                    data.put(col, value.toString());
                }
                Log.i("sql:insert",data.toString());
                dbWrite.insert(table, null, data);
                return null;
            }

            @Override
            public void doneAtEnd(Object result) {

            }

            @Override
            public void doneAtBeginning() {

            }
        };

        AsyncHandler handler = new AsyncHandler(cb);
        handler.execute();

    }

    public static void put(final String key,final String value){
        Callback cb = new Callback() {
            @Override
            public Object doneInBackground() {
                boolean exists = ifValueExists(key,"KEY_STORAGE","LOCAL_DATA");
                if(exists) {
                    dbWrite.execSQL("UPDATE LOCAL_DATA SET VALUE_STORAGE = '" + value + "' WHERE KEY_STORAGE = '" + key + "'");
                }
                else{
                  //insert
                    ContentValues values = new ContentValues();
                    values.put("KEY_STORAGE", key);
                    values.put("VALUE_STORAGE",value);
                    dbWrite.insert("LOCAL_DATA", null, values);
                }
                return null;
            }

            @Override
            public void doneAtEnd(Object result) {

            }

            @Override
            public void doneAtBeginning() {


            }
        };

        AsyncHandler handler = new AsyncHandler(cb);
        handler.execute();

    }


    public static String get(final String key){
        Cursor cs = dbRead.rawQuery("SELECT * FROM LOCAL_DATA WHERE KEY_STORAGE='"+key+"'", null);
        try{
            String value = "";
            while(cs.moveToNext()) {
                value = cs.getString(1);
            }
            cs.close();
            return value;
        }
        catch(Exception e){
            return null;
        }
    }
    
    
    public JSONObject query(String sql) {
        JSONObject json = new JSONObject();
        try {
            Cursor cs = dbRead.rawQuery(sql, null);
            if (cs == null) {
                return new JSONObject();
            }
            String [] labels = cs.getColumnNames();
            for (String label : labels) {
                json.put(label, new JSONArray());
            }
            while (cs.moveToNext()) {
                for (int x = 0; x < labels.length; x++) {
                    try {
                        String value = cs.getString(x);
                        ((JSONArray) json.get(labels[x])).put(value);
                    } catch (Exception e) {
                        
                    }
                }
            }
            cs.close();
            return json;
        } catch (Exception ex) {
            return json;
        }
    }
    
    public static ArrayList<ArrayList<String>> doSelect(String[] columnNames, String[] tableNames, String[] conditions) {
        //Select col1,col2 from table1 where 
        StringBuilder builder = new StringBuilder("SELECT");
        for (int x = 0; x < columnNames.length - 1; x++) {
            builder.append(" ").append(columnNames[x]).append(" ,");
        }
        builder.append(" ").append(columnNames[columnNames.length - 1]).append(" ");
        builder.append("FROM ");
        for (int x = 0; x < tableNames.length - 1; x++) {
            builder.append(" ").append(tableNames[x]).append(" ,");
        }
        builder.append(" ").append(tableNames[tableNames.length - 1]).append(" ");
        if (conditions.length > 0) {
            builder.append("WHERE ");
            for (int x = 0; x < conditions.length - 1; x++) {
                builder.append(" ").append(conditions[x]).append(" AND");
            }
            builder.append(" ").append(conditions[conditions.length - 1]);
        }
        builder.append(" ORDER BY timestamp desc");
        String sql = builder.toString();
        Log.i("sql:select",sql);
        Cursor cs = dbRead.rawQuery(sql,null);
        try {
            ArrayList<ArrayList<String>> all = new ArrayList();
            while (cs.moveToNext()) {
                ArrayList row = new ArrayList();
                for(int x = 0; x < cs.getColumnCount(); x++){
                    String value = cs.getString(x);
                    row.add(value);
                }
                all.add(row);
            }
            cs.close();
            return all;
        } catch (Exception e) {
            return null;
        }

    }
    
    public SQLiteDatabase dbRead(){
        return dbRead;
    }
    

    public static void remove(final String key){
       dbWrite.rawQuery("DELETE FROM LOCAL_DATA WHERE KEY_STORAGE='"+key+"'", null);
    }

}

