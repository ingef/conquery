package com.bakdata.conquery.models.identifiable.mapping;


/**
 * This class is used as an IdAccessorImpl whenever we fail to get a proper configured one.
 * *
 */
public class DefaultIdAccessorImpl implements IdAccessor {

	@Override
	public CsvEntityId apply(String[] partOfId) {
		return IdAccessorImpl.getFallbackCsvId(partOfId);
	}

}
