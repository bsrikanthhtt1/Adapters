package com.istream;

import java.io.File;
import java.util.Arrays;
import java.util.Queue;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class HologramProducerOne implements Runnable {

	private static final Logger logger = Logger.getLogger(HologramProducerOne.class);

	private static String scannerFilePath;
	private static String workingFilePath;
	private static Long threadSleepTime;

	private Queue<File> sharedQueue;

	public HologramProducerOne(Queue<File> sharedQueue, PropertiesConfiguration prop) {

		this.sharedQueue = sharedQueue;
		scannerFilePath = prop.getString("scannerFilePath");
		workingFilePath = prop.getString("workingFilePath");
		threadSleepTime = prop.getLong("threadSleepPeriod");
		moveFilesFromScannerToPu(scannerFilePath);
	}

	// get all the files from a directory
	public void moveFilesFromScannerToPu(final String directoryName) {
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();
		Arrays.sort(fList);
		for (File file : fList) {
			if (file.isFile()) {
				file.renameTo(new File(workingFilePath+"/"+file.getName()));
				logger.info("File moved successfully --" +file.getName() );
				//sharedQueue.offer(file);
				//logger.info(sharedQueue.size());
			} else if (file.isDirectory()) {
				moveFilesFromScannerToPu(file.getAbsolutePath());
			}
				 
		}
	}
	
	@Override
	public void run() {
		while (true) {
			produce();	   
			try {
				Thread.sleep(threadSleepTime);
			} catch (InterruptedException ex) {
				logger.error("Thread is interrupted for the : " + ex.getCause().getLocalizedMessage());
			}
		}
	}

	private void produce() {
		try {
			logger.info("ProduceOne() calling ...");
		    synchronized (sharedQueue) {
			   if (sharedQueue.isEmpty()) {
				   moveFilesFromScannerToPu(scannerFilePath);
				  sharedQueue.notifyAll();
			   
				}else{
					logger.info("Files are processing .... ");
					sharedQueue.notifyAll();
					Thread.sleep(threadSleepTime);
				}
			 } 

		} catch (InterruptedException e) {
			logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
		}

	}

}
