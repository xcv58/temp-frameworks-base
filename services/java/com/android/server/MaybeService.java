/* maybe We Should Enable More Uncertain Mobile App Programming
*  PhoneLab - phone-lab.org
*  <License info>
*  @author: Sriram Shantharam
*  
*/

/*
* ~List of TODOs/ Nice to have~
* 1. Sanitize all inputs like urls, json strings (partially done for JSONObjects)
* 2. Check for redundant correctness checks in MaybeService.java and MaybeDatabaseHelper.java
* 3. Maybe club all the Asynctasks into one?
*/

package com.android.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.os.IMaybeService;
import android.os.RemoteException;
import android.util.Log;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.content.Context;
import org.json.JSONObject;
import android.telephony.TelephonyManager;
import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import java.io.OutputStream;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import java.io.IOException;
import android.net.http.AndroidHttpClient;
import org.apache.http.client.methods.HttpGet;
import android.os.AsyncTask;
import org.json.JSONObject;
import org.json.JSONException;
import java.lang.Void;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import java.net.URLEncoder;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
//import com.google.android.gms.common.*;

import android.os.IMaybeListener;
import android.os.MaybeListener;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class MaybeService extends IMaybeService.Stub {
    public static final String SERVICE_NAME = "maybe";
  private static final String TAG = "MaybeService";
  private static final String URL = "http://maybe.xcv58.me:5121/query?";
  private static final String SHARED_PREFERENCES_NAME = "maybeServicePreferences";
  private static final String GCM_ID_KEY = "gcm_id";
  private static final String GCM_PROJECT_ID = "0";
  //Server Error Codes
  private static final String ERR_NO_RECORDS_FOUND = "No Record(s) Found";
  private static final String ERR_DUPLICATE_KEY = "E11000 duplicate key error index";
  private static final String ERR_GENERIC_ERROR = "Error";
  private static final String ERR_JSON_ERROR = "JSON parse error";
  private static final int STATUS_200OK = 200;
  private static final int STATUS_201CREATED = 201;
  private static final int STATUS_204NOCONTENT = 204;

  protected static final String STRING_TYPE = "type";
  protected static final String STRING_NAME = "name";
  protected static final String STRING_LABEL = "labels";
  protected static final String STRING_SCORE = "score";
  protected static final String STRING_LOG = "log";
  protected static final String STRING_BAD = "bad";


  private Context mContext;
  private String mJSONResponse;
  private boolean mHasResponse=false;
  private final MaybeDatabaseHelper mDbHelper;
  private static Object sNetworkCallLock = new Object();
  private String mDeviceMEID=null;
  private String mHashedMEID=null;
  private String mGCMId = null;
  private SharedPreferences mSharedPrefs;
  private Object mGCMLock = new Object();
  private ConnectivityManager mConnManager;
  private float mPollInterval = 1.0f;

  public MaybeService(Context context) {
    super();
    mContext = context;
    Log.i(TAG, "MaybeService Called context:"+mContext.getPackageCodePath());

    //init Database
    mDbHelper = MaybeDatabaseHelper.getInstance(mContext);
    getDeviceMEID();
    Log.d(TAG, "Device MEID:"+ mDeviceMEID);
    mSharedPrefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }

/*
  private void getGCMId(){
    if(checkPlayServices()){
      getRegistrationIdLocked();
    }else{
      mGCMId = "NULL";
    }

  }



  void getRegistrationIdLocked(){

    synchronized(mGCMLock){
      String gcmId = mSharedPrefs.getString(GCM_ID_KEY, "");
      if(gcmId.isEmpty()){
        
        
          GCMTask gcmTask = new GCMTask();
          gcmTask.execute(null, null, null);
          try{
            mGCMLock.wait();
           }catch(InterruptedException e){
              Log.e(TAG, "Exception while waiting for task");
              e.printStackTrace();
           }
        
      }else{
        mGCMId = gcmId;
      }
    }
    Log.i(TAG,"GCM ID:"+mGCMId);

  }

  class GCMTask extends AsyncTask<String, Void, String>{

    public GCMTask(){

    }

    @Override
    protected String doInBackground(String... params){
      GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(mContext);
            try {
                String regId = gcm.register(GCM_PROJECT_ID);
                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                //TODO: maybe service should send this every time a new app registers
                // a new url
                //sendRegistrationIdToBackend();
                synchronized(mGCMLock){
                  try{
                  SharedPreferences.Editor editor = mSharedPrefs.edit();
                  editor.putString(GCM_ID_KEY, regId);
                  editor.commit();
                  mGCMId = regId;
                  mGCMLock.notify();

                  }catch(InterruptedException e){
                    Log.e(TAG, "Exception in task");
                    e.printStackTrace();
                  }
              }

              


            } catch (IOException e) {
                Log.e(TAG, "Exception while registering with GCM");
                e.printStackTrace();
                
            }
            return null;
    }

    

    @Override
    protected void onPostExecute(String result){
      
    }
    }

    
    
        
        
        
  

  



  private boolean checkPlayServices() {
      int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
      if (resultCode != ConnectionResult.SUCCESS) {
          if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
              Log.i(TAG, "Google play services not installed");
          } else {
              Log.i(TAG, "This device is not supported.");
              
          }
          return false;
      }
      return true;
  }
*/
  public String getCurrentTime() throws RemoteException {
    Log.d(TAG, "Time"+":getCurrentTime()");
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    Log.i(TAG, (sdf.format(cal.getTime())).toString());
    return (sdf.format(cal.getTime())).toString();
  }

  public void printCurrentTime() throws RemoteException {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    Log.i(TAG, (sdf.format(cal.getTime())).toString());
  }
  
  public int registerUrl(String url, String hash){
    //add to database
    String packageName = getCallerPackageName();
    if(mDbHelper.hasEntries(packageName)){
      String urlInDb = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName);
      if(url == urlInDb){
        return 0;
      }else{
        mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.URL_COL, url);
      }
    }else{
      mDbHelper.setUrl(packageName, url);
      if(hash!=null){
        mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.HASH_COL, hash);
      }

      

      synchronized(sNetworkCallLock){
        
        
        new DeviceRegisterTask().execute(url, packageName, null);
        
            try{
              sNetworkCallLock.wait();
              
              mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.DATA_COL, mJSONResponse);
              
            }catch(InterruptedException e){
              e.printStackTrace();
            }
         
        Log.d(TAG, "Received data from TASK: "+mJSONResponse);
      
      }

    }
    
    
    return 1; // Error codes in future?
  }

  public int registerUrl(String url) throws RemoteException{
    return registerUrl(url, null);
  }

  public int deletePackageData(){
    return mDbHelper.deletePackageInfo(getCallerPackageName());
  }
 

