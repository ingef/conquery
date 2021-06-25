package com.bakdata.conquery.apiv1.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CQTable {
	@Valid
	@NotNull
	private List<FilterValue<?>> filters = Collections.emptyList();

	@NotNull
	@NsIdRefCollection
	private List<Select> selects = Collections.emptyList();

	@JsonBackReference
	private CQConcept concept;

	@NsIdRef
	@JsonProperty("id")
	private Connector connector;

	private ValidityDateContainer dateColumn;

	@JsonIgnore
	@ValidationMethod(message = "Connector does not belong to Concept.")
	public boolean isConnectorForConcept() {
		return connector.getConcept().equals(concept.getConcept());
	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate does not belong to Connector.")
	public boolean isValidityDateForConnector() {
		return dateColumn == null || dateColumn.getValue().getConnector().equals(getConnector());
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Selects belong to Connector.")
	public boolean isAllSelectsForConnector() {
		return selects.stream().allMatch(select -> select.getHolder().equals(connector));
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Filters belong to Connector.")
	public boolean isAllFiltersForConnector() {
		return filters.stream().allMatch(filter -> filter.getFilter().getConnector().equals(connector));
	}

}