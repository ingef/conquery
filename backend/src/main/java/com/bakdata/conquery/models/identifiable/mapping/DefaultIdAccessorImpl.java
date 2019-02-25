package com.bakdata.conquery.models.identifiable.mapping;


/**
 * This class is used as an IdAccessorImpl whenever we fail to get a proper configured one.
 * *
 */
public enum DefaultIdAccessorImpl implements IdAccessor {
	INSTANCE;

	@Override
	public CsvEntityId getCsvEntityId(String[] csvLine) {
		return IdAccessorImpl.getFallbackCsvId(csvLine);
	}
}
