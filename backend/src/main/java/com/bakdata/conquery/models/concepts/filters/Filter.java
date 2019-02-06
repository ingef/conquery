package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class is the abstract superclass for all filters.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class Filter<FE_TYPE extends FilterValue<?>> extends Labeled<FilterId> {

	private String unit;
	private String description;

	@JsonBackReference
	private Connector connector;

	@JsonProperty("select")
	@Getter
	@Setter
	private SelectId selectId;

	public abstract void configureFrontend(FEFilter f) throws ConceptConfigurationException;

	@JsonIgnore
	public abstract Column[] getRequiredColumns();

	public final boolean requiresColumn(Column c) {
		return ArrayUtils.contains(getRequiredColumns(), c);
	}

	public abstract FilterNode createFilter(FE_TYPE filterValue, Aggregator<?> aggregator);

	@Override
	public FilterId createId() {
		return new FilterId(connector.getId(), getName());
	}

	/*
	public Condition createSimpleCondition(FE_TYPE qf) {return null;}
	public Condition createGroupCondition(FE_TYPE qf) {return null;}
	public Select<Record> generateComplexFilter(ComplexFilterExecutor exec, QueryContext context, FE_TYPE qf, Table<?> baseTable) {return null;}

	public abstract Field<Object> selectId();
	public abstract Field<Object> aggregate(QueryContext context);
	public Field<Object> combine(QueryContext context) {
		return JooqHelper.aggregateSameValueOrNull(DSL.field(DSL.name(DBNames.QUERY_RESULTS.FEATURE)));
	}
	public Field<Object> map(Field<Object> f) {
		return f;
	}*/
}
