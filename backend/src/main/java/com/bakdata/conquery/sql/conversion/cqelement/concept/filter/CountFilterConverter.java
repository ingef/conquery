package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class CountFilterConverter implements FilterConverter<Range.LongRange, CountFilter> {

	@Override
	public ConceptFilter convert(CountFilter countFilter, FilterContext<Range.LongRange> context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				countFilter.getColumn().getName(),
				Object.class
		);

		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		CountSqlSelect countSqlSelect = new CountSqlSelect(qualifiedRootSelect, countFilter.getName(), CountSqlSelect.CountType.fromBoolean(countFilter.isDistinct()));

		Field<Object> qualifiedCountGroupBy = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_FILTER, countSqlSelect.aliased());
		CountCondition countFilterCondition = new CountCondition(qualifiedCountGroupBy, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(List.of(rootSelect))
						  .forAggregationSelectStep(List.of(countSqlSelect))
						  .build(),
				Filters.builder()
					   .group(List.of(countFilterCondition))
					   .build()
		);
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.AGGREGATION_FILTER);
	}

	@Override
	public Class<CountFilter> getConversionClass() {
		return CountFilter.class;
	}

}
