package com.istream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class CsvFileWriter {

	private static final Logger logger = Logger.getLogger(CsvFileWriter.class);

	private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("ddMMyyyy-HHmmss");

	private static Map<String,Object> insertionMap = new LinkedHashMap<String,Object>();
	private BufferedWriter bw;
	private static String centralFilePath;

	public CsvFileWriter(final PropertiesConfiguration prop) {
		centralFilePath = prop.getString("centralFilePath");
	}

	public String writeToCSV(final String JsonData, final Long lineProductionId) {
		{
			try {
				insertionMap.put(JsonData,lineProductionId);
				if (insertionMap.size() == 5 || JsonData == null) {
					String dateTime = formatter.print(new LocalDateTime());
					FileWriter fileWriter = new FileWriter(centralFilePath + "/Production_" + dateTime + ".csv");
					bw = new BufferedWriter(fileWriter);
					for (Map.Entry<String, Object> singleRow : insertionMap.entrySet()) {
						bw.write(singleRow.getKey() + "," + singleRow.getValue() + ",\n");
					}
					logger.info("file created Successfully with size:" + insertionMap.size() + "/Production_" + dateTime + ".csv");
					bw.close();
					fileWriter.close();
					insertionMap.clear();
				} else {
					logger.info("file current size :" + insertionMap.size());
				}
				return "success";
			} catch (IOException e) {
				logger.error(e.getMessage());
				return "failure";
			} catch (Exception e) {
				logger.error(e.getMessage());
				return "failure";
			}

		}
	}
}

