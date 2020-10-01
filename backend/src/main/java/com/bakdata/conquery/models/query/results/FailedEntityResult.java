package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.error.ConqueryError;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import javax.validation.constraints.NotEmpty;

@Getter @Setter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="FAILED", base=EntityResult.class)
public class FailedEntityResult implements SinglelineEntityResult {

	private final int entityId;
	@NotEmpty
	private final ConqueryError error;
		
	@JsonIgnore @Override
	public boolean isFailed() {
		return true;
	}

	@Override
	public boolean isContained() {
		return false;
	}

	@Override
	public FailedEntityResult asFailed() {
		return this;
	}
	
	@Override @JsonIgnore
	public Object[] getValues() {
		throw new IllegalStateException("A FAILED result has no values.");
	}
}
