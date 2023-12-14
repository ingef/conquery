package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

class SelectFilterUtil {

	public static <T> SqlFilters convert(SelectFilter<T> selectFilter, FilterContext<T> context, String[] values) {

		ExtractingSqlSelect<String> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				selectFilter.getColumn().getName(),
				String.class
		);

		FilterCondition condition = new MultiSelectCondition(
				context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, rootSelect.aliased()),
				values,
				context.getParentContext().getSqlDialect().getFunctionProvider()
		);

		return new SqlFilters(
				SqlSelects.builder()
						  .preprocessingSelect(rootSelect)
						  .build(),
				Filters.builder()
					   .event(List.of(condition))
					   .build()
		);
	}

}
