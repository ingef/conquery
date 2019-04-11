package com.bakdata.conquery.models.query.results;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface EntityResult {

	static ContainedEntityResult of(int entityId, Object[] values) {
		return new SinglelineContainedEntityResult(entityId, values);
	}
	
	static ContainedEntityResult multilineOf(int entityId, List<Object[]> values) {
		return new MultilineContainedEntityResult(entityId, values);
	}
	
	static FailedEntityResult failed(int entityId, Throwable t) {
		return new FailedEntityResult(entityId, t);
	}
	
	static NotContainedEntityResult notContained() {
		return NotContainedEntityResult.INSTANCE;
	};
	
	@JsonIgnore
	default boolean isFailed() {
		return false;
	}
	
	default FailedEntityResult asFailed() {
		throw new IllegalStateException("The EntityResult "+this+" is not failed");
	}
}