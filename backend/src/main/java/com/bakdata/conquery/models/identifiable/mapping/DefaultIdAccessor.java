package com.bakdata.conquery.models.identifiable.mapping;


/*package*/ class DefaultIdAccessor extends IdAccessor {

	/*package*/ DefaultIdAccessor() {
		super(null,null);
	}

	@Override public String apply(String[] csvLine) {
		return String.join("|", csvLine);
	}

}
