package com.bakdata.conquery.models.exceptions;

import java.io.File;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.util.io.LogUtil;

/**
 * This exception if thrown if there is any kind of error in one of the concept tree configurations.
 */
public class ConceptConfigurationException extends JSONException {

	private static final long serialVersionUID = 1L;

	public ConceptConfigurationException(String node, String message, Throwable cause) {
		super("In Node "+node+": "+message, cause);
	}
	
	public ConceptConfigurationException(Identifiable<?, ?> node, String message, Throwable cause) {
		this(node.toString()+"("+node+")", message, cause);
	}

	public ConceptConfigurationException(String node, String message) {
		this(node, message, null);
	}
	
	public ConceptConfigurationException(Identifiable<?, ?> node, String message) {
		this(node.toString()+"("+node+")", message, null);
	}

	public ConceptConfigurationException(File f, String message, Throwable e) {
		super("In Concept Tree file "+LogUtil.printPath(f)+": "+message,e);
	}
	
	public ConceptConfigurationException(File f, String message) {
		super("In Concept Tree file "+LogUtil.printPath(f)+": "+message);
	}

}
