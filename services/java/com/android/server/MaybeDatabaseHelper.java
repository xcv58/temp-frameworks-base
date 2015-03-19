/* maybe We Should Enable More Uncertain Mobile App Programming
*  PhoneLab - phone-lab.org
*  <License info>
*  @author: Sriram Shantharam
*  
*/
package com.android.server;


import android.os.IBinder;
import android.os.Binder;
import android.os.IMaybeService;
import android.os.RemoteException;
import android.util.Log;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.content.Context;

import java.io.OutputStream;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import java.io.IOException;


import java.lang.Void;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
  final class MaybeDatabaseHelper{

    private static final String DBTAG = "MaybeDatabaseHelper";
    private static final String DBNAME = "maybe.db";
    private static final int DB_VERSION = 1;

    private static MaybeDatabaseHelper sInstance = null;
    private static final Object sInstanceLock = new Object();
    private static SQLiteDatabase sDatabase = null;

    private static final Object sAppTableLock = new Object();
    private Context mContext;
    private static final String APP_TABLE_NAME = "apptable";

    public static final String ID_COL = "_id";
    public static final String PACKAGE_COL = "package";
    public static final String URL_COL = "url";
    public static final String DATA_COL = "data"; // data stored as JSON string 
    public static final String STALE_COL = "stale"; // boolean, get from server?
    public static final String PUSH_COL = "push"; // boolean, push data to apps?
    public static final String UPLOAD_COL = "upload";
    public static final String HANDLER_COL = "handler"; //handler reference for pushing data to app
    public static final String GCM_COL = "gcmid";
    public static final String HASH_COL = "hash";

    private boolean mInitialized = false;

    private MaybeDatabaseHelper(final Context context){

      new Thread(){
        @Override
        public void run(){
          init(context);
        }
      }.start();
    }

    public static MaybeDatabaseHelper getInstance(Context context){
      synchronized(sInstanceLock){
        if(sInstance == null){
          sInstance = new MaybeDatabaseHelper(context);
        }
        return sInstance;
      }
    }


    private synchronized void init(Context context){
      if(mInitialized)
        return;
      mContext = context;
      initDatabase(mContext);
      mInitialized = true;
      notify();
    }

    private void initDatabase(Context context){
      try{
        sDatabase = context.openOrCreateDatabase(DBNAME, 0, null);
      }catch(SQLiteException e){
        Log.e(DBTAG, "Exception while opening database. Retrying.");
        e.printStackTrace();
        if (context.deleteDatabase(DBNAME)) {
                sDatabase = context.openOrCreateDatabase(DBNAME, 0,
                        null);
            }
      }

      if(sDatabase == null){
        //Use for any damage control because something went wrong horribly 
        //if sDatabase is null at this point
        Log.d(DBTAG, "sDatabase is null");
      }

      //sDatabase.execSQL("DROP TABLE IF EXISTS apptable");

      sDatabase.execSQL("CREATE TABLE IF NOT EXISTS "+APP_TABLE_NAME
        +" ("+ ID_COL+" INTEGER PRIMARY_KEY, "
        +PACKAGE_COL+ " TEXT, "
        +GCM_COL+ " TEXT, "
        +URL_COL + " TEXT, "
        +HASH_COL+ " TEXT, "
        +DATA_COL+ " TEXT, "
        +STALE_COL+ " BOOLEAN, "
        +PUSH_COL+ " BOOLEAN "
        +");");



    }

    private boolean checkInitialized() {
      synchronized (this) {
        while (!mInitialized) {
          try {
            wait();
          }catch(InterruptedException e){
            Log.e(DBTAG, "Exception during checkInitialized", e);
          }
        }
      }
      return (sDatabase != null);
    }

    public boolean hasEntries(String packagename){
      if(!checkInitialized()){
        return false;
      }

      Cursor cursor = null;
      boolean ret = false;

      try{
       /* cursor = sDatabase.query(APP_TABLE_NAME, new String[]{DATA_COL}, PACKAGE_COL+"="+packagename,
          null, null, null, null, null);
        */  
        cursor = sDatabase.rawQuery("SELECT count(*) FROM "+APP_TABLE_NAME+" where "+PACKAGE_COL+" = ?", new String[]{packagename});
        /*
        if(cursor == null){
          Log.v(DBTAG, "cursor is null");
          return false;
        }
        ret = (cursor.moveToFirst() == true);
        */
        Log.v(DBTAG, "num columns|column"+cursor.getColumnCount()+"|"+cursor.getColumnName(0)+"|");
        cursor.moveToFirst();
        ret = (cursor.getInt(cursor.getColumnIndex("count(*)"))>0);
        Log.v(DBTAG, "Package exists: Count:"+ret+cursor.getCount());
      }catch (IllegalStateException e){
        Log.e(DBTAG, "IllegalStateException in hasEntries", e);

      }catch(Exception e){
        e.printStackTrace();
      }finally{
        if(cursor!=null){
          cursor.close();
        }
      }
      return ret;
    }

    public long setUrl(String packagename, String url){
      Log.v(DBTAG, "packagename="+packagename+" url="+url);
      if(packagename == null || url == null){
        return 0;
      }
      Log.v(DBTAG, "packagename="+packagename+" url="+url);
      if(rowExists(packagename)){
        Log.v(DBTAG, "Row exists");
        listTableContents(); //TODO: test- remove in final code
        return 0;
      }
      Log.v(DBTAG, "Row does not exist");
        listTableContents();
        long ret;

      synchronized(sAppTableLock){
        final ContentValues c = new ContentValues();
        c.put(PACKAGE_COL, packagename);
        c.put(URL_COL, url);
        ret = sDatabase.insert(APP_TABLE_NAME, URL_COL, c);
      }
      Log.v(DBTAG, "Database value inserted"+getAppDataFromDb(URL_COL, packagename));
      return ret;
    }

    public int setData(String packagename, String data){
      Log.v(DBTAG, "packagename="+packagename+" data="+data);
      if(data==null || packagename == null)
        return 0;
      int ret;
      synchronized(sAppTableLock){
        final ContentValues c = new ContentValues();
        c.put(DATA_COL, data);
        c.put(PUSH_COL, true);
        ret = sDatabase.update(APP_TABLE_NAME, c, PACKAGE_COL+"='"+packagename+"'", null);
      }
      Log.v(DBTAG, "Database value inserted"+getAppDataFromDb(DATA_COL, packagename));
      return ret;
    }

    public int deletePackageInfo(String packagename){
      //packagename should never be null! or all the rows get deleted
      if(packagename == null){
        return 0;
      }
      int ret;
      synchronized(sAppTableLock){
        ret = sDatabase.delete(APP_TABLE_NAME, PACKAGE_COL+"="+packagename,null);
      }
      return ret;
    }

    public int updateDataInDb(String packagename, String column, String columndata){
      Log.v(DBTAG, "Packagename|column|columdata:"+packagename+"|"+column+"|"+columndata);
      if(packagename == null || column == null){
        return 0;

      }
      if(column != URL_COL && column != GCM_COL && column != DATA_COL){
        return 0;

      }
      int ret;
      synchronized(sAppTableLock){
        final ContentValues c = new ContentValues();
        c.put(column, columndata);
        ret = sDatabase.update(APP_TABLE_NAME, c, PACKAGE_COL+"= ?", new String[]{packagename});
      }
      return ret;
    }

    String getAppDataFromDb(String column, String packagename){
      if(packagename == null || column == null){
        Log.i(DBTAG, "packagename or column is null");
        return null;
      }
      if(column != DATA_COL && column != URL_COL){
         Log.i(DBTAG, "Not an acceptable query column");
        return null;
      }

      synchronized(sAppTableLock){
        String ret_data = null;
        Cursor cursor = null;
        try{
          cursor = sDatabase.query(APP_TABLE_NAME, null, PACKAGE_COL+"='"+packagename+"'",
                    null, null, null, null, null);
          if(cursor.moveToFirst()){
            ret_data = new String(cursor.getString(cursor.getColumnIndex(column)));
          }

        }catch(IllegalStateException e){
          Log.e(DBTAG, "getAppDataFromDB failed", e);
        }finally{
          if(cursor != null) cursor.close();
        }
         Log.i(DBTAG, "DB Query: return data:"+ret_data);
        return ret_data;
      }
    }

    public void listTableContents(){
      Cursor cursor = null;
        try{
          cursor = sDatabase.query(APP_TABLE_NAME, null, null,
                    null, null, null, null, null);
          if(cursor.moveToFirst()){
            while(true){
              Log.v(DBTAG, "Row: Package="+cursor.getString(cursor.getColumnIndex(PACKAGE_COL))+"URL="+cursor.getString(cursor.getColumnIndex(URL_COL))+"DATA="+cursor.getString(cursor.getColumnIndex(DATA_COL)));
              if(cursor.isLast())
                break;
              cursor.moveToNext();
            }
          }

        }catch(IllegalStateException e){
          Log.e(DBTAG, "getAppDataFromDB failed", e);
        }finally{
          if(cursor != null) cursor.close();
        }
    }

/* TODO: Probably incorrect. Check later */
    public boolean rowExists(String packagename){
      Log.v(DBTAG, "rowExits:"+packagename);
      Cursor cursor = null;
      String selection = PACKAGE_COL+"='"+packagename+"'";
      boolean ret = false;
        try{
          cursor = sDatabase.query(APP_TABLE_NAME, null, selection,
                    null, null, null, null, null);
          if(cursor.getCount()>0){
            ret = true;
          }else{
            ret = false;
          }
          

        }catch(IllegalStateException e){
          Log.e(DBTAG, "getAppDataFromDB failed", e);
        }finally{
          if(cursor != null) cursor.close();
        }
        return ret;
    }

    




}