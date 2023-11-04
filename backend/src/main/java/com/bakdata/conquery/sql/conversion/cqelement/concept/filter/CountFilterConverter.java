package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class CountFilterConverter implements FilterConverter<Range.LongRange, CountFilter> {

	@Override
	public ConceptFilter convert(CountFilter countFilter, FilterContext<Range.LongRange> context) {

		ExtractingSqlSelect<Object> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				countFilter.getColumn().getName(),
				Object.class
		);

		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		CountSqlSelect countSqlSelect = new CountSqlSelect(
				qualifiedRootSelect,
				countFilter.getName(),
				CountSqlSelect.CountType.fromBoolean(countFilter.isDistinct())
		);

		Field<Object> qualifiedCountGroupBy = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_FILTER, countSqlSelect.aliased());
		CountCondition countFilterCondition = new CountCondition(qualifiedCountGroupBy, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .forAggregationSelectStep(Collections.singletonList(countSqlSelect))
						  .build(),
				Filters.builder()
					   .group(Collections.singletonList(countFilterCondition))
					   .build()
		);
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		Set<ConceptCteStep> countFilterSteps = new HashSet<>(FilterConverter.super.requiredSteps());
		countFilterSteps.add(ConceptCteStep.AGGREGATION_FILTER);
		return countFilterSteps;

	}

	@Override
	public Class<CountFilter> getConversionClass() {
		return CountFilter.class;
	}

}
