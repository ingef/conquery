package com.bakdata.conquery.models.query.results;

import java.util.stream.Stream;

public interface ContainedEntityResult extends EntityResult {

	int getEntityId();
	/**
	 * Provides the number of columns this result contains.
	 */
	int columnCount();
	Stream<Object[]> streamValues();
	
	static Stream<ContainedEntityResult> filterCast(EntityResult result) {
		if(result instanceof ContainedEntityResult) {
			return Stream.of(result.asContained());
		}
		return Stream.empty();
	}
	
	@Override
	default ContainedEntityResult asContained() {
		return this;
	}
}
