package edu.buffalo.cse.phonelab.json;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Strict JSON array.
 *
 * To be used together with StrictJSONObject.
 *
 * @hide
 */
public class StrictJSONArray {
    public static final String NONE = "<none>";

    private JSONArray array;

    public StrictJSONArray() {
        array = new JSONArray();
    }

    public StrictJSONArray put(Boolean value) {
        array.put(value == null? NONE: value);
        return this;
    }

    public StrictJSONArray put(Integer value) {
        array.put(value == null? NONE: value);
        return this;
    }

    public StrictJSONArray put(Long value) {
        array.put(value == null? NONE: value);
        return this;
    }

    public StrictJSONArray put(Double value) {
        array.put(value == null? NONE: value);
        return this;
    }

    public StrictJSONArray put(String value) {
        array.put(value == null? NONE: value);
        return this;
    }

    public StrictJSONArray put(StrictJSONObject value) {
        if (value != null) {
            array.put(value.toJSONObject());
        }
        return this;
    }

    public StrictJSONArray put(StrictJSONArray value) {
        if (value != null) {
            array.put(value.toJSONArray());
        }
        return this;
    }

    public <T extends JSONable> StrictJSONArray put(T value) {
        if (value != null) {
            this.put(value.toJSONObject());
        }
        return this;
    }

    public JSONArray toJSONArray() {
        try {
            return new JSONArray(array.toString());
        }
        catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
