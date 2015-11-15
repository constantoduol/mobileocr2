package com.quest.mobileocr;

public interface Callback {
       
   public void doneAtBeginning();
   
   public Object doneInBackground();
   
   public void doneAtEnd(Object result);
   
}
