package com.htt.central;

import java.io.File;
import java.util.Queue;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class HologramCentralProducer implements Runnable {

	private static final Logger logger = Logger.getLogger(HologramCentralProducer.class);

	private static String filesReadLocation;
	private static Long threadSleepTime;

	private Queue<File> sharedQueue;

	public HologramCentralProducer(Queue<File> sharedQueue, PropertiesConfiguration prop) {

		this.sharedQueue = sharedQueue;
		filesReadLocation = prop.getString("unProcessedFilePath");
		threadSleepTime = prop.getLong("threadSleepPeriod");
		readFilesFromCentralLocation(filesReadLocation);
	}

	// get all the files from a directory and maintain in queue
	public void readFilesFromCentralLocation(final String directoryName) {
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();
		if (fList != null) {
			for (File file : fList) {
				if (file.isFile()) {
					sharedQueue.offer(file);
				}
			}
		} else {
			logger.info("There is no files for processing...");
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
			logger.info("Producer() calling ...");
			synchronized (sharedQueue) {
				if (sharedQueue.isEmpty()) {
					readFilesFromCentralLocation(filesReadLocation);
					sharedQueue.notifyAll();
				} else {
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
