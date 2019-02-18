package com.bakdata.conquery.models.query.results;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="MULTILINE_CONTAINED", base=EntityResult.class)
public class MultilineContainedEntityResult implements ContainedEntityResult {

	private final int entityId;
	private final List<String[]> values;

	@Override
	public Stream<String[]> streamValues() {
		return values.stream();
	}
}
