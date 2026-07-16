package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jooq.impl.DSL.field;

abstract class AbstractSelectFilterConverter<F extends SelectFilter<T>, T> implements FilterConverter<F, T> {

	@Override
	public SqlFilters convertToSqlFilter(F filter, FilterContext<T> filterContext) {

		WhereCondition condition = new MultiSelectCondition(
				field(name(filterContext.getTables().getRootTable(), filter.getColumn().getColumn()), String.class),
				getValues(filterContext),
				filterContext.getFunctionProvider()
		);

		return new SqlFilters(
				ConnectorSqlSelects.none(),
				WhereClauses.builder()
							.eventFilter(condition)
							.build()
		);
	}

	@Override
	public Condition convertForTableExport(F filter, FilterContext<T> filterContext) {
		Column column = filter.getColumn().resolve();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<String> field = field(DSL.name(tableName, columnName), String.class);
		return new MultiSelectCondition(field, getValues(filterContext), filterContext.getFunctionProvider()).condition();
	}

	protected abstract String[] getValues(FilterContext<T> filterContext);
}
