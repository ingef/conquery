package com.bakdata.conquery.io.xodus;

import java.io.Closeable;
import java.io.IOException;
import java.io.File;

import javax.validation.Validator;

import com.bakdata.conquery.models.identifiable.CentralRegistry;

public interface ConqueryStorage extends Closeable {

	Validator getValidator();
	CentralRegistry getCentralRegistry();
	
	void loadData();

	/**
	 * Completely remove the Storage, deleting its contents.
	 */
	void remove() throws IOException;
	
	/**
	 * Gives a human readable information about the origin of this store,
	 * i.e. a place where the stored data comes from (a folder or database).
	 * @return String presenting the origin for this store.
	 */
	String getStorageOrigin();
	void clear();
}
