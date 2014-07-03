package edu.buffalo.cse.phonelab;

import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;


/** 
 * PhoneLab logger.
 *
 * @hide
 */
public class Logger extends JSONObject {
    public static final String LOG_FORMAT = "1.0";

    public String tag;

    public Logger(String tag) {
        this.tag = tag;
    }

    public Logger put(String name, int value) {
        try {
            super.put(name, value);
        }
        catch (JSONException e) {
        }
        return this;
    }

    public Logger put(String name, long value) {
        try {
            super.put(name, value);
        }
        catch (JSONException e) {
        }
        return this;
    }

    public Logger put(String name, Object value) {
        try {
            super.put(name, value);
        }
        catch (JSONException e) {
        }
        return this;
    }

    public Logger put(String name, boolean value) {
        try {
            super.put(name, value);
        }
        catch (JSONException e) {
        }
        return this;
    }

    public Logger put(String name, double value) {
        try {
            super.put(name, value);
        }
        catch (JSONException e) {
        }
        return this;
    }

    public void log() {
        this.put("LogFormat", LOG_FORMAT);
        Log.i(tag, toString());
    }
}
