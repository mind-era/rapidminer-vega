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
package com.rapidminer.operator.nio.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.operator.nio.model.DataResultSet.ValueType;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.container.Pair;

/**
 * This class encapsulates the translation step from a {@link DataResultSetTranslator} to an
 * {@link ExampleSet} which is controlled by the {@link DataResultSetTranslationConfiguration}.
 * 
 * @author Sebastian Land
 */
public class DataResultSetTranslator {

	private class NominalValueSet {
		private String first = null;
		private String second = null;
		private boolean moreThanTwo = false;

		private boolean register(String value) {
			if (moreThanTwo) {
				return true;
			} else if (value == null) {
				return false;
			} else if (first == null) {
				first = value;
				return false;
			} else if (first.equals(value)) {
				return false;				
			} else if (second == null) {
				second = value;
				return false;
			} else if (second.equals(value)) {
				return false;
			} else {
				moreThanTwo = true;
				return true;
			}
		}
	}

	private boolean shouldStop = false;
	private boolean isReading = false;

	private boolean cancelGuessingRequested = false;
	private boolean cancelLoadingRequested = false;

	private final Map<Pair<Integer,Integer>,ParsingError> errors = new HashMap<Pair<Integer,Integer>, ParsingError>();

	private Operator operator;

	public DataResultSetTranslator(Operator operator) {
		this.operator = operator;
	}

