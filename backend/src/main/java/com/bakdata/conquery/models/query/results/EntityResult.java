package com.bakdata.conquery.models.query.results;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.error.ConqueryError;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface EntityResult {

	static SinglelineContainedEntityResult of(int entityId, Object[] values) {
		return new SinglelineContainedEntityResult(entityId, values);
	}
	
	static EntityResult of(int id, List<Object[]> values) {
		if(values.isEmpty()) {
			return notContained();
		}
		if(values.size() == 1) {
			return of(id, values.get(0));
		}
		return multilineOf(id, values);
	}
	
	static MultilineContainedEntityResult multilineOf(int entityId, List<Object[]> values) {
		return new MultilineContainedEntityResult(entityId, values);
	}
	
	static FailedEntityResult failed(int entityId, ConqueryError error) {
		return new FailedEntityResult(entityId, error);
	}
	
	static NotContainedEntityResult notContained() {
		return NotContainedEntityResult.INSTANCE;
	};
	
	default FailedEntityResult asFailed() {
		throw new IllegalStateException("The EntityResult "+this+" is not failed");
	}
	
	default ContainedEntityResult asContained() {
		throw new IllegalStateException("The EntityResult "+this+" is not contained");
	}
}