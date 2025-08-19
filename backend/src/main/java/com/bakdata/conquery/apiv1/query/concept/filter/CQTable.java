package com.bakdata.conquery.apiv1.query.concept.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
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
	private List<ConnectorSelectId> selects = Collections.emptyList();

	@JsonBackReference
	@EqualsAndHashCode.Exclude
	private CQConcept concept;

	@JsonProperty("id")
	private ConnectorId connector;

	private ValidityDateContainer dateColumn;

	@JsonIgnore
	@ValidationMethod(message = "Connector does not belong to Concept.")
	public boolean isConnectorForConcept() {
		return connector.getConcept().equals(concept.getConceptId());
	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate does not belong to Connector.")
	public boolean isValidityDateForConnector() {
		return dateColumn == null || dateColumn.getValue().getConnector().equals(getConnector());
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Selects belong to Connector.")
	public boolean isAllSelectsForConnector() {
		return selects.stream().map(ConnectorSelectId::getConnector).allMatch(connectorId -> connectorId.equals(connector));
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
	public ValidityDate findValidityDate() {

		if (dateColumn != null) {
			return dateColumn.getValue().resolve();
		}

		final Connector resolvedConnector = connector.resolve();
		if (!resolvedConnector.getValidityDates().isEmpty()) {
			return resolvedConnector.getValidityDates().get(0);
		}

		return null;
	}

	public boolean hasSelectedSecondaryId(SecondaryIdDescriptionId secondaryId) {
		final Connector resolvedConnector = connector.resolve();
		return Arrays.stream(resolvedConnector.getResolvedTable().getColumns())
					 .map(Column::getSecondaryId)
					 .anyMatch(secondaryId::equals);
	}

}
