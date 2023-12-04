package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.FilterCondition;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

class SelectFilterUtil {

	public static <T> ConceptFilter convert(SelectFilter<T> selectFilter, FilterContext<T> context, String[] values) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				selectFilter.getColumn().getName(),
				String.class
		);

		FilterCondition condition = new MultiSelectCondition(
				context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, rootSelect.aliased()),
				values,
				context.getParentContext().getSqlDialect().getFunctionProvider()
		);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(List.of(rootSelect))
						  .build(),
				Filters.builder()
					   .event(List.of(condition))
					   .build()
		);
	}

}
