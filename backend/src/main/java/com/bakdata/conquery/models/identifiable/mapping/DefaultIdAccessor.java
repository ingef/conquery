package com.bakdata.conquery.models.identifiable.mapping;


public class DefaultIdAccessor extends IdAccessor {

	public DefaultIdAccessor() {
		super(null,null, null);
	}

	@Override
	public CsvId apply(String[] partOfId) {
		return CsvId.getFallbackCsvId(partOfId);
	}

}
