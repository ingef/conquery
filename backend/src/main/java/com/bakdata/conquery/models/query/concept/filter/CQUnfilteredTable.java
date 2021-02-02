package com.bakdata.conquery.models.query.concept.filter;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class CQUnfilteredTable {
	@Valid
	@NotNull
	private final ConnectorId id;

	@Nullable
	private final ValidityDateContainer dateColumn;
}
