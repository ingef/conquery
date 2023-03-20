package com.bakdata.conquery.apiv1.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "concept")
@EqualsAndHashCode
public class CQTable {
	@Valid
	@NotNull
	private List<FilterValue<?>> filters = Collections.emptyList();

	@NotNull
	@NsIdRefCollection
	private List<Select> selects = Collections.emptyList();

	@JsonBackReference
	@EqualsAndHashCode.Exclude
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

	public void resolve(QueryResolveContext context) {
		filters.forEach(f -> f.resolve(context));
	}

	@CheckForNull
	public Column findValidityDateColumn() {

		// if no dateColumn is provided, we use the default instead which is always the first one.
		// Set to null if none-available in the connector.
		if (dateColumn != null) {
			return dateColumn.getValue().getColumn();
		}

		if (!connector.getValidityDates().isEmpty()) {
			return connector.getValidityDates().get(0).getColumn();
		}

		return null;
	}
}