package com.bakdata.conquery.models.query.results;

import java.util.stream.Stream;

public interface ContainedEntityResult extends EntityResult {

	int getEntityId();
	Stream<String[]> streamValues();
	
	static Stream<ContainedEntityResult> filterCast(EntityResult result) {
		if(result instanceof ContainedEntityResult) {
			return Stream.of((ContainedEntityResult)result);
		}
		else {
			return Stream.empty();
		}
	}
}
