package com.rapidminer.operator.nio.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Tools;

public class LoadFileOperator extends Operator {

	public static final String PARAMETER_FILENAME = "filename";
	public static final String PARAMETER_URL = "url";
	
	public static final String[] SOURCE_TYPES = new String[]{"file","URL"};
	public static final String PARAMETER_SOURCE_TYPE = "source_type";
	public static final int  SOURCE_TYPE_FILE = 0;
	public static final int  SOURCE_TYPE_URL = 1;
	
	public OutputPort fileOutputPort = getOutputPorts().createPort("file");
	
	private List<File> myTempFiles = new LinkedList<File>();
	
	public LoadFileOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(fileOutputPort, FileObject.class));
	}
	
	@Override
	public void doWork() throws OperatorException {
		File file;
		String source;
		switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
		case SOURCE_TYPE_FILE:
			file = getParameterAsFile(PARAMETER_FILENAME);
			source = file.getAbsolutePath();
			SimpleFileObject result = new SimpleFileObject(file);
			result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, source);
			fileOutputPort.deliver(result);
			break;
		case SOURCE_TYPE_URL:
			URL url;
			try {
				url = new URL(getParameterAsString(PARAMETER_URL));
				source = url.toString();
				URLConnection connection = url.openConnection();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				Tools.copyStreamSynchronously(connection.getInputStream(), buffer, true);
				BufferFileObject result1 = new BufferFileObject(buffer.toByteArray());
				result1.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, source);
				fileOutputPort.deliver(result1);
			} catch (MalformedURLException e) {
				throw new UserError(this, e, "313", getParameterAsString(PARAMETER_URL));
			} catch (IOException e) {
				throw new UserError(this, e, "316", getParameterAsString(PARAMETER_URL), e.getMessage());
			}
			break;
		default:
			// cannot happen
			throw new OperatorException("Illegal source type: "+getParameterAsString(PARAMETER_SOURCE_TYPE));
		}
		

	}

	@Override
	public void processFinished() throws OperatorException {
		for (File file : myTempFiles) {
			file.delete();
		}
		myTempFiles.clear();
		super.processFinished();
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		parameterTypes.add(new ParameterTypeCategory(PARAMETER_SOURCE_TYPE, "Choose wether to open a file or a URL.", SOURCE_TYPES, SOURCE_TYPE_FILE, false));
		
		ParameterTypeFile parameterTypeFile = new ParameterTypeFile(PARAMETER_FILENAME, "File to open", null, true, false);
		parameterTypeFile.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE, SOURCE_TYPES, true, SOURCE_TYPE_FILE));
		parameterTypes.add(parameterTypeFile);

		ParameterTypeString parameterTypeUrl = new ParameterTypeString(PARAMETER_URL, "URL to open", true, false);
		parameterTypeUrl.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE, SOURCE_TYPES, true, SOURCE_TYPE_URL));
		parameterTypes.add(parameterTypeUrl);
		return parameterTypes;
	}

}
