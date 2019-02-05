package com.bakdata.conquery.models.concepts.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.query.select.Select;
import org.apache.commons.collections4.CollectionUtils;

import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConceptTreeConnector extends Connector {

	private static final long serialVersionUID = 1L;
	
	@NotNull @IdReference
	private Column column;
	@Valid @JsonManagedReference
	private List<Filter<?>> filters = new ArrayList<>();

	@Valid @JsonManagedReference
	private List<Select> selects = new ArrayList<>();


	@Override @JsonIgnore
	public Table getTable() {
		return column.getTable();
	}

	@Override
	public Collection<Filter<?>> collectAllFilters() {
		List<Filter<?>> l = new ArrayList<>(filters.size()+1);
		CollectionUtils.addIgnoreNull(l, getDateSelectionFilter());
		l.addAll(filters);
		return l;
	}

	@Override
	protected Collection<Select> collectAllSelects() {
		return new ArrayList<>(selects);
	}

	/*
	@Override
	public EventProcessingResult processEvent(Event r) throws ConceptConfigurationException {
		ConceptTreeChild concept = getConcept().findMostSpecificConcept(column, r.<Integer>get(column), null);//see #172  new CalculatedValue<>(r::createRowMap));
		if(concept!=null) {
			CDateRange dateRange = extractValidityDates(r);
			concept.incMatchingEntries(dateRange);

			r.addPrefixEntry(this, concept.getPrefix());
			return EventProcessingResult.OK;
		}
		else
			return EventProcessingResult.COULD_NOT_RESOLVE;
	}
	*/
	@Override
	public TreeConcept getConcept() {
		return (TreeConcept) super.getConcept();
	}
}