/* deprecated */
  public synchronized String getAppData() throws RemoteException {
    Log.d(TAG,"getAppData called from app:"+getCallerPackageName());
     
      String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, getCallerPackageName());
      

      synchronized(sNetworkCallLock){
        
        
      new JSONDownloaderTask().execute(url, null, null);
      
          try{
            sNetworkCallLock.wait();
          }catch(InterruptedException e){
            e.printStackTrace();
          }
       
      Log.d(TAG, "Received data from TASK: "+mJSONResponse);
      
      }

     //registerUrl(URL); //TODO: test code remove in final version
    mDbHelper.setData(getCallerPackageName(), mJSONResponse);
      
    return mJSONResponse;
  }

  private String getDeviceMEID(){
    if(mDeviceMEID==null){
      TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
      mDeviceMEID = tm.getDeviceId();
      Log.d(TAG, "Device MEID:"+mDeviceMEID);
    }
    return mDeviceMEID;
  }



  /* 
  * Request maybe server for data 
  * (ignore)
  */
  public String getDeviceData(){
    
    synchronized(sNetworkCallLock){
    new JSONDownloaderTask().execute(URL, null, null);
    
        try{
          sNetworkCallLock.wait();
        }catch(InterruptedException e){
          e.printStackTrace();
        }
     
    Log.d(TAG, "Received data from TASK: "+mJSONResponse);
    return mJSONResponse;
    }

  }

  public void requestMaybeUpdates(String url, IMaybeListener listener){

  }

  public void removeMaybeUpdates(IMaybeListener listener){

  }

  public synchronized int getMaybeAlternative(String label){
    String packageName = getCallerPackageName();
    String jsonData = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.DATA_COL, packageName);
    JSONObject jsonObjData = MaybeUtils.getJSONObject(jsonData);
    if(jsonObjData == null){
      return -1;
    }
    int choice = MaybeUtils.getChoiceForLabel(jsonObjData);

    return choice; 
    
  }

  public void badMaybeAlternative(String label, int value){
    String packageName = getCallerPackageName();
    //TODO: Needs improvement
    /*
    if(!MaybeUtils.isValidJSONString(jsonstring)){
      Log.e(TAG, "(1)Invalid JSON Object passed for scoring");
      return; //fail silently
    }
    */
    String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName)+"/"+mDeviceMEID;
    if(url == null){
      Log.e(TAG, "Server URL is null. Server url might not be registered with the service");
      return;
    }
    String hash = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.HASH_COL, packageName);
    JSONObject scoreData = new JSONObject();
    try{
    scoreData.put(STRING_TYPE, STRING_BAD);
    scoreData.put(STRING_NAME, packageName);
    scoreData.put(label,value);
    }catch(JSONException e){
      e.printStackTrace();
    }
    new ServerUpdaterTask().execute(url, hash, scoreData.toString());
    return; //if JSONException fail silently
  }
