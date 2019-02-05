package com.bakdata.conquery.models.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.select.Select;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CQTable {
	@Valid @NotNull
	private ConnectorId id;
	@Valid @NotNull
	private List<FilterValue> filters = Collections.emptyList();

	@Valid @NotNull
	private List<SelectId> select = Collections.emptyList();

	@JsonBackReference
	private CQConcept concept;

	@JsonIgnore
	private Connector resolvedConnector;

	@JsonIgnore
	private Select[] resolvedSelects;
}
