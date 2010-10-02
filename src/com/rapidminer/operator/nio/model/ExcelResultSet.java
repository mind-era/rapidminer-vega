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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

/**
 * A DataResultSet for an Excel File.
 * 
 * @author Sebastian Land
 * 
 */
public class ExcelResultSet implements DataResultSet {

	private Sheet sheet = null;

	private boolean[] emptyRows;
	private boolean[] emptyColumns;

	private int rowOffset = 0;
	private int columnOffset = 0;

	private int totalNumberOfRows = 0;
	private int totalNumberOfColumns = 0;

	private int currentRow;
	private Cell[] currentRowCells;

	private Workbook workbook;

	private ExcelResultSetConfiguration configuration;

	private String[] attributeNames;

	/**
	 * The constructor to build an ExcelResultSet from the given configuration.
	 * The calling operator might be null. It is only needed for error handling.
	 */
	public ExcelResultSet(Operator callingOperator, ExcelResultSetConfiguration configuration) throws OperatorException {
		this.configuration = configuration;

		// reading configuration
		columnOffset = configuration.getColumnOffset();
		rowOffset = configuration.getRowOffset();
		currentRow = configuration.getRowOffset() - 1;

		// load the excelWorkbook if it is not set
		if (configuration.hasWorkbook()) {
			workbook = configuration.getWorkbook();
		} else {
			try {
				workbook = Workbook.getWorkbook(configuration.getFile());
			} catch (IOException e) {
				throw new UserError(callingOperator, 302, configuration.getFile().getPath(), e.getMessage());
			} catch (BiffException e) {
				throw new UserError(callingOperator, 302, configuration.getFile().getPath(), e.getMessage());
			}
		}
		try {
			sheet = workbook.getSheet(configuration.getSheet() - 1);
		} catch (IndexOutOfBoundsException e) {
			throw new UserError(callingOperator, 953, configuration.getSheet());
		}

		totalNumberOfColumns = configuration.getColumnLast() - columnOffset + 1;
		totalNumberOfRows = configuration.getRowLast() - rowOffset + 1;
		
		emptyColumns = new boolean[totalNumberOfColumns];
		emptyRows = new boolean[totalNumberOfRows];

		// filling offsets
		Arrays.fill(emptyColumns, true);
		Arrays.fill(emptyRows, true);

		// determine offsets and emptiness
		boolean foundAny = false;
		for (int r = 0; r < totalNumberOfRows; r++) {
			for (int c = 0; c < totalNumberOfColumns; c++) {
				if (emptyRows[r] || emptyColumns[c]) {
					final Cell cell = sheet.getCell(c+columnOffset, r+rowOffset);
					if (cell.getType() != CellType.EMPTY && !"".equals(cell.getContents().trim())) {
						foundAny = true;
						emptyRows[r] = false;
						emptyColumns[c] = false;
					}
				}
			}
		}
		if (!foundAny) {
			throw new UserError(callingOperator, 302, configuration.getFile().getPath(), "spreadsheet seems to be empty");
		}

		// retrieve attribute names: first count columns
		int numberOfAttributes = 0;
		for (int i = 0; i < totalNumberOfColumns; i++) {
			if (!emptyColumns[i])
				numberOfAttributes++;
		}

		// retrieve or generate attribute names
		attributeNames = new String[numberOfAttributes];
		for (int i = 0; i < totalNumberOfColumns; i++) {
			if (!emptyColumns[i]) {
				attributeNames[i] = Tools.getExcelColumnName(i);
			}
		}

		// // annotations
		// Annotations[] annotations = new Annotations[numberOfAttributes];
		// int columnCounter = 0;
		// for (int c = columnOffset; c < totalNumberOfColumns; c++) {
		// if (!emptyColumns[c]) {
		// annotations[columnCounter] = new Annotations();
		// for (String annotationKey : configuration.getAnnotations()) {
		// if (!ExcelExampleSource.ANNOTATION_NAME.equals(annotationKey)) {
		// Cell cell = sheet.getCell(c, rowOffset + configuration.getAnnotationLine(annotationKey));
		// annotations[columnCounter].put(annotationKey, cell.getContents());
		// }
		// }
		// columnCounter++;
		// }
		// }

		// setting annotation row to ignore
		// for (String annotationKey : configuration.getAnnotations()) {
		// if (!ExcelExampleSource.ANNOTATION_NAME.equals(annotationKey)) {
		// emptyRows[configuration.getAnnotationLine(annotationKey)] = true;
		// }
		// }
	}

