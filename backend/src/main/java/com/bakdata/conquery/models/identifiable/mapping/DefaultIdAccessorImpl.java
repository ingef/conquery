package com.bakdata.conquery.models.identifiable.mapping;

import lombok.RequiredArgsConstructor;

/**
 * This class is used as an IdAccessorImpl whenever we fail to get a proper configured one.
 * *
 */
@RequiredArgsConstructor
public enum DefaultIdAccessorImpl implements IdAccessor {

	INSTANCE;
	
	@Override
	public CsvEntityId getCsvEntityId(String[] csvLine) {
		return new CsvEntityId(csvLine[0]);
	}
}
