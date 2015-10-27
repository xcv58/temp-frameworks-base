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
import android.content.*;
import android.os.IBinder;
import android.os.Binder;
import android.os.IMaybeService;
import android.os.RemoteException;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;
import java.text.SimpleDateFormat;

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
import org.json.JSONArray;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
// import com.google.android.gms.common.*;
import com.google.gson.Gson;
import retrofit.Retrofit;
import edu.buffalo.cse.maybeclient.MaybeClient;
import edu.buffalo.cse.maybeclient.rest.Device;
import edu.buffalo.cse.maybeclient.rest.PackageChoices;
import edu.buffalo.cse.maybeclient.rest.Choice;
import com.google.gson.Gson;

import android.os.IMaybeListener;
import android.os.MaybeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import edu.buffalo.cse.phonelab.json.StrictJSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Date;

public class MaybeService extends IMaybeService.Stub {
    private static final String TAG = "Maybe-Service-PhoneLab";

    private static final String QUERY_BACKEND_ACTION = "query_backend";
    private static final String FLUSH_ACTION = "flush";
    private static final String LOAD_ACTION = "load";
    private static final String LIFE_CYCLE_ACTION = "life_cycle";
    private static final String PERSONAL_ACTION = "personal_information";

    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";
    private static final String CANCEL = "cancel";
    private static final String MESSAGE = "message";
    private static final String CONTENT = "content";
    private static final String METHOD = "method";

    private static final String LOGTAG = "MaybeServiceLogging";
    private static final String URL = "http://maybe.cse.buffalo.edu/maybe-api-v1/devices";
    private static final String BASE_URL = "http://maybe.cse.buffalo.edu/maybe-api-v1/";
    private static final String SHARED_PREFERENCES_NAME = "maybeServicePreferences";
    private static final String GCM_ID_KEY = "gcm_id";
    private static final String GCM_PROJECT_ID = "0";

    private static final String MDEVICE_FILENAME = "MAYBE_mDevice_file";
    //Server Error Codes
    private static final String ERR_NO_RECORDS_FOUND = "No Record(s) Found";
    private static final String ERR_DUPLICATE_KEY = "E11000 duplicate key error index";
    private static final String ERR_GENERIC_ERROR = "Error";
    private static final String ERR_JSON_ERROR = "JSON parse error";
    private static final int STATUS_200OK = 200;
    private static final int STATUS_201CREATED = 201;
    private static final int STATUS_204NOCONTENT = 204;

    public static final String STRING_TYPE = "type";
    public static final String STRING_NAME = "name";
    public static final String STRING_LABELS = "labels";
    public static final String STRING_LABEL = "label";
    public static final String STRING_SCORE = "score";
    public static final String STRING_CHOICES = "choices";
    public static final String STRING_CHOICE = "choice";
    public static final String STRING_LOG = "log";
    public static final String STRING_BAD = "bad";
    public static final String STRING_LAST_CHOICE = "last_choice";


    private Context mContext;
    private String mJSONResponse;
    private String mJSONDownloadData;
    private boolean mHasResponse = false;
    private final MaybeDatabaseHelper mDbHelper;
    private static Object sNetworkCallLock = new Object();
    private static Object sDownloadLock = new Object();
    private boolean mIsDeviceRegistered = false;

    private static final int DEFAULT_CHOICE = 0;

    private String mDeviceMEID = null;
    private String mHashedMEID = null;
    private String mGCMId = null;
    private SharedPreferences mSharedPrefs;
    private Object mGCMLock = new Object();
    // private long mPollInterval = 3600; //in seconds
    private long mPollInterval = 60; //in seconds

