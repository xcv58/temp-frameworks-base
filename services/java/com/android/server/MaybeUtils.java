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
		//TODO:
		//return sha 224 Message Digest
		return null;
	}

	public static JSONArray getJSONArray(String jsonString){
		if(!isValidJSONString(jsonString)){
			return null;
		}
		try{
			return new JSONArray(jsonString);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;

	}

	public static String parseJSONArray(String key, String jsonString){
		
		JSONArray jsonArr = getJSONArray(jsonString);
		if(jsonArr == null){
			return null;
		}
		try{
			int length = jsonArr.length();
			
			for(int i = 0; i < length; i++){
				JSONObject jsonObj = jsonArr.getJSONObject(i);
				if(jsonObj.has(MaybeService.STRING_LABEL)){
					if(jsonObj.optString(MaybeService.STRING_LABEL).equals(key)){
						return jsonObj.optString(MaybeService.STRING_CHOICE);
					}
				}
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		return null;
		
		
		
	}
}