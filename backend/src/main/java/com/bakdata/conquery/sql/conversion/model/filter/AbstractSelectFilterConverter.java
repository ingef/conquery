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

abstract class AbstractSelectFilterConverter<F extends SelectFilter<T>, T> implements FilterConverter<F, T> {

	@Override
	public SqlFilters convertToSqlFilter(F filter, FilterContext<T> filterContext) {

		ExtractingSqlSelect<String> rootSelect = new ExtractingSqlSelect<>(
				filterContext.getTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				filter.getColumn().getColumn(),
				String.class
		);

		WhereCondition condition = new MultiSelectCondition(
				rootSelect.qualify(filterContext.getTables().getPredecessor(ConceptCteStep.EVENT_FILTER)).select(),
				getValues(filterContext),
				filterContext.getFunctionProvider()
		);

		return new SqlFilters(
				ConnectorSqlSelects.builder()
								   .preprocessingSelect(rootSelect)
								   .build(),
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
		Field<String> field = DSL.field(DSL.name(tableName, columnName), String.class);
		return new MultiSelectCondition(field, getValues(filterContext), filterContext.getFunctionProvider()).condition();
	}

	protected abstract String[] getValues(FilterContext<T> filterContext);
}
