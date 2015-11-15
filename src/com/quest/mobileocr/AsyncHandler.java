package com.quest.mobileocr;

import android.os.AsyncTask;

public class AsyncHandler extends AsyncTask<Void, Void, Object> {

    private Callback cb;

    public AsyncHandler(Callback cb) {
        this.cb = cb;
    }

    @Override
    protected Object doInBackground(Void... arg0) {
        // TODO Auto-generated method stub
        return cb.doneInBackground();
    }

    @Override
    protected void onPreExecute() {
        // start a loading dialog		  
        cb.doneAtBeginning();
    }

    @Override
    protected void onPostExecute(Object obj) {
        cb.doneAtEnd(obj);
    }
}
