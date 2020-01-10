package com.bakdata.conquery.io.xodus;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import jetbrains.exodus.env.Environment;

public interface ConqueryStorage extends Closeable {

	File getDirectory();
	Validator getValidator();
	Environment getEnvironment();
	CentralRegistry getCentralRegistry();
	
	void loadData();

	/**
	 * Completely remove the Storage, deleting its contents.
	 */
	void remove() throws IOException;
}
