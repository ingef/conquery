package com.bakdata.conquery.io.xodus;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import jetbrains.exodus.env.Environment;

import javax.validation.Validator;
import java.io.Closeable;
import java.io.File;

public interface ConqueryStorage extends Closeable {

	File getDirectory();
	Validator getValidator();
	Environment getEnvironment();
	CentralRegistry getCentralRegistry();
	
	void loadData();
}
