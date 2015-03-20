package android.os;

import android.content.Context;
import android.util.Log;
import android.os.RemoteException;

public final class MaybeManager {
    private static final String TAG = "MaybeManager";

    final IMaybeService mService;
    final Context mContext;
    final Handler mHandler;

    public MaybeManager(Context context, IMaybeService service, Handler handler) {
        mContext = context;
        mService = service;
        mHandler = handler;
    }

    public String getCurrentTime() {
        try {
            return mService.getCurrentTime();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return null;
        }
    }

    public String getAppData(String pkgName) {
        try {
            return mService.getAppData();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return null;
        }
    }
    
    public void requestMaybeUpdates(String pkgName, String url, IMaybeListener listener) {
        try {
            mService.requestMaybeUpdates(url, listener);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }

	public void removeMaybeUpdates(String pkgName, IMaybeListener listener) {
        try {
            mService.removeMaybeUpdates(listener);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
 
    }

	public int registerUrl(String pkgName, String url, String hash) {
        try {
            return mService.registerUrl(url, hash);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return 0;
        }
 
    }

	public int getMaybeAlternative(String pkgName, String label) {
        try {
            return mService.getMaybeAlternative(label);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return 0;
        }
    }

	public void badMaybeAlternative(String pkgName, String label, int value) {
        try {
            mService.badMaybeAlternative(label, value);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }

	public void scoreMaybeAlternative(String pkgName, String label, String jsonString) {
        try {
            mService.scoreMaybeAlternative(label, jsonString);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
 
    }

	public void logMaybeAlternative(String pkgName, String label, String jsonString) {
        try {
            mService.logMaybeAlternative(label, jsonString);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }
}
