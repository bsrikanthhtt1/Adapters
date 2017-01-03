package com.htt.central;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
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

public class HologramCentralConsumer implements Runnable {

	private static final Logger logger = Logger.getLogger(HologramCentralConsumer.class);

	Timer timer = new Timer(); // Instantiate Timer Object
	ScheduledTask st = new ScheduledTask(); // Instantiate SchedulerTask Object

	private PropertiesConfiguration properties;

	private static CSVReader csvReader;
	private static Long threadSleepTime;
	private static String processedFilePath;
	private static String tenantIdentifier;
	private static String encodePassword;
	private static String baseUrl;
	private static String authenticationUrl;
	private static String refreshUrl;
	private static HttpPost post;

	private Queue<File> sharedQueue;
	private HttpClient postClient;

	private Long expiresIn;
	private String accessToken;
	private String refreshToken;
	private String tokenType;

	static Socket requestSocket = null;
	String message;
	private String result;


	public HologramCentralConsumer(Queue<File> queue, PropertiesConfiguration prop, HttpClient httpClient) {

		this.sharedQueue = queue;
		properties = prop;
		postClient = httpClient;
		threadSleepTime = prop.getLong("threadSleepPeriod");
		processedFilePath = prop.getString("processedFilePath");
		tenantIdentifier = prop.getString("tenantIdentfier");
		String credentials = prop.getString("username") + ":" + prop.getString("password");
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		authenticationUrl = prop.getString("authenticationApi").trim();
		refreshUrl = prop.getString("refreshApi").trim();
		baseUrl = prop.getString("lineDetailApi").trim();
		post = setAutherizationAndRequiredPostUrl(prop);

	}

	private HttpPost setAutherizationAndRequiredPostUrl(final PropertiesConfiguration prop) {

		HttpPost postRequest = new HttpPost(baseUrl);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
		if ("oauth".equalsIgnoreCase(prop.getString("security"))) {
			try {
				HttpClient authenticationClient = HgmCentralPushingMainAdapter.wrapClient(new DefaultHttpClient());
				HttpPost postAuthentication = new HttpPost(authenticationUrl);
				postAuthentication.addHeader("Content-Type", "application/json");
				postAuthentication.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
				HttpResponse response = authenticationClient.execute(postAuthentication);
				org.json.JSONObject authentication = readResponseContent(response);
				logger.info(authentication);
				expiresIn = authentication.getLong("expires_in");
				accessToken = authentication.getString("access_token");
				refreshToken = authentication.getString("refresh_token");
				tokenType = authentication.getString("token_type");
				timer.schedule(st, expiresIn * 1000, expiresIn * 1000); // Create Repetitively task for every expiresIn seconds
				postRequest.addHeader("Authorization", tokenType + ' ' + accessToken);
				authenticationClient.getConnectionManager().shutdown();
			} catch (ClientProtocolException e) {
				logger.error(e.getMessage());
			} catch (IOException | JSONException e) {
				logger.error(e.getMessage());
			}
		} else {
			postRequest.addHeader("Authorization", "Basic " + encodePassword);
		}
		return postRequest;
	}

	class ScheduledTask extends TimerTask {

		public void run() {
			try {
				logger.info("****** Timer Calling *******");
				refreshUrl = properties.getString("refreshApi").trim() + "&refresh_token=" + refreshToken;
				logger.info(refreshUrl);
				HttpClient refreshClient = HgmCentralPushingMainAdapter.wrapClient(new DefaultHttpClient());
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
				HttpPost postRequest = new HttpPost(baseUrl);
				postRequest.addHeader("Content-Type", "application/json");
				postRequest.addHeader("Hologram-TrackTrace-TenantId", tenantIdentifier);
				postRequest.addHeader("Authorization", tokenType + ' ' + accessToken);
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
			logger.info("Consumer() class calling ...");
			try {
				consume();
				Thread.sleep(threadSleepTime);
			} catch (InterruptedException ex) {
				logger.error("thread is Interrupted for the : " + ex.getCause().getLocalizedMessage());
			} catch (Exception e) {
				logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	private void consume() {
		
		try {
			while (!sharedQueue.isEmpty()) {
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
	
	/**
	 * 
	 * for Processing scan file and rename
	 * 
	 */
	private void processRequest(final File processFile) {
		try {
			StringBuilder sb = new StringBuilder();
			csvReader = new CSVReader(new FileReader(processFile));
			String[] currentLineData;
			while ((currentLineData = csvReader.readNext()) != null) {
				sb.append(currentLineData[0]);
				String string = sb.substring(1, sb.length()).toString();
				if("oauth".equalsIgnoreCase(properties.getString("security"))){
					   post.setHeader("Authorization", tokenType +' '+ accessToken);
				}
				post.setURI(new URI(baseUrl + "/" + currentLineData[3]));
				post.setEntity(new StringEntity("{\"" + string + "\",\"" + currentLineData[1] + "\",\"" + currentLineData[2]));

				HttpResponse response = postClient.execute(post);
				result = EntityUtils.toString(response.getEntity());
				org.json.JSONObject obj = new org.json.JSONObject(result);
				if (response.getStatusLine().getStatusCode() != 200) {
					logger.error("HTTP error code : " + response.getStatusLine().getStatusCode() + "Error Message :");
					throw new Exception(String.valueOf(response.getStatusLine().getStatusCode()));
				} else if(response.getStatusLine().getStatusCode() != 401){
					logger.info("RESPONSE : "+result);
					String returnMessage = null;
					if(obj.has("changes")){returnMessage = obj.getJSONObject("changes").getString("pReturnMessage");
					logger.info(returnMessage.length());}
					if (returnMessage == null || returnMessage.isEmpty()) {
						logger.info("##################### record processed:###################  ");
						logger.info(" Hologram data inserted for " + currentLineData[1]);
					} else {
						logger.info(returnMessage);
					}
				}else if(response.getStatusLine().getStatusCode() == 401){
					logger.error("Tocken failed ");
				}
				sb.setLength(0);
			}
			csvReader.close();
			File newFile = new File(processedFilePath+"/"+processFile.getName());
			if (processFile.renameTo(newFile)) {
				logger.info("ALL Record Inserted Successfully And File name changed to :" + newFile);
			} else {
				logger.info("record inserted but file renamed fail but records processed");
			}

		} catch (IOException | URISyntaxException e) {
			logger.error(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException Ae) {
		
				File newFile = new File(processedFilePath+"/"+processFile.getName());
				if (processFile.renameTo(newFile)) {
					logger.info("ALL Record Inserted Successfully And File name changed to :" + newFile);
				} else {
					logger.info("record inserted but file renamed fail but records processed");
				}
			logger.error(Ae.getLocalizedMessage());
			Ae.printStackTrace();
		} catch (JSONException je) {
			logger.error(je.getLocalizedMessage());
			je.printStackTrace();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public org.json.JSONObject readResponseContent(HttpResponse response) throws IOException, JSONException {
		return new org.json.JSONObject(EntityUtils.toString(response.getEntity()));
    }

}
