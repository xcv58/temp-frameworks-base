package android.os;

import android.content.Context;
import android.util.Log;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.IMaybeService;

public final class MaybeManager {
    private static final String TAG = "MaybeManager";

    private IMaybeService mService;
    private Context mContext;
    private Handler mHandler;

    public MaybeManager(Context context, IMaybeService service, Handler handler) {
        mContext = context;
        mService = service;
        mHandler = handler;
    }

    private void checkService() {
        if (mService == null) {
            IBinder b = ServiceManager.getService(Context.MAYBE_SERVICE);
            mService = IMaybeService.Stub.asInterface(b);
        }
    }

    public String getCurrentTime() {
        checkService();
        try {
            return mService.getCurrentTime();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return null;
        }
    }

    public String getAppData(String pkgName) {
        checkService();
        try {
            return mService.getAppData(pkgName);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return null;
        }
    }
    
    public void requestMaybeUpdates(String pkgName, String url, IMaybeListener listener) {
        checkService();
        try {
            mService.requestMaybeUpdates(pkgName, url, listener);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }

	public void removeMaybeUpdates(String pkgName, IMaybeListener listener) {
        checkService();
        try {
            mService.removeMaybeUpdates(pkgName, listener);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
 
    }

	public int registerUrl(String pkgName, String url, String hash) {
        checkService();
        try {
            return mService.registerUrl(pkgName, url, hash);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return 0;
        }
 
    }

	public int getMaybeAlternative(String pkgName, String label) {
        checkService();
        try {
            return mService.getMaybeAlternative(pkgName, label);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
            return 0;
        }
    }

	public void badMaybeAlternative(String pkgName, String label, int value) {
        checkService();
        try {
            mService.badMaybeAlternative(pkgName, label, value);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }

	public void scoreMaybeAlternative(String pkgName, String label, String jsonString) {
        checkService();
        try {
            mService.scoreMaybeAlternative(pkgName, label, jsonString);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
 
    }

	public void logMaybeAlternative(String pkgName, String label, String jsonString) {
        checkService();
        try {
            mService.logMaybeAlternative(pkgName, label, jsonString);
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed", e);
        }
    }
}
