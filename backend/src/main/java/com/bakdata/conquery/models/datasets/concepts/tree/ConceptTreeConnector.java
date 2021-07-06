package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.validation.Valid;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter
@Slf4j
public class ConceptTreeConnector extends Connector {

	private static final long serialVersionUID = 1L;

	@NsIdRef @CheckForNull
	private Table table;

	@NsIdRef @CheckForNull
	private Column column = null;

	private CTCondition condition = null;

	@Valid @JsonManagedReference
	private List<Filter<?>> filters = new ArrayList<>();

	@JsonIgnore
	@ValidationMethod(message = "Table and Column usage are exclusive")
	public boolean isTableXOrColumn() {
		if(table != null){
			return column == null;
		}

		return column != null;
	}

	@JsonIgnore
	@ValidationMethod(message = "Column is not STRING.")
	public boolean isColumnForTree(){
		return column == null || column.getType().equals(MajorTypeId.STRING);
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