    private Handler mHandler;

    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            queryBackend();
            mHandler.postDelayed(this, mPollInterval * 1000);
        }
    };

    private Device mDevice = null;
    private Gson gson = new Gson();

    public MaybeService(Context context) {
        super();
        (new StrictJSONObject(TAG))
            .put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
            .put(METHOD, "MaybeService")
            .put("context", context.toString())
            .log();
        if (context == null) {
            Log.e(TAG, "ERROR: context is null");
        }
        mContext = context;
        mDbHelper = MaybeDatabaseHelper.getInstance(mContext);
    }

    private boolean hasActiveNetwork() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public void systemRunning() {
        (new StrictJSONObject(TAG))
            .put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
            .put(METHOD, "systemRunning")
            .log();
        // start network sampling ..
        mSharedPrefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        initializeTimerTask();
        load();
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

    private void initializeTimerTask() {
        mHandler = new Handler();
        long first = 15L;
        (new StrictJSONObject(TAG))
            .put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
            .put(METHOD, "initializeTimerTask")
            .put("first", first)
            .put("interval", mPollInterval)
            .log();
        mHandler.postDelayed(queryRunnable, (first * 1000));
    }

    private void queryBackend() {
        StrictJSONObject jsonLog = new StrictJSONObject(TAG)
            .put(StrictJSONObject.KEY_ACTION, QUERY_BACKEND_ACTION)
            .put("start", System.currentTimeMillis());
        if (!hasActiveNetwork()) {
            jsonLog.put(STATUS, CANCEL).log();
            return;
        }
        String deviceMeid = getDeviceMEID();
        Device device = new MaybeClient().getDevice(BASE_URL, deviceMeid);
        String deviceString = gson.toJson(device);
        if (device != null) {
            jsonLog.put(STATUS, "sucess")
                .put("end", System.currentTimeMillis())
                .put(CONTENT, deviceString);
            updateDevice(device, deviceString, jsonLog);
        } else {
            jsonLog.put(STATUS, FAIL)
                .put(MESSAGE, deviceString)
                .log();
        }
        retrivePersonalInfo();
    }

    private void retrivePersonalInfo() {
        ContentResolver contentResolver = mContext.getContentResolver();
        String gender = Settings.Global.getString(contentResolver, Settings.Global.PHONELAB_GENDER);
        String age = Settings.Global.getString(contentResolver, Settings.Global.PHONELAB_AGE);
        String laptop = Settings.Global.getString(contentResolver, Settings.Global.PHONELAB_LAPTOP);
        String desktop = Settings.Global.getString(contentResolver, Settings.Global.PHONELAB_DESKTOP);
        String anotherPhone = Settings.Global.getString(contentResolver, Settings.Global.PHONELAB_ANOTHER_PHONE);
        new StrictJSONObject(TAG).put(StrictJSONObject.KEY_ACTION, PERSONAL_ACTION)
                .put(Settings.Global.PHONELAB_GENDER, gender)
                .put(Settings.Global.PHONELAB_AGE, age)
                .put(Settings.Global.PHONELAB_LAPTOP, laptop)
                .put(Settings.Global.PHONELAB_DESKTOP, desktop)
                .put(Settings.Global.PHONELAB_ANOTHER_PHONE, anotherPhone)
                .log();
    }

    protected synchronized void updateDevice(Device device, String deviceString, StrictJSONObject strictJSONObject) {
        // TODO: detect difference?
        mDevice = device;
        flush(deviceString, strictJSONObject);
    }

    protected synchronized void flush(String deviceString, StrictJSONObject strictJSONObject) {
        File file = new File(Environment.getDataDirectory(), MDEVICE_FILENAME);
        FileOutputStream fos;
        StrictJSONObject localJSONObject = new StrictJSONObject(TAG)
            .put(StrictJSONObject.KEY_ACTION, FLUSH_ACTION);
        try {
            fos = new FileOutputStream(file);
            // fos = mContext.openFileOutput(MDEVICE_FILENAME, Context.MODE_PRIVATE);
            fos.write(deviceString.getBytes());
            fos.close();
            localJSONObject.put(STATUS, SUCCESS)
                .put("file", MDEVICE_FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
            localJSONObject.put(STATUS, SUCCESS)
                .put(STATUS, FAIL)
                .put(MESSAGE, e.getMessage());
        }
        strictJSONObject.put(FLUSH_ACTION, localJSONObject).log();
    }

    protected synchronized void load() {
        if (mDevice == null) {
            File file = new File(Environment.getDataDirectory(), MDEVICE_FILENAME);
            if (!file.exists()) {
                (new StrictJSONObject(TAG))
                    .put(StrictJSONObject.KEY_ACTION, LOAD_ACTION)
                    .put(STATUS, FAIL)
                    .put(MESSAGE, "no file")
                    .log();
                return;
            }
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
            //     fis = mContext.openFileInput(MDEVICE_FILENAME);
            //     // fis.read
                StringBuilder builder = new StringBuilder();
                int ch;
                while((ch = fis.read()) != -1){
                    builder.append((char)ch);
                }
                fis.close();
                String jsonString = builder.toString();
                mDevice = gson.fromJson(jsonString, Device.class);
                (new StrictJSONObject(TAG))
                    .put(StrictJSONObject.KEY_ACTION, LOAD_ACTION)
                    .put(STATUS, SUCCESS)
                    .put("file", MDEVICE_FILENAME)
                    .log();
            } catch (IOException e) {
                e.printStackTrace();
                (new StrictJSONObject(TAG))
                    .put(StrictJSONObject.KEY_ACTION, LOAD_ACTION)
                    .put(STATUS, FAIL)
                    .put(MESSAGE, e.getMessage())
                    .log();
            }
        } else {
            (new StrictJSONObject(TAG))
                .put(StrictJSONObject.KEY_ACTION, LOAD_ACTION)
                .put(STATUS, "skip")
                .log();
        }
    }

    protected synchronized void parseData(String data) {
        if (data == null) {
            Log.i(TAG, "data is null");
            return;
        }
        try {
            //JSONObject jsonData = new JSONObject(data.trim());
            JSONArray jsonArray = MaybeUtils.getJSONArray(data);
            if (jsonArray == null) {
                Log.e(TAG, "Not a valid JSON Array");
                return;
            }
            JSONObject jsonData = jsonArray.getJSONObject(0); //one record for each device

            Log.v(TAG, "JsonData:" + jsonData.toString());
            JSONObject choiceData = jsonData.optJSONObject(STRING_CHOICES);
            Log.v(TAG, "Choices:" + choiceData.toString());
            Iterator<String> appHashKeys = choiceData.keys();
            while (appHashKeys.hasNext()) {
                String packageHash = (String) appHashKeys.next();
                Log.v(TAG, "Current packageHash:" + packageHash);
                JSONObject packageJSONObj = choiceData.optJSONObject(packageHash);
                String packageName = packageJSONObj.optString(STRING_NAME);
                Log.v(TAG, "Current packageName:" + packageName);
                JSONArray jsonArrayData = packageJSONObj.optJSONArray(STRING_LABELS);
      /* insert data into db */
                if (!mDbHelper.hasEntries(packageName)) {
                    mDbHelper.setUrl(packageName, URL);
                    mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.HASH_COL, packageHash);
                }

                mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.DATA_COL, jsonArrayData.toString());
                Log.i(TAG, jsonArrayData.toString());
      /* end insert data into db */
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void logTime(String message, long time1, long time2) {
        long interval = time2 - time1;
        if (interval < 0) {
            interval *= -1;
        }
        JSONObject timeObj = new JSONObject();
        try {
            timeObj.put("interval", interval);
            timeObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOGTAG, timeObj.toString());
    }

    public Date tick() {
        return new Date();
    }

    public void tock(Date time1, JSONObject data) {
        Date time2 = new Date();
        long interval = time2.getTime() - time1.getTime();
        if (interval < 0) {
            interval *= -1;
        }
        JSONObject timeObj = new JSONObject();
        try {
            timeObj.put("interval", interval);
            timeObj.put("message", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOGTAG, timeObj.toString());
    }


    public String getCurrentTime() throws RemoteException {
        Log.d(TAG, "Time" + ":getCurrentTime()");
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

    public int registerUrl(String pkgName, String url, String hash) throws RemoteException {
        //add to database
        //String packageName = getCallerPackageName();
        //temporary hack
        String packageName = pkgName;
        if (mDbHelper.hasEntries(packageName)) {
            String urlInDb = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName);
            if (url.equals(urlInDb)) {
                return 0;
            } else {
                mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.URL_COL, url);
            }
        } else {
            mDbHelper.setUrl(packageName, url);
            if (hash != null) {
                mDbHelper.updateDataInDb(packageName, MaybeDatabaseHelper.HASH_COL, hash);
            }


            synchronized (sNetworkCallLock) {


                new DeviceRegisterTask().execute(url, packageName, null);

                try {
                    sNetworkCallLock.wait();

                    parseData(mJSONResponse);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Received data from TASK: " + mJSONResponse);

            }

        }


        return 1; // Error codes in future?
    }

    public int registerUrl(String url) throws RemoteException {
        //return registerUrl(null, url, null);
        return 0;
    }

    public int deletePackageData() {
        return mDbHelper.deletePackageInfo(getCallerPackageName());
    }


    private String getDeviceMEID() {
        if (mDeviceMEID == null) {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            mDeviceMEID = tm.getDeviceId();
            // Log.d(TAG, "Device MEID:" + mDeviceMEID);
            (new StrictJSONObject(TAG))
                .put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
                .put(METHOD, "getDeviceMEID")
                .put(CONTENT, mDeviceMEID)
                .log();
        }
        return mDeviceMEID;
    }

    /* testing only */
    public String getAppData(String pkgName) throws RemoteException {
        return null;
    }


    public void requestMaybeUpdates(String pkgName, String url, IMaybeListener listener) {

    }

    public void removeMaybeUpdates(String pkgName, IMaybeListener listener) {

    }

    public synchronized int getMaybeAlternative(String pkgName, String label) {
        // Date tick = tick();
        // String packageName = getCallerPackageName();
        // temporary hack
        // JSONObject jsonLog = new JSONObject();

        String packageName = pkgName;

        // Log.v(TAG, "Calling package:" + packageName);
        StrictJSONObject strictJSONObject = new StrictJSONObject(TAG);
        strictJSONObject.put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
            .put(METHOD, "getMaybeAlternative")
            .put("packageName", packageName)
            .put("label", label);

        if (mDevice == null) {
            // Log.d(TAG, "Call get(" + label + ") before service is ready!");
            strictJSONObject.put(STATUS, FAIL)
                .put(MESSAGE, "mDevice is null")
                .log();
            return DEFAULT_CHOICE;
        }

        if (mDevice.choices == null) {
            // Log.d(TAG, "mDevice.choices is null: " + gson.toJson(mDevice));
            strictJSONObject.put(STATUS, FAIL)
                .put(MESSAGE, "mDevice.choices is null")
                .log();
            return DEFAULT_CHOICE;
        }

        PackageChoices choices = mDevice.choices.get(packageName);
        if (choices == null) {
            // Log.d(TAG, "No PackageChoices for package: " + packageName + " from mDevice: " + gson.toJson(mDevice));
            strictJSONObject.put(STATUS, FAIL)
                .put(MESSAGE, "mDevice.choices doesn't have packageName")
                .log();
            return DEFAULT_CHOICE;
        }

        if (choices.labelJSON == null) {
            // Log.d(TAG, "mDevice.choices.labelJSON is null: " + gson.toJson(mDevice));
            strictJSONObject.put(STATUS, FAIL)
                .put(MESSAGE, "labelJSON is null")
                .log();
            return DEFAULT_CHOICE;
        }

        Choice choice = choices.labelJSON.get(label);
        if (choice == null) {
            // Log.d(TAG, "mDevice.choices.labelJSON.label is null: " + gson.toJson(mDevice));
            strictJSONObject.put(STATUS, FAIL)
                .put(MESSAGE, "labelJSON deosn't have label")
                .log();
            return DEFAULT_CHOICE;
        }

        // Log.d(TAG, "get(" + label + ") = " + choice.choice);
        strictJSONObject.put(STATUS, SUCCESS)
            .put("choice", choice.choice)
            .log();

        return choice.choice;

        // String jsonData = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.DATA_COL, packageName);
        //JSONObject jsonObjData = MaybeUtils.getJSONObject(jsonData);

        // if (jsonData == null) {
        //     Log.i(TAG, "jsondata is null");
        //     //TODO: Optimize logging for error scenarios

        //     try {
        //         jsonLog.put("function", "getMaybeAlternative");
        //         jsonLog.put("label", label);
        //         jsonLog.put("choice", "Error");
        //         jsonLog.put("package", pkgName);
        //         jsonLog.put("Error", "DBQUERY:NULL");
        //         tock(tick, jsonLog);
        //     } catch (JSONException e) {
        //         e.printStackTrace();
        //     }

        //     return -1;
        // }

        // String strChoice = MaybeUtils.parseJSONArray(label, jsonData);
        // if (strChoice == null) {
        //     Log.i(TAG, "strchoice is null");
        //     //TODO: Optimize logging for error scenarios
        //     try {
        //         jsonLog.put("function", "getMaybeAlternative");
        //         jsonLog.put("label", label);
        //         jsonLog.put("choice", "Error");
        //         jsonLog.put("package", pkgName);
        //         jsonLog.put("Error", "JSON:NULL");
        //         tock(tick, jsonLog);
        //     } catch (JSONException e) {
        //         e.printStackTrace();
        //     }
        //     return -1;
        // }

        // int choice = Integer.parseInt(strChoice);
        // Log.i(TAG, "choice: " + choice);


        // try {
        //     jsonLog.put("function", "getMaybeAlternative");
        //     jsonLog.put("label", label);
        //     jsonLog.put("choice", choice);
        //     jsonLog.put("package", pkgName);

        // } catch (JSONException e) {
        //     e.printStackTrace();
        // }
        // tock(tick, jsonLog);
        // return choice;

    }

    public void badMaybeAlternative(String pkgName, String label, int value) {
        (new StrictJSONObject(TAG))
            .put(StrictJSONObject.KEY_ACTION, LIFE_CYCLE_ACTION)
            .put(METHOD, "badMaybeAlternative")
            .put("packageName", pkgName)
            .put("label", label)
            .put("value", value)
            .log();
        // Date tick = tick();
        // //String packageName = getCallerPackageName();
        // //temporary hack
        // JSONObject jsonLog = new JSONObject();
        // String packageName = pkgName;
        //TODO: Needs improvement
    /*
    if(!MaybeUtils.isValidJSONString(jsonstring)){
      Log.e(TAG, "(1)Invalid JSON Object passed for scoring");
      return; //fail silently
    }
    */
        // String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName) + "/" + getDeviceMEID();
        // if (url == null) {
        //     Log.e(TAG, "Server URL is null. Server url might not be registered with the service");
        //     try {
        //         jsonLog.put("function", "badMaybeAlternative");
        //         jsonLog.put("label", label);
        //         jsonLog.put("value", value);
        //         jsonLog.put("package", pkgName);
        //         jsonLog.put("Error", "URL:NULL");
        //         tock(tick, jsonLog);
        //     } catch (JSONException e) {
        //         e.printStackTrace();
        //     }
        //     return;
        // }
        // String hash = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.HASH_COL, packageName);
        // JSONObject scoreData = new JSONObject();
        // try {
        //     scoreData.put(STRING_TYPE, STRING_BAD);
        //     scoreData.put(STRING_NAME, packageName);
        //     scoreData.put(label, value);
        // } catch (JSONException e) {
        //     e.printStackTrace();
        // }
        // new ServerUpdaterTask().execute(url, hash, scoreData.toString());


        // try {
        //     jsonLog.put("function", "badMaybeAlternative");
        //     jsonLog.put("label", label);
        //     jsonLog.put("value", value);
        //     jsonLog.put("package", pkgName);
        //     tock(tick, jsonLog);
        // } catch (JSONException e) {
        //     e.printStackTrace();
        // }

        return; //if JSONException fail silently
    }

    /*
      {

      }
    */
    public void scoreMaybeAlternative(String pkgName, String label, String jsonString) {
        Date tick = tick();
        JSONObject jsonLog = new JSONObject();
        //String packageName = getCallerPackageName();
        //temporary hack
        String packageName = pkgName;
        //TODO: Needs improvement
        if (!MaybeUtils.isValidJSONString(jsonString)) {
            Log.e(TAG, "(1)Invalid JSON Object passed for scoring");
            try {
                jsonLog.put("function", "scoreMaybeAlternative");
                jsonLog.put("label", label);
                jsonLog.put("value", jsonString);
                jsonLog.put("package", pkgName);
                jsonLog.put("Error", "JSON:INVALID");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tock(tick, jsonLog);
            return; //fail silently
        }
        String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName) + "/" + getDeviceMEID();
        if (url == null) {
            Log.e(TAG, "Server URL is null. Server url might not be registered with the service");
            try {
                jsonLog.put("function", "scoreMaybeAlternative");
                jsonLog.put("label", label);
                jsonLog.put("value", jsonString);
                jsonLog.put("package", pkgName);
                jsonLog.put("Error", "URL:NULL");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tock(tick, jsonLog);
            return;
        }
        String hash = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.HASH_COL, packageName);
        JSONObject scoreData = new JSONObject();
        try {
            scoreData.put(STRING_TYPE, STRING_SCORE);
            scoreData.put(STRING_NAME, packageName);
            scoreData.put(label, jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new ServerUpdaterTask().execute(url, hash, scoreData.toString());


        try {
            jsonLog.put("function", "scoreMaybeAlternative");
            jsonLog.put("label", label);
            jsonLog.put("value", jsonString);
            jsonLog.put("package", pkgName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tock(tick, jsonLog);
        return; //if JSONException fail silently
    }

    public void logMaybeAlternative(String pkgName, String label, String jsonString) {
        Date tick = tick();
        JSONObject jsonLog = new JSONObject();
        //String packageName = getCallerPackageName();
        //temporary hack
        String packageName = pkgName;

        String lastChoice = null;

        if (!MaybeUtils.isValidJSONString(jsonString)) {
            Log.e(TAG, "(1)Invalid JSON Object passed for scoring");

            try {
                jsonLog.put("function", "logMaybeAlternative");
                jsonLog.put("label", label);
                jsonLog.put("value", jsonString);
                jsonLog.put("package", pkgName);
                jsonLog.put("Error", "JSON:INVALID");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tock(tick, jsonLog);

            return; //fail silently
        }
        String url = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.URL_COL, packageName) + "/" + getDeviceMEID();
        if (url == null) {
            Log.e(TAG, "Server URL is null. Server url might not be registered with the service");

            try {
                jsonLog.put("function", "logMaybeAlternative");
                jsonLog.put("label", label);
                jsonLog.put("value", jsonString);
                jsonLog.put("package", pkgName);
                jsonLog.put("Error", "URL:NULL");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tock(tick, jsonLog);

            return;
        }
        //TODO: How do we ensure data hasn't changed since last call?
        String jsonData = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.DATA_COL, packageName);
        if (jsonData == null) {
            Log.i(TAG, "jsondata is null");

            try {
                jsonLog.put("function", "logMaybeAlternative");
                jsonLog.put("label", label);
                jsonLog.put("value", jsonString);
                jsonLog.put("package", pkgName);
                jsonLog.put("Error", "JSON:NULL");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tock(tick, jsonLog);

            return;
        } else {

            lastChoice = MaybeUtils.parseJSONArray(label, jsonData);
            if (lastChoice == null) {
                Log.i(TAG, "lastChoice is null");
                try {
                    jsonLog.put("function", "logMaybeAlternative");
                    jsonLog.put("label", label);
                    jsonLog.put("value", jsonString);
                    jsonLog.put("package", pkgName);
                    jsonLog.put("Error", "CHOICE:NULL");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                tock(tick, jsonLog);
                return;
            }
        }
        String hash = mDbHelper.getAppDataFromDb(MaybeDatabaseHelper.HASH_COL, packageName);

        JSONObject logData = new JSONObject();
        try {
            logData.put(STRING_TYPE, STRING_LOG);
            logData.put(STRING_NAME, packageName);
            logData.put(STRING_LAST_CHOICE, lastChoice);
            logData.put(label, jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new ServerUpdaterTask().execute(url, hash, logData.toString());


        try {
            jsonLog.put("function", "logMaybeAlternative");
            jsonLog.put("label", label);
            jsonLog.put("value", jsonString);
            jsonLog.put("package", pkgName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tock(tick, jsonLog);
        return;

    }


    private String getCallerPackageName() {
        return mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
    }

  /* HTTP post
  * @deprecated
  */

    class JSONDownloaderTask extends AsyncTask<String, Void, String> {
        HttpClient client;

        public JSONDownloaderTask() {
            client = new DefaultHttpClient();

        }

        @Override
        protected String doInBackground(String... params) {
            String networkResponse = null;
            try {

                HttpGet geturl = new HttpGet(params[0]);
                geturl.setHeader("Content-type", "application/json");

                HttpResponse response = client.execute(geturl);
                int statusCode = response.getStatusLine().getStatusCode();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                networkResponse = out.toString();
                //TODO:
                Log.d(TAG, networkResponse);
                if (networkResponse.contains(ERR_NO_RECORDS_FOUND)) {
                    Log.d(TAG, "No Records found");
                    // Keep the error and parse it at the timer task. Register the device if
                    // this is the case.
                    mIsDeviceRegistered = false;
                    //networkResponse = null;
                } else {
                    Log.d(TAG, "update exists");
                    //TODO: (?)

                }
                out.close();

            } catch (IOException e) {
                Log.v(TAG, "(1)JSON Downloader Task exception: check network connectivity");
                e.printStackTrace();
            } catch (Exception e) {
                Log.v(TAG, "(2)JSON Downloader Task exception: check network connectivity");
                e.printStackTrace();
            }

            return networkResponse;
        }


        @Override
        protected void onPostExecute(String result) {
            synchronized (sDownloadLock) {
                mJSONDownloadData = result;
                sDownloadLock.notify();
            }
        }

    }

    class ServerUpdaterTask extends AsyncTask<String, Void, String> {
        HttpClient client;

        public ServerUpdaterTask() {
            client = new DefaultHttpClient();

        }

        @Override
        protected String doInBackground(String... params) {
            // params[0]- url, params[1] - package name , params[2] - data in JSONString format
            String networkResponse = "";
            ByteArrayOutputStream out = null;
            Log.v(TAG, "url:" + params[0] + " packagename:" + params[1] + " data:" + params[2]);
            try {

                HttpPut putUrl = new HttpPut(params[0]);
                putUrl.setHeader("Content-type", "application/json");
        /*
        JSONObject data = new JSONObject();
        data.put(params[1],params[2]);
        */
                JSONObject data = MaybeUtils.getJSONObject(params[2]);
                JSONObject labelData = new JSONObject();
                labelData.put(params[1], data);
                JSONObject postData = new JSONObject();
                postData.put("$set", labelData);
                StringEntity se = new StringEntity(postData.toString());
                putUrl.setEntity(se);
                HttpResponse response = client.execute(putUrl);
                //StatusLine statusLine = response.getStatusLine();
                out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                networkResponse = out.toString();
                int statusCode = response.getStatusLine().getStatusCode();
                Log.d(TAG, "Network response|status code:" + networkResponse + statusCode);
                if (statusCode == STATUS_204NOCONTENT || statusCode == STATUS_201CREATED || statusCode == STATUS_200OK) {
                    networkResponse = out.toString();
                } else {
                    networkResponse = ERR_GENERIC_ERROR;
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                if (out != null) {
                    try {
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return networkResponse;
        }


        // Don't really need this at present, use in case we need it in future
        // to display toasts/message to the user in case of failures
        @Override
        protected void onPostExecute(String result) {
            String packageName = getCallerPackageName();
            if (result.contains(ERR_DUPLICATE_KEY) || result.contains(ERR_GENERIC_ERROR)) {
                Log.v(TAG, "Error while updating data to server");
                return;
            }

        }

    }

    class DeviceRegisterTask extends AsyncTask<String, Void, String> {
        HttpClient client;

        public DeviceRegisterTask() {
            client = new DefaultHttpClient();

        }

        @Override
        protected String doInBackground(String... params) {
            String networkResponse = null;
            ByteArrayOutputStream out = null;
            try {

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
                Log.v(TAG, "networkResponseString:" + networkResponseString);
                if (statusCode == STATUS_204NOCONTENT || statusCode == STATUS_201CREATED || statusCode == STATUS_200OK) {
                    Log.v(TAG, "Device Register Status code: 2xx ");
                    networkResponse = networkResponseString;
                    mIsDeviceRegistered = true;
                } else {
                    if (networkResponseString.contains(ERR_DUPLICATE_KEY)) {
                        //networkResponse = networkResponseString;
                        mIsDeviceRegistered = true;
                    } else {
                        Log.d(TAG, "Network error:" + networkResponseString);
                        //networkResponse = ERR_GENERIC_ERROR;

                    }
                }

                Log.d(TAG, "Network response:" + networkResponse);
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }

            return networkResponse;
        }


        // Don't really need this at present, use in case we need it in future
        // to display toasts/message to the user in case of failures
        @Override
        protected void onPostExecute(String result) {

            Log.v(TAG, "DeviceRegisterTask nw data received" + result);
        /*
        if(result.contains(ERR_DUPLICATE_KEY) || result.contains(ERR_GENERIC_ERROR)){
          return;
        }
        */

            synchronized (sNetworkCallLock) {
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

    private void dbTest() {

    }

    private void networkQueryTest() {

    }

    private void cacheTest() {

    }


}
