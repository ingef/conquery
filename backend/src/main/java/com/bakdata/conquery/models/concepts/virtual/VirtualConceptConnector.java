package com.bakdata.conquery.models.concepts.virtual;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.select.Select;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VirtualConceptConnector extends Connector {

	private static final long serialVersionUID = 1L;

	@NotNull @NsIdRef
	private Table table;
	@Valid @JsonManagedReference
	private Filter<?> filter;

	@Valid @JsonManagedReference
	private Select[] select;

	@Override
	public Collection<Filter<?>> collectAllFilters() {
		return Stream.of(getDateSelectionFilter(), filter).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	protected Collection<Select> collectAllSelects() {
		//TODO Fetch Aggregator from Filter
		return Arrays.asList(select);
	}
/*
	@Override
	public EventProcessingResult processEvent(Event r) {
		CDateRange dateRange = extractValidityDates(r);
		getConcept().incMatchingEntries(dateRange);
		return EventProcessingResult.OK;
	}*/
}
