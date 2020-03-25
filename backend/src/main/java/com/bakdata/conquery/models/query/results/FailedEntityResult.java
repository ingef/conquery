package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter @Setter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="FAILED", base=EntityResult.class)
public class FailedEntityResult implements SinglelineEntityResult {

	private final int entityId;
	@NotEmpty
	private final Throwable throwable;
		
	@JsonIgnore @Override
	public boolean isFailed() {
		return true;
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
