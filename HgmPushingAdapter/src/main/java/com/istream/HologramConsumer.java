package com.istream;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import au.com.bytecode.opencsv.CSVReader;
import net.sf.json.JSONObject;

public class HologramConsumer implements Runnable {
	
	private static final Logger logger = Logger.getLogger(HologramConsumer.class);
	
	public static JSONObject  lineHgJson = new JSONObject();
	Timer timer = new Timer(); // Instantiate Timer Object
	ScheduledTask st = new ScheduledTask(); // Instantiate SchedulerTask Object
	
	private PropertiesConfiguration properties;
	
	private static CSVReader csvReader;
	private static Long threadSleepTime;
	private static String processedFilePath;
	private static String unProcessedFilePath;
	private static String tenantIdentifier;
	private static String encodePassword;
	private static String baseUrl;
	private static String authenticationUrl;
	private static String refreshUrl;
	private static HttpPost post;
	
	
	private Queue<File> sharedQueue;
	private HttpClient postClient;
	private CsvFileWriter csvFileWriter;
	
	private Long expiresIn;
	private String accessToken;
	private String refreshToken;
	private String tokenType;

	private int  lineIdle;
	private static int counter;


	public HologramConsumer(Queue<File> queue, PropertiesConfiguration prop,HttpClient httpClient) {
		this.sharedQueue =queue;
		properties = prop;
		postClient = httpClient;
		threadSleepTime = prop.getLong("threadSleepPeriod");
		processedFilePath = prop.getString("processedFilePath");
		unProcessedFilePath = prop.getString("unProcessedFilePath");
		tenantIdentifier = prop.getString("tenantIdentfier");
		String credentials = prop.getString("username")+":"+prop.getString("password");
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		authenticationUrl = prop.getString("authenticationApi").trim();
		refreshUrl = prop.getString("refreshApi").trim();
		baseUrl = prop.getString("lineDetailApi").trim();
		post = setAutherizationAndRequiredPostUrl(prop);
		csvFileWriter = new CsvFileWriter(prop);
		
	}
	
	private HttpPost setAutherizationAndRequiredPostUrl(final PropertiesConfiguration prop) {
		
		HttpPost postRequest=new HttpPost(baseUrl);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
		if("oauth".equalsIgnoreCase(prop.getString("security"))){
			try {
				HttpClient authenticationClient = HgmPushingMainAdapter.wrapClient(new DefaultHttpClient());
				HttpPost postAuthentication=new HttpPost(authenticationUrl);
				postAuthentication.addHeader("Content-Type", "application/json");
				postAuthentication.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
				HttpResponse response = authenticationClient.execute(postAuthentication);
				org.json.JSONObject authentication = readResponseContent(response);
				logger.info(authentication);
				expiresIn=authentication.getLong("expires_in");
				accessToken=authentication.getString("access_token");
				refreshToken=authentication.getString("refresh_token");
				tokenType=authentication.getString("token_type");
				timer.schedule(st, expiresIn*1000, expiresIn*1000); // Create Repetitively task for every expiresIn seconds
				postRequest.addHeader("Authorization", tokenType +' '+ accessToken);
				authenticationClient.getConnectionManager().shutdown();
			} catch (ClientProtocolException e) {
				logger.error(e.getMessage());
			} catch (IOException | JSONException e) {
				logger.error(e.getMessage());
			} 
		}else{
			postRequest.addHeader("Authorization", "Basic " + encodePassword);
		}
		return postRequest;
	}
	
	class ScheduledTask extends TimerTask {
		
