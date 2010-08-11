/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * This class handles all existing ResourceConsumptionEstimator values, which are stored in the 
 * "OperatorsResourceConsumption.csv" file. If no value is found for a given Operator, <code>null</code> 
 * will be returned.
 * 
 * @author Marco Boeck
 */
public class OperatorResourceConsumptionHandler {
	
	/** map holding the class name and ResourceConsumptionValues */
	private static Map<String, String[]> resourceMap;
	
	/** name of the csv resource consumption file */
	private static final String OPERATORS_RESOURCE_CONSUMPTION = "OperatorsResourceConsumption.csv";
	
	/** number of values the csv file contains per row */
	private static final int RESOURCE_CONSUMPTION_CSV_SPLITPARTS = 7;
	
	private static final Logger LOGGER = Logger.getLogger(OperatorResourceConsumptionHandler.class.getName());
	
	
	static {
		resourceMap = new HashMap<String, String[]>();
		InputStream is;
		String resource = "/" + Tools.RESOURCE_PREFIX + OPERATORS_RESOURCE_CONSUMPTION;
		try {
			is = OperatorResourceConsumptionHandler.class.getResource(resource).openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String[] splitString;
			String row;
			int i = 0;
			while ((row = reader.readLine()) != null) {
				i++;
				// skip empty rows
				if (row.trim().equals("")) {
					continue;
				}
				// skip comments
				if (row.charAt(0) == '#') {
					continue;
				}
				splitString = row.split(";");
				// malformed csv file
				if (splitString.length != RESOURCE_CONSUMPTION_CSV_SPLITPARTS) {
					LOGGER.warning("Malformed Resource Consumption CSV file (" + OPERATORS_RESOURCE_CONSUMPTION + ") in line " + i + "!");
				}
				resourceMap.put(splitString[0], splitString);
			}
		} catch (FileNotFoundException e) {
			LOGGER.warning(e.getLocalizedMessage());
		} catch (IOException e) {
			LOGGER.warning(e.getLocalizedMessage());
		}
		
	}
	
	/**
	 * Gets an array with the cpu time consumption values.
	 * <br>
	 * [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes.
	 * <br>
	 * Returns <code>null</code> if no values are found in the CSV file.
	 * 
	 * @param className use XYZ.class where XYZ is the operator class
	 * @return an array containg cpu time consumption values. [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes.
	 */
	public static String[] getTimeConsumption(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz must not be null!");
		}
		if (resourceMap.get(clazz.toString()) == null) {
			return null;
		}
		String [] savedString = resourceMap.get(clazz.toString());
		return new String[] {savedString[1], savedString[2], savedString[3]};
	}
	
	/**
	 * Gets an array with the memory consumption values.
	 * <br>
	 * [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes.
	 * <br>
	 * Returns <code>null</code> if no values are found in the CSV file.
	 * 
	 * @param className use XYZ.class where XYZ is the operator class
	 * @return an array containg memory consumption values. [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes.
	 */
	public static String[] getMemoryConsumption(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz must not be null!");
		}
		if (resourceMap.get(clazz.toString()) == null) {
			return null;
		}
		String [] savedString = resourceMap.get(clazz.toString());
		return new String[] {savedString[4], savedString[5], savedString[6]};
	}

}
