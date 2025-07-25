package com.bakdata.conquery.models.datasets.concepts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset.Entry;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A connector represents the connection between a column and a concept.
 */
@Getter
@Setter
@Valid
@Slf4j
@JsonIgnoreProperties({"defaultForEntityPreview"})
public abstract class Connector extends LabeledNamespaceIdentifiable<ConnectorId> implements SelectHolder<Select> {

	public static final int[] NOT_CONTAINED = new int[]{-1};

	@Nullable
	@JsonAlias("validityDatesTooltip")
	private String validityDatesDescription;

	@NotNull
	@JsonManagedReference
	@Valid
	private List<ValidityDate> validityDates = new ArrayList<>();

	@JsonBackReference
	@EqualsAndHashCode.Exclude
	private Concept<?> concept;

	@JsonIgnore
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Valid
	private transient IdMap<FilterId, Filter<?>> allFiltersMap;
	@NotNull
	@Getter
	@Setter
	@JsonManagedReference
	@Valid
	private List<Select> selects = new ArrayList<>();
	/**
	 * Determines if the connector is preselected for the user when creating a new {@link com.bakdata.conquery.apiv1.query.concept.specific.CQConcept}.
	 */
	@JsonProperty("default")
	private boolean isDefault = true;

	public static boolean isNotContained(int[] mostSpecificChildren) {
		return Arrays.equals(mostSpecificChildren, NOT_CONTAINED);
	}

	public Collection<Filter<?>> getFilters() {
		return allFiltersMap.values();
	}

	@CheckForNull
	public abstract ColumnId getColumn();

	@CheckForNull
	public abstract CTCondition getCondition();

	@JsonIgnore
	public List<Select> getDefaultSelects() {
		return getSelects()
				.stream().filter(Select::isDefault)
				.collect(Collectors.toList());
	}

	@Override
	public Concept<?> findConcept() {
		return concept;
	}

	@Override
	public ConnectorId createId() {
		return new ConnectorId(concept.getId(), getName());
	}

	public abstract Table getResolvedTable();

	public abstract TableId resolveTableId();

	@JsonIgnore
	@ValidationMethod(message = "Filter names are not unique.")
	public boolean isUniqueFilterNames() {
		boolean valid = true;

		for (Entry<String> e : collectAllFilters().stream().map(Filter::getName).collect(ImmutableMultiset.toImmutableMultiset()).entrySet()) {
			if (e.getCount() == 1) {
				continue;
			}

			valid = false;
			log.error("Multiple Filters with name `{}` for Connector[{}]", e.getElement(), getId());
		}

		return valid;
	}

	@JsonIgnore
	public abstract List<Filter<?>> collectAllFilters();

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return getConcept().getDataset();
	}

	public void init() {
		getSelects().forEach(Select::init);
	}

	public Filter<?> getFilterByName(String name) {
		for (Filter<?> filter : collectAllFilters()) {
			if (filter.getName().equals(name)) {
				return filter;
			}
		}
		return null;
	}

	public Select getSelectByName(String name) {
		for (Select select : getSelects()) {

			if (select.getName().equals(name)) {
				return select;
			}
		}

		return null;
	}

	public ValidityDate getValidityDateByName(String name) {
		for (ValidityDate validityDate : validityDates) {
			if (validityDate.getName().equals(name)) {
				return validityDate;
			}
		}
		return null;
	}
}
