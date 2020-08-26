package com.bakdata.conquery.models.concepts.tree;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.validation.Valid;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConceptTreeConnector extends Connector {

	private static final long serialVersionUID = 1L;

	@NsIdRef @CheckForNull
	private Table table;

	@NsIdRef @CheckForNull
	private Column column = null;

	private CTCondition condition = null;

	@Valid @JsonManagedReference
	private List<Filter<?>> filters = new ArrayList<>();

	@ValidationMethod(message = "Table and Column usage are exclusive")
	public boolean tableXOrColumn() {
		return table == null ^ column == null;
	}


	@Override @JsonIgnore
	public Table getTable() {
		if(column != null){
			return column.getTable();
		}

		return table;
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
