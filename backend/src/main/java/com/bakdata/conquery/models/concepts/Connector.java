package com.bakdata.conquery.models.concepts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset.Entry;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
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
public abstract class Connector extends Labeled<ConnectorId> implements Serializable, SelectHolder<Select> {

	public static final int[] NOT_CONTAINED = new int[]{-1};
	private static final long serialVersionUID = 1L;

	@NotNull
	@JsonManagedReference
	@Valid
	private List<ValidityDate> validityDates = new ArrayList<>();

	@JsonBackReference
	private Concept<?> concept;

	@JsonIgnore
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Valid
	private transient IdMap<FilterId, Filter<?>> allFiltersMap;

	public Collection<Filter<?>> getFilters() {
		return allFiltersMap.values();
	}

	@NotNull
	@Getter
	@Setter
	@JsonManagedReference
	@Valid
	private List<Select> selects = new ArrayList<>();

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

	public abstract Table getTable();

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



	public Filter<?> getFilterByName(String name) {
		return collectAllFilters().stream()
								  .filter(f -> name.equals(f.getName()))
								  .findAny()
								  .orElseThrow(() -> new IllegalArgumentException("Unable to find filter " + name));
	}

	@JsonIgnore
	public abstract List<Filter<?>> collectAllFilters();

	public <T extends Filter> T getFilter(FilterId id) {
		if (allFiltersMap == null) {
			allFiltersMap = new IdMap<>(collectAllFilters());
		}
		return (T) allFiltersMap.getOrFail(id);
	}

	public Column getValidityDateColumn(ValidityDateId id) {
		for (ValidityDate vDate : validityDates) {
			if (vDate.getId().equals(id)) {
				return vDate.getColumn();
			}
		}

		throw new NoSuchElementException("There is no validityDate called '" + id + "' in " + this);
	}

	public synchronized void addImport(Import imp) {
		for (Filter<?> f : collectAllFilters()) {
			f.addImport(imp);
		}
	}

	public static boolean isNotContained(int[] mostSpecificChildren) {
		return Arrays.equals(mostSpecificChildren, NOT_CONTAINED);
	}

	/**
	 * @param cBlock
	 * @param bucket
	 */
	public abstract void calculateCBlock(CBlock cBlock, Bucket bucket);
}
