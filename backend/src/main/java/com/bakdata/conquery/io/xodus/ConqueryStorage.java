package com.bakdata.conquery.io.xodus;

import java.io.Closeable;
import java.io.File;

import javax.validation.Validator;

import com.bakdata.conquery.models.identifiable.CentralRegistry;

import jetbrains.exodus.env.Environment;

public interface ConqueryStorage extends Closeable {

	File getDirectory();
	Validator getValidator();
	Environment getEnvironment();
	CentralRegistry getCentralRegistry();
	
	void loadData();
}
