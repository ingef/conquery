package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import jakarta.validation.Valid;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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

	@CheckForNull
	private TableId table;

	@CheckForNull
	private ColumnId column = null;

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
		return column == null || column.resolve().getType().equals(MajorTypeId.STRING);
	}

	@Override @JsonIgnore
	public Table getResolvedTable() {
		if(column != null){
			return column.getTable().resolve();
		}

		if (table != null) {
			return table.resolve();
		}
		return null;
	}

	@Override @JsonIgnore
	public TableId getResolvedTableId() {
		if(column != null){
			return column.getTable();
		}

		if (table != null) {
			return table;
		}
		return null;
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
