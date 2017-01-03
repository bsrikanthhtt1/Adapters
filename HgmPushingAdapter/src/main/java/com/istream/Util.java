package com.istream;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Util {

	
	private final static Gson gsonConverter = new Gson();
	private final static Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();

	public static String getStringFromJson(String paramName, String json) {

		Map<String, String> requestMap = gsonConverter.fromJson(json, typeOfMap);
		String paramValue = null;
		if (requestMap.containsKey(paramName)) {
			paramValue = (String) requestMap.get(paramName);
		}

		if (paramValue != null) {
			paramValue = paramValue.trim();
		}

		return paramValue;

	}

	/*
	 * public static String getStringFromJsonArray(String paramName, String
	 * json,String plancode) {
	 * 
	 * String paramValue = null;
	 * 
	 * Type collectionType = new TypeToken<List<Test>>() {}.getType();
	 * 
	 * @SuppressWarnings("unchecked") List<Test> lcs = (List<Test>)
	 * gsonConverter.fromJson(json, collectionType);
	 * 
	 * for (Test test : lcs) {
	 * 
	 * if (plancode.trim().equalsIgnoreCase(test.getPlanCode().trim())) {
	 * paramValue = test.getId().toString(); break; } }
	 * 
	 * return paramValue;
	 * 
	 * }
	 */
	
}
