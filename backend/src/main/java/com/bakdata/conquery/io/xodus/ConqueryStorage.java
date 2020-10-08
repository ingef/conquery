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
	 * Gives a human readable information about the origin of this store,
	 * i.e. a place where the stored data comes from (a folder or database).
	 * @return String presenting the origin for this store.
	 */
	String getStorageOrigin();
}