/*
  {

  }
*/
  public void scoreMaybeAlternative(String label, String jsonString){
    String packageName = getCallerPackageName();
    //TODO: Needs improvement
    if(!MaybeUtils.isValidJSONString(jsonString)){
      Log.e(TAG, "(1)Invalid JSON Object passed for scoring");
      return; //fail silently
    }
    String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName)+"/"+mDeviceMEID;
    if(url == null){
      Log.e(TAG, "Server URL is null. Server url might not be registered with the service");
      return;
    }
    String hash = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.HASH_COL, packageName);
    JSONObject scoreData = new JSONObject();
    try{
    scoreData.put(STRING_TYPE, STRING_SCORE);
    scoreData.put(STRING_NAME, packageName);
    scoreData.put(label,jsonString);
    }catch(JSONException e){
      e.printStackTrace();
    }

    new ServerUpdaterTask().execute(url, hash, scoreData.toString());
    return; //if JSONException fail silently
  }

  public void logMaybeAlternative(String label, String jsonString){

  }



  private String getCallerPackageName(){
    return mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
  }

  /* HTTP post 
  * @deprecated
  */

  class JSONDownloaderTask extends AsyncTask<String, Void, String> {
    HttpClient client;
    
    public JSONDownloaderTask(){
      client = new DefaultHttpClient();
      
    }

    @Override
    protected String doInBackground(String... params){
      String networkResponse = "";
      try{
      
        HttpPost posturl = new HttpPost(params[0]);
        posturl.setHeader("Content-type", "application/json");

        JSONObject data = new JSONObject();
        String meid = getDeviceMEID();
         data.put("deviceid", meid);
        StringEntity se = new StringEntity(data.toString());
        posturl.setEntity(se);
        HttpResponse response = client.execute(posturl);
        StatusLine statusLine = response.getStatusLine();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getEntity().writeTo(out);
        networkResponse = out.toString();
        Log.d(TAG, "Networkd response:"+networkResponse);
        out.close();
      }catch(IOException e){
        e.printStackTrace();
      }catch(Exception e){
        e.printStackTrace();
      }

      return networkResponse;
    }

    

    @Override
    protected void onPostExecute(String result){
      synchronized(sNetworkCallLock){
        mJSONResponse = result;
        sNetworkCallLock.notify();
      }
    }

  }

class ServerUpdaterTask extends AsyncTask<String, Void, String> {
    HttpClient client;
    
    public ServerUpdaterTask(){
      client = new DefaultHttpClient();
      
    }

