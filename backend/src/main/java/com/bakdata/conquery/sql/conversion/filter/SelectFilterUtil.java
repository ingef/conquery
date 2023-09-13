package com.bakdata.conquery.sql.conversion.filter;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;

class SelectFilterUtil {

	public static <T> ConceptFilter convert(SelectFilter<T> selectFilter, FilterContext<T> context, String[] values) {

		ConquerySelect rootSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				selectFilter.getColumn().getName(),
				String.class
		);

		FilterCondition condition = new MultiSelectCondition(
				context.getConceptTables().qualifyOnPredecessorTableName(CteStep.EVENT_FILTER, rootSelect.aliased()),
				values,
				context.getParentContext().getSqlDialect().getFunction()
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
