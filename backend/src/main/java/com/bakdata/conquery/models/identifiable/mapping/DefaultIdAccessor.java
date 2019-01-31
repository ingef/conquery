package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Map;

/*package*/ class DefaultIdAccessor extends IdAccessor {

	/*package*/ DefaultIdAccessor() {
		super(null,null);
	}

	@Override public String apply(String[] csvLine) {
		return String.join("|", csvLine);
	}

}
