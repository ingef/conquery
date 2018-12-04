package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(id="NOT_CONTAINED", base=EntityResult.class)
public enum NotContainedEntityResult implements EntityResult {
	INSTANCE;
	
	@Override
	public String toString() {
		return "NOT_CONTAINED";
	}
}
