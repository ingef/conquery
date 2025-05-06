package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a single node or concept in a concept tree.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(of = "connectors")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class Concept<CONNECTOR extends Connector> extends ConceptElement<ConceptId> implements Authorized {

	/**
	 * Display Concept for users.
	 */
	private boolean hidden;

	private boolean defaultExcludeFromTimeAggregation = false;

	@JsonManagedReference
	@Valid
	private List<CONNECTOR> connectors = Collections.emptyList();

	@JacksonInject(useInput = OptBoolean.TRUE)
	private DatasetId dataset;

	/**
	 * rawValue is expected to be an Integer, expressing a localId for {@link TreeConcept#getElementByLocalId(int)}.
	 *
	 * <p>
	 * If {@link PrintSettings#isPrettyPrint()} is false, {@link ConceptElement#getId()} is used to print.
	 */
	public abstract String printConceptLocalId(Object rawValue, PrintSettings printSettings);

	@JsonIgnore
	public List<SelectId<?>> getDefaultSelects() {
		return getSelects().stream().filter(Select::isDefault)
						   .map(select -> (SelectId<?>) select.getId())
						   .collect(Collectors.toList());
	}

	public abstract List<? extends Select> getSelects();

	public Select getSelectByName(String name) {
		for (Select select : getSelects()) {
			if (select.getName().equals(name)) {
				return select;
			}
		}
		return null;
	}

	public void initElements() throws ConfigurationException, JSONException {
		getSelects().forEach(Select::init);
		getConnectors().forEach(CONNECTOR::init);
	}

	@Override
	@JsonIgnore
	public Concept<?> getConcept() {
		return this;
	}

	@Override
	public ConceptId createId() {
		return new ConceptId(dataset, getName());
	}

	public int countElements() {
		return 1;
	}

	/**
	 * Allows concepts to create their own altered FiltersNode if necessary.
	 */
	public QPNode createConceptQuery(QueryPlanContext context, List<FilterNode<?>> filters, List<Aggregator<?>> aggregators, List<Aggregator<CDateSet>> eventDateAggregators, ValidityDate validityDate) {
		final QPNode child = filters.isEmpty() && aggregators.isEmpty() ? new Leaf() : FiltersNode.create(filters, aggregators, eventDateAggregators);


		// Only if a validityDateColumn exists, capsule children in ValidityDateNode
		return validityDate != null ? new ValidityDateNode(validityDate, child) : child;
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return getId().createPermission(abilities);
	}

	public CONNECTOR getConnectorByName(String name) {
		for (CONNECTOR connector : connectors) {
			if (connector.getName().equals(name)) {
				return connector;
			}
		}
		return null;
	}

	public abstract ConceptElement<?> findById(ConceptElementId id);
}
