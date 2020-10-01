package com.bakdata.conquery.models.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQTable {
	@Valid
	@NotNull
	private List<FilterValue<?>> filters = Collections.emptyList();

	@Valid
	@NotNull
	@NsIdRefCollection
	private List<Select> selects = Collections.emptyList();

	@JsonBackReference
	private CQConcept concept;
	@Valid
	@NotNull
	private ConnectorId id;

	private ValidityDate dateColumn;

	@JsonIgnore
	private Connector resolvedConnector;
}