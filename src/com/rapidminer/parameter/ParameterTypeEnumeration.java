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
package com.rapidminer.parameter;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Sebastian Land
 *
 */
public class ParameterTypeEnumeration extends CombinedParameterType {

	private static final long serialVersionUID = -3677952200700007724L;
	// only one character allowed
	private static final String ESCAPE_CHAR = "\\";
	private static final String ESCAPE_CHAR_REGEX = "\\\\";
	// only one character allowed, that additionally has no special meaning during regular expressions
	private static final String SEPERATOR_CHAR = "\u241E"; //",";

	private Object defaultValue;

	private ParameterType type;


	public ParameterTypeEnumeration(String key, String description, ParameterType parameterType) {
		super(key, description, parameterType);
		this.type = parameterType;
	}

	public ParameterTypeEnumeration(String key, String description, ParameterType parameterType, boolean expert) {	
		this(key, description, parameterType);
		setExpert(false);
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		Element element = doc.createElement("enumeration");
		element.setAttribute("key", key);
		String[] list = null;
		if (value != null) {
			list = transformString2Enumeration(value);
		} else {
			list = (String[]) getDefaultValue();
		}
		if (list != null) {
			for (String string: list) {
				element.appendChild(type.getXML(type.getKey(), string, false, doc));
			}						
		}		
		return element;
	}

	@Override
	@Deprecated
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		return "";
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public String getRange() {
		return "enumeration";
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getDefaultValueAsString() {
		if (defaultValue == null)
			return null;
		return getValueType().toString(defaultValue);
	}
	
	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public ParameterType getValueType() {
		return type;
	}

	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		String[] enumeratedValues = transformString2Enumeration(parameterValue);
		for (int i = 0; i < enumeratedValues.length; i++) {
			enumeratedValues[i] = type.notifyOperatorRenaming(oldOperatorName, newOperatorName, enumeratedValues[i]);
		}
		return transformEnumeration2String(Arrays.asList(enumeratedValues));

	}
	
	public static String transformEnumeration2String(List<String> list) {
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (String string: list) {
			if (!isFirst)
				builder.append(SEPERATOR_CHAR);
			builder.append(escape(string));
			isFirst = false;
		}
		return builder.toString();
	}


	public static String[] transformString2Enumeration(String parameterValue) {
		if (parameterValue != null && !parameterValue.equals("")) {
			String[] unescaped = parameterValue.split("(?<=[^"+ ESCAPE_CHAR_REGEX + "])" + SEPERATOR_CHAR, -1);
			for (int i = 0; i < unescaped.length; i++) {
				unescaped[i] = unescape(unescaped[i]);
			}
			return unescaped;
		}
		return new String[0];
	}

	private static String unescape(String escapedString) {
		escapedString = escapedString.replace(ESCAPE_CHAR + SEPERATOR_CHAR, SEPERATOR_CHAR);
		escapedString = escapedString.replace(ESCAPE_CHAR + ESCAPE_CHAR, ESCAPE_CHAR);
		return escapedString; 
	}

	private static String escape(String unescapedString) {
		unescapedString = unescapedString.replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR);
		return unescapedString.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);

	}
}
