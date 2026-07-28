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

import java.util.Set;

abstract class AbstractSelectFilterConverter implements FilterConverter<SelectFilter, Set<String>> {

	@Override
	public SqlFilters convertToSqlFilter(SelectFilter filter, FilterContext<Set<String>> filterContext) {

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
	public Condition convertForTableExport(SelectFilter filter, FilterContext<Set<String>> filterContext) {
		Column column = filter.getColumn().resolve();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<String> field = DSL.field(DSL.name(tableName, columnName), String.class);
		return new MultiSelectCondition(field, getValues(filterContext), filterContext.getFunctionProvider()).condition();
	}

	protected abstract String[] getValues(FilterContext<Set<String>> filterContext);
}
