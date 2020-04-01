package com.bakdata.conquery.models.concepts.tree;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
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
	
	@NotNull @NsIdRef
	private Column column;

	private CTCondition condition;

	@Valid @JsonManagedReference
	private List<Filter<?>> filters = new ArrayList<>();

	@Override @JsonIgnore
	public Table getTable() {
		return column.getTable();
	}

	@Override
	public List<Filter<?>> collectAllFilters() {
		return filters;
	}

	@Override
	public TreeConcept getConcept() {
		return (TreeConcept) super.getConcept();
	}
}