	@Override
	public void reset(ProgressListener listener) {
		currentRow = rowOffset - 1;
		if (listener != null) {
			listener.setTotal(totalNumberOfRows);
			listener.setCompleted(0);
		}
	}

	@Override
	public boolean hasNext() {
		int nextRow = currentRow + 1;
		while (nextRow < totalNumberOfRows + rowOffset && emptyRows[nextRow - rowOffset])
			nextRow++;
		return nextRow < totalNumberOfRows + rowOffset;
	}

	@Override
	public void next(ProgressListener listener) {
		currentRow++;
		while (currentRow < totalNumberOfRows  + rowOffset && emptyRows[currentRow - rowOffset]) {
			currentRow++;
		}

		if (currentRow >= totalNumberOfRows + rowOffset) {
			throw new NoSuchElementException("No further row in excel sheet.");
		}

		currentRowCells = new Cell[attributeNames.length];
		int columnCounter = 0;
		for (int c = 0; c < totalNumberOfColumns; c++) {
			if (!emptyColumns[c]) {
				currentRowCells[columnCounter] = sheet.getCell(c+columnOffset, currentRow);
				columnCounter++;
			}
		}

		// notifying progress listener
		if (listener != null) {
			listener.setCompleted(currentRow);
		}
	}

	@Override
	public void close() throws OperatorException {
		// only close it if no other instance has control over this workbook
		if (!configuration.hasWorkbook())
			workbook.close();
	}

	@Override
	public int getNumberOfColumns() {
		return attributeNames.length;
	}

	@Override
	public String[] getColumnNames() {
		return attributeNames;
	}

	@Override
	public boolean isMissing(int columnIndex) {
		Cell cell = getCurrentCell(columnIndex);
		return cell.getType() == CellType.EMPTY || 
			cell.getType() == CellType.ERROR || 
			cell.getType() == CellType.FORMULA_ERROR || 
			cell.getContents() == null ||
			"".equals(cell.getContents().trim());
	}
	
	private Cell getCurrentCell(int index) {
		//return currentRowCells[index + columnOffset];
		return currentRowCells[index];
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if ((cell.getType() == CellType.NUMBER) ||
				(cell.getType() == CellType.NUMBER_FORMULA)) {
			final double value = ((NumberCell) cell).getValue();
			return Double.valueOf(value);
		} else {
			String valueString = cell.getContents();
			try {				
				return Double.valueOf(valueString);
			} catch (NumberFormatException e) {
				throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, valueString));
			}
		}
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if ((cell.getType() == CellType.DATE) ||
				(cell.getType() == CellType.DATE_FORMULA)) {
			return ((DateCell) cell).getDate();
		} else {
			String valueString = cell.getContents();
			throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, valueString));
		}
//		// TODO: Why is that???
//		int offset = TimeZone.getDefault().getOffset(date.getTime());
//		return new Date(date.getTime() - offset);
	}

	@Override
	public String getString(int columnIndex) {
		return getCurrentCell(columnIndex).getContents();
	}

	@Override
	public int[] getValueTypes() {
		return new int[this.attributeNames.length];
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		final CellType type = getCurrentCell(columnIndex).getType();
		if (type == CellType.EMPTY) {
			return ValueType.EMPTY;
		} else if  ((type == CellType.NUMBER) || (type == CellType.NUMBER_FORMULA)) {
			return ValueType.NUMBER;
		} else if ((type == CellType.DATE) || (type == CellType.DATE_FORMULA)) {
			return ValueType.DATE;
		} else {
			return ValueType.STRING;
		}
	}

	@Override
	public int getCurrentRow() {
		return currentRow;
	}

}
