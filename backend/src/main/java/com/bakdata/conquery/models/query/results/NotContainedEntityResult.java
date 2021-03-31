package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@CPSType(id="NOT_CONTAINED", base=EntityResult.class)
public enum NotContainedEntityResult implements EntityResult, SinglelineEntityResult {
	INSTANCE;
	
	@Override
	public String toString() {
		return "NOT_CONTAINED";
	}

	@Override @JsonIgnore
	public Object[] getValues() {
		throw new IllegalStateException("A NOT_CONTAINED result has no values.");
	}
}
