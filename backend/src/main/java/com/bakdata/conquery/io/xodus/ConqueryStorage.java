package com.bakdata.conquery.io.xodus;

import java.io.Closeable;

import javax.validation.Validator;

import com.bakdata.conquery.models.identifiable.CentralRegistry;

public interface ConqueryStorage extends Closeable {

	Validator getValidator();
	CentralRegistry getCentralRegistry();
	
	void loadData();

	/**
	 * Delete the storage's contents.
	 */
	void clear();

	/**
	 * Remove the storage.
	 */
	void remove();
}
