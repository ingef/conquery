package com.bakdata.conquery.models.concepts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdReferenceDeserializer;
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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
	private List<ValidityDate> validityDates = new ArrayList<>();
	@JsonBackReference
	private Concept<?> concept;
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private transient IdMap<FilterId, Filter<?>> allFiltersMap;

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

	@JsonDeserialize(contentUsing = NsIdReferenceDeserializer.class)
	public void setSelectableDates(List<Column> cols) {
		this.setValidityDates(
				cols
						.stream()
						.map(c -> {
							ValidityDate sd = new ValidityDate();
							sd.setColumn(c);
							sd.setName(c.getName());
							sd.setConnector(this);
							return sd;
						})
						.collect(Collectors.toList())
		);
	}

	@Override
	public ConnectorId createId() {
		return new ConnectorId(concept.getId(), getName());
	}

	public abstract Table getTable();

	@JsonIgnore
	public Column getSelectableDate(String name) {
		return validityDates
					   .stream()
					   .filter(vd -> vd.getName().equals(name))
					   .map(ValidityDate::getColumn)
					   .findAny()
					   .orElseThrow(() -> new IllegalArgumentException("Unable to find date " + name));
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Filters are for Connector's table.")
	public boolean isFiltersForTable() {
		boolean valid = true;

		for (Filter<?> filter : collectAllFilters()) {
			for (Column column : filter.getRequiredColumns()) {
				if (column == null || column.getTable() == getTable()) {
					continue;
				}

				log.error("Filter[{}] of Table[{}] is not of Connector[{}]#Table[{}]", filter.getId(), column.getTable().getId(), getId(), getTable().getId());
				valid = false;
			}
		}

		return valid;
	}

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
	@ValidationMethod(message = "Not all validity dates are Date-compatible.")
	public boolean isValidValidityDates() {
		if (validityDates == null) {
			return true;
		}
		boolean passed = true;
		for (ValidityDate date : validityDates) {
			if (date.getColumn().getType().isDateCompatible()) {
				continue;
			}

			passed = false;
			log.error("ValidityDate-Column[{}] for Connector[{}] is not of type DATE or DATERANGE", date.getColumn().getId(), getId());
		}
		return passed;
	}

	@JsonIgnore
	@ValidationMethod
	public boolean isValidityDatesForTable() {
		if (validityDates == null) {
			return true;
		}
		boolean passed = true;
		for (ValidityDate sd : validityDates) {
			Column col = sd.getColumn();

			if (!col.getTable().equals(getTable())) {
				passed = false;
				log.error("ValidityDate[{}](Column = `{}`) does not belong to Connector[{}]#Table[{}]", sd.getId(), col.getId(), getId(), getTable().getId());
			}
		}
		return passed;
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
