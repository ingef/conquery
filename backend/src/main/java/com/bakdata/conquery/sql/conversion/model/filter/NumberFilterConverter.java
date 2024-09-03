package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class NumberFilterConverter<RANGE extends IRange<? extends Number, ?>> implements FilterConverter<NumberFilter<RANGE>, RANGE> {

	@Override
	public SqlFilters convertToSqlFilter(NumberFilter<RANGE> filter, FilterContext<RANGE> filterContext) {

		Column column = filter.getColumn();
		ConnectorSqlTables tables = filterContext.getTables();

		Class<? extends Number> numberClass = NumberMapUtil.getType(column);
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), numberClass);

		Field<? extends Number> eventFilterCtePredecessor = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.EVENT_FILTER)).select();
		IRange<? extends Number, ?> filterValue = NumberFilter.readFilterValue(filterContext.getValue(), column.getType(), filter.getConfig());
		NumberCondition condition = new NumberCondition(eventFilterCtePredecessor, filterValue);

		ConnectorSqlSelects selects = ConnectorSqlSelects.builder().preprocessingSelects(List.of(rootSelect)).build();

		WhereClauses whereClauses = WhereClauses.builder().eventFilter(condition).build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(NumberFilter<RANGE> filter, FilterContext<RANGE> filterContext) {
		Column column = filter.getColumn();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<Number> field = DSL.field(DSL.name(tableName, columnName), Number.class);
		IRange<? extends Number, ?> range = NumberFilter.readFilterValue(filterContext.getValue(), column.getType(), filter.getConfig());

		return new NumberCondition(field, range).condition();
	}
}
