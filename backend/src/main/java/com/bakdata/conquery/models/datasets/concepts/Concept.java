package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import jakarta.validation.Valid;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AccessLevel;
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
public abstract class Concept<CONNECTOR extends Connector> extends ConceptElement<ConceptId> implements Authorized, Initializing {

	/**
	 * Display Concept for users.
	 */
	private boolean hidden;

	private boolean defaultExcludeFromTimeAggregation = false;

	@JsonManagedReference
	@Valid
	private List<CONNECTOR> connectors = Collections.emptyList();

	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@Getter(AccessLevel.PRIVATE)
	private NamespacedStorageProvider namespacedStorageProvider;

	@JsonView(View.InternalCommunication.class)
	@Setter(AccessLevel.PRIVATE)
	@Nullable
	private DatasetId dataset;

	@Override
	public void init() throws Exception {

		if (this.dataset == null && this.namespacedStorageProvider != null) {
			NamespacedStorage namespacedStorage = namespacedStorageProvider.getStorage(null);
			this.dataset = namespacedStorage.getDataset().getId();
		}

	}

	@JsonIgnore
	public List<SelectId> getDefaultSelects() {
		return getSelects().stream().filter(Select::isDefault)
						   .map(select -> (SelectId) select.getId())
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
	public ConceptElement<?> getParent() {
		return null;
	}

	@Override
	public ConceptId createId() {
		return new ConceptId(getDataset(), getName());
	}

	public int countElements() {
		return 1;
	}

	/**
	 * Allows concepts to create their own altered FiltersNode if necessary.
	 */
	public QPNode createConceptQuery(
			QueryPlanContext context,
			List<FilterNode<?>> filters,
			List<Aggregator<?>> aggregators,
			EventDateUnionAggregator eventDateAggregators,
			ValidityDate validityDate) {
		final QPNode child;
		if (filters.isEmpty() && aggregators.isEmpty()) {
			child = new Leaf();
		}
		else {
			child = FiltersNode.create(filters, aggregators, eventDateAggregators);
		}


		// Only if a validityDateColumn exists, capsule children in ValidityDateNode
		if (validityDate != null) {
			return new ValidityDateNode(validityDate, child);
		}

		return child;
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

	public abstract ConceptElement<?> findById(ConceptElementId<?> id);
}
