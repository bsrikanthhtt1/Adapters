package com.htt.central;

import java.io.File;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;


public class HgmCentralPushingMainAdapter {

	private static final Logger logger = Logger.getLogger(HgmCentralPushingMainAdapter.class);
	
	private static final String EMAIL_HEADER = "Dear Customer <br/><br/>";
	private static final String EMAIL_FOOTER = "Thank You. <br/>";
	
	private static PropertiesConfiguration prop;
	
	private static HttpClient httpClient = new DefaultHttpClient();

	private static String mailId;
	private static Properties properties;
	private static String password;

	private static String setContentString;
	private static String receiptEmailId;
	private static String receiptEmail2;

	public static void main(String[] args) {

		try {
			Queue<File> sharedQueue = new ConcurrentLinkedQueue<File>();
			prop = new PropertiesConfiguration("HologramCentralIntegrator.ini");
			logger.info("Adapter Started...");
			
			final String logPath = prop.getString("LogFilePath");
			
			/**
			 * creates the log file/directory if not exist
			 */
			File filelocation = new File(logPath);
			if (!filelocation.isDirectory()) {
				filelocation.mkdirs();
			}

			Logger logger = Logger.getRootLogger();
			FileAppender appender = (FileAppender) logger.getAppender("fileAppender");
			appender.setFile(logPath+"/hologramCentralIntegrator.log");
			appender.activateOptions();
			httpClient = wrapClient(httpClient);
		
			//InitializeEmailData();
			HologramCentralProducer p = new HologramCentralProducer(sharedQueue, prop);
			HologramCentralConsumer c = new HologramCentralConsumer(sharedQueue,prop,httpClient);
			
			Thread t1 = new Thread(p);
			Thread t2 = new Thread(c);
			
			t1.start();
			t2.start();
			
		}catch (ConfigurationException e) {
			logger.error("Configuration Properties file loading error.... : " + e.getMessage());
		}catch (Exception e) {
			logger.error("Exception : " + e.getMessage());
		}
	}
	
	public static HttpClient wrapClient(HttpClient base) {

		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				@SuppressWarnings("unused")
				public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}

				@SuppressWarnings("unused")
				public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}

				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {

				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static void sendToUserEmail(String subject, String body) {

		try {
			Session session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailId, password);
				}
			});

			// 2) compose message
			MimeMessage message = new MimeMessage(session);
			// message.setFrom(new
			// InternetAddress(emailDetail.getMessageFrom()));

			message.setFrom(new InternetAddress(mailId));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiptEmailId));
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(receiptEmail2));
			message.setSubject(subject);

			StringBuilder messageBuilder = new StringBuilder().append(EMAIL_HEADER).append(body).append(EMAIL_FOOTER);
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(messageBuilder.toString(), setContentString);

			// 5) create Multipart object and add MimeBodyPart objects to this
			// object
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// 6) set the multiplart object to the message object
			message.setContent(multipart);

			// 7) send message
			Transport.send(message);
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
