package com.bakdata.conquery.models.concepts;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.Validator;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a single node or concept in a concept tree.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(of = {"connectors"})
@Getter
@Setter
public abstract class Concept<CONNECTOR extends Connector> extends ConceptElement<ConceptId> {

	/**
	 * Display Concept for users.
	 */
	private boolean hidden = false;

	@JsonManagedReference
	@Valid
	private List<CONNECTOR> connectors = Collections.emptyList();

	@NsIdRef
	private Dataset dataset;

	public List<Select> getDefaultSelects() {
		return getSelects()
					  .stream()
					  .filter(Select::isDefault)
					  .collect(Collectors.toList());
	}

	public CONNECTOR getConnector(ConnectorId connectorId) {
		return connectors.stream()
						 .filter(conn -> connectorId.equals(conn.getId()))
						 .findAny()
						 .orElseThrow(() -> new NoSuchElementException("Connector not found: " + connectorId));
	}

	public abstract List<? extends Select> getSelects();

	public void initElements(Validator validator) throws ConfigurationException, JSONException {
	}

	@Override
	@JsonIgnore
	public Concept<?> getConcept() {
		return this;
	}

	@Override
	public ConceptId createId() {
		return new ConceptId(dataset.getId(), getName());
	}

	public int countElements() {
		return 1;
	}

	@Override
	public long calculateBitMask() {
		return 0L;
	}

	/**
	 * Allows concepts to create their own altered FiltersNode if necessary.
	 */
	public QPNode createConceptQuery(QueryPlanContext context, List<FilterNode<?>> filters, List<Aggregator<?>> aggregators) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			return new Leaf();
		}
		return FiltersNode.create(filters, aggregators);
	}
}
