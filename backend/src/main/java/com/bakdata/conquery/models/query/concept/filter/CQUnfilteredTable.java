package com.bakdata.conquery.models.query.concept.filter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQUnfilteredTable {
	@Valid
	@NotNull
	private ConnectorId id;

	@NotNull
	private ValidityDateContainer dateColumn;

	@JsonIgnore
	private Connector resolvedConnector;
}