		public void run() {
			try {
				logger.info("****** Timer Calling *******");
				refreshUrl=properties.getString("refreshApi").trim()+"&refresh_token="+refreshToken;
				logger.info(refreshUrl);
				HttpClient refreshClient = HgmPushingMainAdapter.wrapClient(new DefaultHttpClient());
				HttpPost postAuthentication = new HttpPost(refreshUrl);
				postAuthentication.addHeader("Content-Type", "application/json");
				postAuthentication.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
				HttpResponse response = refreshClient.execute(postAuthentication);
				org.json.JSONObject authentication = readResponseContent(response);
				logger.info("Refresh Authentication :" + authentication);
				expiresIn = authentication.getLong("expires_in");
				accessToken = authentication.getString("access_token");
				refreshToken = authentication.getString("refresh_token");
				tokenType = authentication.getString("token_type");
				Thread.sleep(threadSleepTime);
				HttpPost postRequest=new HttpPost(baseUrl);
				postRequest.addHeader("Content-Type", "application/json");
				postRequest.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
				postRequest.addHeader("Authorization", tokenType +' '+ accessToken);
				post = postRequest;
				refreshClient.getConnectionManager().shutdown();
			} catch (ClientProtocolException e) {
				logger.error(e.getMessage());
			} catch (IOException | JSONException e) {
				logger.error(e.getMessage());
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
	}
	
	@Override
	public void run() {

		while (true) {
			logger.info("Consumer() calling ...");
			try {
				synchronized (sharedQueue) {
					consume();
					Thread.sleep(threadSleepTime);
				}
			} catch (InterruptedException ex) {
				logger.error("thread is Interrupted for the : " + ex.getCause().getLocalizedMessage());
			}
		}
	}

	private void consume() {
		
		try {
			if(sharedQueue.isEmpty()){
				counter = counter+2;
				logger.info("Idle count:" +counter);
				if(counter == 150){
					csvFileWriter.writeToCSV(null,3L);
					lineIdle = counter+lineIdle;
					counter =0;
				}
			}
				while (!sharedQueue.isEmpty()) {
					counter =0;
					for (File processFile : sharedQueue) {
						logger.info(processFile.getName());
						sharedQueue.poll();
						processRequest(processFile);
					}
					sharedQueue.notifyAll();
				}
			
		} catch (Exception e) {
			logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
		}

	}
	
	private void processRequest(final File processFile) {

		StringBuilder hologramsData = new StringBuilder();
		String barcodeNumber = null; 
		Long lineNumber = null;
		String[] currentLineData;
		int i = 0;
		try {
			csvReader = new CSVReader(new FileReader(processFile));

			while ((currentLineData = csvReader.readNext()) != null) {
				if (i == 0) {
					lineNumber = Long.valueOf(currentLineData[0]);
					logger.info("Line Number -- " +lineNumber);
				} else if (i == 1) {
					barcodeNumber = currentLineData[0];
					logger.info("BarCode Number -- "+barcodeNumber);
				} else {
					hologramsData.append(currentLineData[0] + ",");
				}
				i++;
			}
			hologramsData.deleteCharAt(hologramsData.length() - 1);
			lineHgJson.put("hologramNumber", hologramsData.toString());
			lineHgJson.put("barcodeNumber", barcodeNumber);
			lineHgJson.put("status", 0);
			csvReader.close();
			logger.info(lineHgJson);
			if("oauth".equalsIgnoreCase(properties.getString("security"))){
			   post.setHeader("Authorization", tokenType +' '+ accessToken);
			}
			post.setURI(new URI(baseUrl+ "/" + lineNumber));
			post.setEntity(new StringEntity(lineHgJson.toString()));
			logger.info(post.getURI());
			HttpResponse response =postClient.execute(post);
		     org.json.JSONObject resultJson = readResponseContent(response);
			if (response.getStatusLine().getStatusCode() != 200) {
				logger.error("HTTP error code : " + response.getStatusLine().getStatusCode() + "Error Message : "+resultJson);
				throw new Exception(String.valueOf(response.getStatusLine().getStatusCode()));
		     } else {
		    	if(resultJson.has("changes")){
		    		processFile.renameTo(new File(unProcessedFilePath+"/"+processFile.getName())); 
		    		logger.error(resultJson.getJSONObject("changes").getString("pReturnMessage"));
		    	}else{
		    		logger.info(resultJson);
		    		csvFileWriter.writeToCSV(lineHgJson.toString(),resultJson.getLong("resourceId"));
		    		processFile.renameTo(new File(processedFilePath+"/"+processFile.getName()));
					logger.info("ALL Records Inserted Successfully And File Moved -- "+processFile.getName() +"-- " + sharedQueue.size());
		    	}
		     }
			lineHgJson.clear();
		} catch (IOException  e) {
			logger.error(e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		} 
	}
	
	public org.json.JSONObject readResponseContent(HttpResponse response) throws IOException, JSONException {
			return new org.json.JSONObject(EntityUtils.toString(response.getEntity()));
    }

}