    @Override
    protected String doInBackground(String... params){
      // params[0]- url, params[1] - package name , params[2] - data in JSONString format
      String networkResponse = "";
      ByteArrayOutputStream out = null;
      Log.v(TAG, "url:"+params[0]+" packagename:"+params[1]+" data:"+params[2]);
      try{
      
        HttpPut putUrl = new HttpPut(params[0]);
        putUrl.setHeader("Content-type", "application/json");
        /*
        JSONObject data = new JSONObject();
        data.put(params[1],params[2]); 
        */
        JSONObject data = MaybeUtils.getJSONObject(params[2]);
        JSONObject labelData = new JSONObject();
        labelData.put(params[1],data);
        JSONObject postData = new JSONObject();
        postData.put("$set",labelData);
        StringEntity se = new StringEntity(postData.toString());
        putUrl.setEntity(se);
        HttpResponse response = client.execute(putUrl);
        //StatusLine statusLine = response.getStatusLine();
        out = new ByteArrayOutputStream();
        response.getEntity().writeTo(out);
        networkResponse = out.toString();
        int statusCode = response.getStatusLine().getStatusCode();
        Log.d(TAG, "Network response|status code:"+networkResponse+statusCode);
        if(statusCode == STATUS_204NOCONTENT || statusCode == STATUS_201CREATED || statusCode == STATUS_200OK){
          networkResponse = out.toString();
        }else{
          networkResponse = ERR_GENERIC_ERROR;
        }
        
        if(out!=null){
          out.close();
          out = null;
        }
      }catch(IOException e){
        e.printStackTrace();
      }catch(Exception e){
        e.printStackTrace();
      }finally{
        
      }

      return networkResponse;
    }

    
    // Don't really need this at present, use in case we need it in future
    // to display toasts/message to the user in case of failures
    @Override
    protected void onPostExecute(String result){
        String packageName = getCallerPackageName();
        if(result.contains(ERR_DUPLICATE_KEY) || result.contains(ERR_GENERIC_ERROR)){
          Log.v(TAG, "Error while updating data to server");
          return;
        }
        
    }

  }

class DeviceRegisterTask extends AsyncTask<String, Void, String> {
    HttpClient client;
    
    public DeviceRegisterTask(){
      client = new DefaultHttpClient();
      
    }

    @Override
    protected String doInBackground(String... params){
      String networkResponse = "";
      ByteArrayOutputStream out=null;
      try{
      
        HttpPost posturl = new HttpPost(params[0]);
        posturl.setHeader("Content-type", "application/json");

        JSONObject data = new JSONObject();
        String meid = getDeviceMEID();
        data.put("deviceid", meid);
        StringEntity se = new StringEntity(data.toString());
        posturl.setEntity(se);
        HttpResponse response = client.execute(posturl);
        //StatusLine statusLine = response.getStatusLine();
        out = new ByteArrayOutputStream();
        response.getEntity().writeTo(out);
        int statusCode = response.getStatusLine().getStatusCode();
        String networkResponseString = out.toString();
        Log.v(TAG, "networkResponseString:"+networkResponseString);
        if(statusCode == STATUS_204NOCONTENT || statusCode == STATUS_201CREATED || statusCode == STATUS_200OK){
          networkResponse = networkResponseString;
        }else{
          if(networkResponse.contains(ERR_DUPLICATE_KEY)){
            networkResponse = networkResponseString;
          }else{
            networkResponse = ERR_GENERIC_ERROR;
          }
        }
        
        Log.d(TAG, "Network response:"+networkResponse);
        if(out != null){
          out.close();
          out = null;
        }
      }catch(IOException e){
        e.printStackTrace();
      }catch(Exception e){
        e.printStackTrace();
      }finally{

      }

      return networkResponse;
    }

    
    // Don't really need this at present, use in case we need it in future
    // to display toasts/message to the user in case of failures
    @Override
    protected void onPostExecute(String result){
        
        Log.v(TAG, "DeviceRegisterTask nw data received"+result);
        if(result.contains(ERR_DUPLICATE_KEY) || result.contains(ERR_GENERIC_ERROR)){
          return;
        }

        synchronized(sNetworkCallLock){
          /*
          if(mDbHelper.hasEntries(packageName)){
            mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.DATA_COL, result);
          }
          */
        mJSONResponse = result;
        sNetworkCallLock.notify();
      }
    }

  }



  /* Test suite */

    private void dbTest(){

    }

    private void networkQueryTest(){

    }

    private void cacheTest(){

    }


}
