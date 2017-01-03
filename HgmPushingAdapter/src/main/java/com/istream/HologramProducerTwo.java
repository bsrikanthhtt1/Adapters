package com.istream;

import java.io.File;
import java.util.Queue;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class HologramProducerTwo implements Runnable {

	private static final Logger logger = Logger.getLogger(HologramProducerTwo.class);

	private static String filesReadPath;
	private static Long threadSleepTime;

	private Queue<File> sharedQueue;

	public HologramProducerTwo(Queue<File> sharedQueue, PropertiesConfiguration prop) {

		this.sharedQueue = sharedQueue;
		filesReadPath = prop.getString("workingFilePath");
		threadSleepTime = prop.getLong("threadSleepPeriod");
		readFilesFromFolder(filesReadPath);
	}

	// get all the files from a directory and maintain in queue
	public void readFilesFromFolder(final String directoryName) {
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				sharedQueue.offer(file);
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
			logger.info("ProduceTwo() calling ...");
		    synchronized (sharedQueue) {
			   if (sharedQueue.isEmpty()) {
				  readFilesFromFolder(filesReadPath);
				  sharedQueue.notifyAll();
			   
				}else{
					logger.info("records are Processing .... ");
					sharedQueue.notifyAll();
					Thread.sleep(threadSleepTime);
				}
			 } 

		} catch (InterruptedException e) {
			logger.error("Thread is interrupted for the : " + e.getCause().getLocalizedMessage());
		}

	}
}
