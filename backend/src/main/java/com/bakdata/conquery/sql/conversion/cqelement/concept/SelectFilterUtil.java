package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class SelectFilterUtil {

	public static <T> SqlFilters convert(SelectFilter<T> selectFilter, FilterContext<T> context, String[] values) {
		ExtractingSqlSelect<String> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConnectorCteStep.PREPROCESSING),
				selectFilter.getColumn().getName(),
				String.class
		);

		WhereCondition condition = new MultiSelectCondition(
				context.getConceptTables().qualifyOnPredecessor(ConnectorCteStep.EVENT_FILTER, rootSelect.aliased()),
				values,
				context.getParentContext().getSqlDialect().getFunctionProvider()
		);

		return new SqlFilters(
				SqlSelects.builder()
						  .preprocessingSelect(rootSelect)
						  .build(),
				WhereClauses.builder()
							.eventFilter(condition)
							.build()
		);
	}

}
