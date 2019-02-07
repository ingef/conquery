package com.bakdata.conquery.models.identifiable.mapping;


public class DefaultIdAccessor extends IdAccessor {

	public DefaultIdAccessor() {
		super(null,null);
	}

	@Override public String apply(String[] csvLine) {
		return String.join("|", csvLine);
	}

}
