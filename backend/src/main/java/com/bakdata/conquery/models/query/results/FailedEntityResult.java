package com.bakdata.conquery.models.query.results;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="FAILED", base=EntityResult.class)
public class FailedEntityResult implements EntityResult {

	private final int entityId;
	@NotEmpty
	private final String exceptionStackTrace;
	
	public FailedEntityResult(int entityId, Throwable exception) {
		this.entityId = entityId;
		this.exceptionStackTrace = ExceptionUtils.getStackTrace(exception);
	}
	
	@JsonIgnore @Override
	public boolean isFailed() {
		return true;
	}
	
	@Override
	public FailedEntityResult asFailed() {
		return this;
	}
}
