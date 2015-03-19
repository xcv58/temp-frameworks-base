package com.android.server;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public final class MaybeUtils{

	public static String getHashValue(String data){
		
	return null;
	}

	public static JSONObject getJSONObject(String key, String data){
		try{
			JSONObject jsonObject = new JSONObject();
			return jsonObject.put(key, data);
		}catch(JSONException e){
			e.printStackTrace();
		}
		return null;

	}

	public static JSONObject getJSONObject(String data){
		if(!isValidJSONString(data)){
			return null;
		}
		try{
			return new JSONObject(data);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public static JSONObject getJSONObject(String key, JSONObject jsonObj){
		try{
			JSONObject jsonObject = new JSONObject();
			return jsonObject.put(key, jsonObj);

		}catch(JSONException e){
			e.printStackTrace();
		}
		return null;
	}

	public static int getChoiceForLabel(JSONObject jsonObj){
		return 0;
	}

	/*
	* NOTE:TODO:JSONArray currently not supported at service
	*/
	public static boolean isValidJSONString(String jsonString){
		try {
        	new JSONObject(jsonString);
	    } catch (JSONException e) {
	        
	        try {
	            new JSONArray(jsonString);
	        } catch (JSONException e1) {
	            return false;
	        }
	    }
	    return true;
	}

	public static String getMessageDigest(){
		//return sha 224 Message Digest
		return null;
	}

	public static String parseJSONString(String key, String jsonString){
		if(!isValidJSONString(jsonString)){
			return null;
		}
		JSONObject jsonObj = getJSONObject(jsonString);
		if(jsonObj == null){
			return null;
		}
		
		if(!jsonObj.has(key)){
			return null;
		}
		return jsonObj.optString(key);
		
		
		
	}
}