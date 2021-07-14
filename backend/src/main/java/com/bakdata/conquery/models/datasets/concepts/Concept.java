package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.Validator;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
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
public abstract class Concept<CONNECTOR extends Connector> extends ConceptElement<ConceptId> implements Authorized {

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

	/**
	 * Allows concepts to create their own altered FiltersNode if necessary.
	 */
	public QPNode createConceptQuery(QueryPlanContext context, List<FilterNode<?>> filters, List<Aggregator<?>> aggregators, List<Aggregator<CDateSet>> eventDateAggregators) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			return new Leaf();
		}
		return FiltersNode.create(filters, aggregators, eventDateAggregators);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return ConceptPermission.onInstance(abilities, getId());
	}
}