	/**
	 * This method will start the translation of the actual ResultDataSet to an ExampleSet. 
	 */
	public ExampleSet read(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration, boolean previewOnly, ProgressListener listener) throws OperatorException {
		int maxRows = previewOnly ? ImportWizardUtils.getPreviewLength() : -1;
		
		cancelLoadingRequested = false;
		boolean isFaultTolerant = configuration.isFaultTolerant();

		isReading = true;
		int[] attributeColumns = configuration.getSelectedIndices();
		int numberOfAttributes = attributeColumns.length;

		Attribute[] attributes = new Attribute[numberOfAttributes];
		for (int i = 0; i < attributes.length; i++) {
			int attributeValueType = configuration.getColumnMetaData(attributeColumns[i]).getAttributeValueType();
			if (attributeValueType == Ontology.ATTRIBUTE_VALUE)  //fallback for uninitialized reading.
				attributeValueType = Ontology.POLYNOMINAL;
			attributes[i] = AttributeFactory.createAttribute(configuration.getColumnMetaData(attributeColumns[i]).getOriginalAttributeName(), attributeValueType);
		}

		// building example table
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

		// now iterate over complete dataResultSet and copy data
		int currentRow = 0; 		// The row in the underlying DataResultSet
		int exampleIndex = 0;		// The row in the example set
		dataResultSet.reset(listener);
		int maxAnnotatedRow = configuration.getLastAnnotatedRowIndex();
		while (dataResultSet.hasNext() && !shouldStop && (currentRow < maxRows || maxRows < 0)) {
			if (cancelLoadingRequested) {
				break;
			}
			dataResultSet.next(listener);
			// checking for annotation
			String currentAnnotation;
			if (currentRow <= maxAnnotatedRow) {
				currentAnnotation = configuration.getAnnotation(currentRow);
			} else {
				currentAnnotation = null;
			}
			if (currentAnnotation != null) {
				// registering annotation on all attributes
				int attributeIndex = 0;
				for (Attribute attribute : attributes) {
					if (AbstractDataResultSetReader.ANNOTATION_NAME.equals(currentAnnotation)) {
						// resetting name
						String newAttributeName = getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant);
						if (newAttributeName != null && !newAttributeName.isEmpty()) {
							attribute.setName(newAttributeName);
							// We also remember the name in the CMD since we otherwise would override the attribute name later in this method
							ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
							if (cmd != null) {
								if (!cmd.isAttributeNameSpecified()) {
									cmd.setUserDefinedAttributeName(newAttributeName);
								}
							}

						}
					} else {
						// setting annotation
						String annotationValue = getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant);
						if (annotationValue != null && !annotationValue.isEmpty())
							attribute.getAnnotations().put(currentAnnotation, annotationValue);
					}
					attributeIndex++;
				}
			} else {
				// creating data row
				DoubleArrayDataRow row = new DoubleArrayDataRow(new double[attributes.length]);
				exampleTable.addDataRow(row);
				int attributeIndex = 0;
				for (Attribute attribute : attributes) {
					// check for missing
					if (dataResultSet.isMissing(attributeColumns[attributeIndex])) {
						row.set(attribute, Double.NaN);
					} else {
						switch (attribute.getValueType()) {
						case Ontology.INTEGER:
							row.set(attribute, getOrParseNumber(configuration, dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant));
							//getNumber(dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant).intValue());
							break;
						case Ontology.NUMERICAL:
						case Ontology.REAL:
							row.set(attribute, getOrParseNumber(configuration, dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant));
							break;
						case Ontology.DATE_TIME:
						case Ontology.TIME:
						case Ontology.DATE:
							row.set(attribute, getOrParseDate(configuration, dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant));
							break;
						default:
							row.set(attribute, getStringIndex(attribute, dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant));
						}
					}
					attributeIndex++;
				}
				exampleIndex++;
			}
			currentRow++;
		}

		// derive ExampleSet from exampleTable and assigning roles
		ExampleSet exampleSet = exampleTable.createExampleSet();
		Attributes exampleSetAttributes = exampleSet.getAttributes();
		int attributeIndex = 0;
		for (Attribute attribute : attributes) {
			// if user defined names have been found, rename accordingly
			final ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
			String userDefinedName = cmd.getUserDefinedAttributeName();
			if (userDefinedName != null)
				attribute.setName(userDefinedName);
			String roleId = cmd.getRole();
			if (!Attributes.ATTRIBUTE_NAME.equals(roleId))
				exampleSetAttributes.setSpecialAttribute(attribute, roleId);
			attributeIndex++;
		}

		isReading = false;
		if (listener != null)
			listener.complete();
		return exampleSet;
	}

	/** If native type is date, returns the date. Otherwise, uses string and parses.
	 */
	private double getOrParseDate(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return Double.NaN;
		}
		if (nativeValueType == ValueType.DATE) {
			return getDate(dataResultSet, row, column, isFaultTolerant);
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			try {
				return config.getDateFormat().parse(value).getTime();
			} catch (ParseException e) {
				ParsingError error = new ParsingError(dataResultSet.getCurrentRow(), column, ErrorCode.UNPARSEABLE_DATE, value, e);
				addOrThrow(isFaultTolerant, error, row);
				return Double.NaN;
			}
		}
	}

	private double getDate(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws OperatorException {
		try {
			return dataResultSet.getDate(column).getTime();
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return Double.NaN;
		}
	}

	private double getStringIndex(Attribute attribute, DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws UserError {
		String value = null;
		try {			
			value = dataResultSet.getString(column);
			int mapIndex = attribute.getMapping().mapString(value);
			return mapIndex;	
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return Double.NaN;
		} catch (AttributeTypeException e) {
			ParsingError error = new ParsingError(dataResultSet.getCurrentRow(), column, ErrorCode.MORE_THAN_TWO_VALUES, value, e);
			addOrThrow(isFaultTolerant, error, row);
			return Double.NaN;
		}
	}

	private String getString(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws UserError {
		try {
			return dataResultSet.getString(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return null;
		}
	}

	/** If native type is date, returns the date. Otherwise, uses string and parses.
	 */
	private double getOrParseNumber(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return Double.NaN;
		}
		if (nativeValueType == ValueType.NUMBER) {
			return getNumber(dataResultSet, row, column, isFaultTolerant).doubleValue();
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				ParsingError error = new ParsingError(dataResultSet.getCurrentRow(), column, ErrorCode.UNPARSEABLE_REAL, value, e);
				addOrThrow(isFaultTolerant, error, row);
				return Double.NaN;
			}
		}
	}

	private Number getNumber(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws OperatorException {
		try {
			return dataResultSet.getNumber(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			if (isFaultTolerant) {
				addError(e.getError(), row);
				return Double.NaN;
			} else {
				throw new UserError(operator, "data_parsing_error", e.toString());
			}
		}
	}

	public void guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, ProgressListener listener) throws OperatorException {
		int maxProbeRows;
		try {
			maxProbeRows = Integer.parseInt(RapidMiner.getRapidMinerPropertyValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			maxProbeRows = 100;
		}
		guessValueTypes(configuration, dataResultSet, maxProbeRows, listener);
	}

	public void guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, int maxNumberOfRows, ProgressListener listener) throws OperatorException {
		int[] originalValueTypes = new int[configuration.getNumerOfColumns()];
		for (int i = 0; i < originalValueTypes.length; i++) {
			originalValueTypes[i] = configuration.getColumnMetaData(i).getAttributeValueType();
		}
		final int[] guessedTypes = guessValueTypes(originalValueTypes, configuration, dataResultSet, maxNumberOfRows, listener);
		for (int i = 0; i < guessedTypes.length; i++) {
			configuration.getColumnMetaData(i).setAttributeValueType(guessedTypes[i]);
		}
	}

	/**
	 * This method will select the most appropriate value types defined on the first few thousand rows.
	 * 
	 * @throws OperatorException
	 */
	private int[] guessValueTypes(int[] definedTypes, DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, int maxProbeRows, ProgressListener listener) throws OperatorException {
		cancelGuessingRequested = false;

		if (listener != null)
			listener.setTotal(1 + maxProbeRows);
		DateFormat dateFormat = configuration.getDateFormat();

		if (listener != null)
			listener.setCompleted(1);
		int[] columnValueTypes = new int[dataResultSet.getNumberOfColumns()];
		Arrays.fill(columnValueTypes, Ontology.INTEGER);

		// TODO: The following could be made more efficient using an indirect indexing to access the columns: would
		dataResultSet.reset(listener);
		// the row in the underlying DataResultSet
		int currentRow = 0;
		// the example row in the ExampleTable
		int exampleIndex = 0;
		NominalValueSet nominalValues[] = new NominalValueSet[dataResultSet.getNumberOfColumns()];
		for (int i = 0; i < nominalValues.length; i++) {
			nominalValues[i] = new NominalValueSet();
		}
		int maxAnnotatedRow = configuration.getLastAnnotatedRowIndex();		
		while (dataResultSet.hasNext() && (currentRow < maxProbeRows || maxProbeRows <= 0)) {
			if (cancelGuessingRequested) {
				break;
			}
			dataResultSet.next(listener);
			if (listener != null)
				listener.setCompleted(1 + currentRow);

			// skip rows with annotations
			if ((currentRow > maxAnnotatedRow) || (configuration.getAnnotation(currentRow) == null)) {
				int numCols = dataResultSet.getNumberOfColumns();
				// number of columns can change as we read the data set.
				if (numCols >= definedTypes.length) {
					addError(new ParsingError(dataResultSet.getCurrentRow(), 0, ErrorCode.ROW_TOO_LONG, null, null),
							exampleIndex);				
				}
				for (int column = 0; column < definedTypes.length; column++) {
					// No more guessing necessary if guessed type is polynomial (this is the most general case)
					if ((definedTypes[column] == Ontology.POLYNOMINAL) || dataResultSet.isMissing(column)) {
						continue;
					}

					ValueType nativeType;
					String stringRepresentation;
					try {
						nativeType = dataResultSet.getNativeValueType(column);
						stringRepresentation = dataResultSet.getString(column);
					} catch (com.rapidminer.operator.nio.model.ParseException e) {
						final ParsingError error = e.getError();
						addError(error, exampleIndex);
						continue;
					}							
					nominalValues[column].register(stringRepresentation);

					if (nativeType != ValueType.STRING) {
						// Native representation is not a string, so we trust the data source
						// and adapt the type accordingly.
						int isType = nativeType.getRapidMinerAttributeType();
						if (nativeType == ValueType.NUMBER) {
							Number value = getNumber(dataResultSet, exampleIndex, column, true);
							if (!Double.isNaN(value.doubleValue())) {
								if (value.intValue() == value.doubleValue()) {
									isType = Ontology.INTEGER;
								} else {
									isType = Ontology.REAL;
								}
							}
						}
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(isType, definedTypes[column])) {
							// We're good, nothing to do
							if (definedTypes[column] == Ontology.ATTRIBUTE_VALUE) {
								// First row, just use the one delivered
								definedTypes[column] = isType;
							}								
							continue;
						} else {
							// otherwise, generalize until we are good
							while (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(isType, definedTypes[column])) {
								definedTypes[column] = Ontology.ATTRIBUTE_VALUE_TYPE.getParent(definedTypes[column]);
							}
							// in the most general case, we switch to polynomial
							if (definedTypes[column] == Ontology.ATTRIBUTE_VALUE) {
								definedTypes[column] =
									nominalValues[column].moreThanTwo ? Ontology.POLYNOMINAL : Ontology.BINOMINAL;
							}
						}
					} else {
						// for strings, we try parsing ourselves
						// fill value buffer for binominal assessment
						definedTypes[column] = guessValueType(definedTypes[column], stringRepresentation, !nominalValues[column].moreThanTwo, dateFormat);
					}					
				}
				exampleIndex++;
			}
			currentRow++;
		}
		if (listener != null)
			listener.complete();
		return definedTypes;
	}

	/**
	 * This method tries to guess the value type by taking into account the current guessed type and the string value.
	 * The type will be transformed to more general ones.
	 */
	private int guessValueType(int currentValueType, String value, boolean onlyTwoValues, DateFormat dateFormat) {
		if (currentValueType == Ontology.POLYNOMINAL)
			return currentValueType;
		if (currentValueType == Ontology.BINOMINAL) {
			if (onlyTwoValues) {
				return Ontology.BINOMINAL;
			} else {
				return Ontology.POLYNOMINAL;
			}
		}
		if (currentValueType == Ontology.DATE) {
			try {
				dateFormat.parse(value);
				return currentValueType;
			} catch (ParseException e) {
				return guessValueType(Ontology.BINOMINAL, value, onlyTwoValues, dateFormat);
			}
		}
		if (currentValueType == Ontology.REAL) {
			try {
				Double.parseDouble(value);
				return currentValueType;
			} catch (NumberFormatException e) {
				return guessValueType(Ontology.DATE, value, onlyTwoValues, dateFormat);
			}
		}
		try {
			Integer.parseInt(value);
			return Ontology.INTEGER;
		} catch (NumberFormatException e) {
			return guessValueType(Ontology.REAL, value, onlyTwoValues, dateFormat);
		}
	}

	/**
	 * This method will stop any ongoing read action and close the underlying DataResultSet. It will wait until this has
	 * been successfully performed.
	 * 
	 * @throws OperatorException
	 */
	public void close() throws OperatorException {
		if (isReading) {
			shouldStop = true;
			shouldStop = false;
		}
		// TODO: Check: Where do we close now?
		//dataResultSet.close();
	}

	public void clearErrors() {
		errors.clear();
	}

	private void addOrThrow(boolean isFaultTolerant, ParsingError error, int row) throws UserError {
		if (isFaultTolerant) {
			addError(error, row);			
		} else {
			throw new UserError(operator, "data_parsing_error", error.toString());
		}
	}

	private void addError(final ParsingError error, int exampleIndex) {
		error.setExampleIndex(exampleIndex);
		errors.put(new Pair<Integer,Integer>(error.getExampleIndex(), error.getColumn()), error);
	}

	public Collection<ParsingError> getErrors() {
		return errors.values();
	}

	public ParsingError getErrorByExampleIndexAndColumn(int row, int column) {
		if (errors == null) {
			return null;
		}
		return errors.get(new Pair<Integer,Integer>(row, column));
	}

	/** Cancels {@link #guessValueTypes(int[], DataResultSetTranslationConfiguration, DataResultSet, int, ProgressListener)}
	 *  after the next row. */
	public void cancelGuessing() {
		cancelGuessingRequested = true;		
	}
	
	/** Cancels {@link #read(DataResultSet, DataResultSetTranslationConfiguration, int, ProgressListener)} after the 
	 *  next row. */
	public void cancelLoading() {
		cancelLoadingRequested = true;
	}
	
	public boolean isGuessingCancelled() {
		return cancelGuessingRequested;
	}
}