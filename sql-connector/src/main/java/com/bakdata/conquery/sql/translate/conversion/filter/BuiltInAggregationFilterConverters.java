package com.bakdata.conquery.sql.translate.conversion.filter;

import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.conversion.aggregation.AggregationConversionContext;
import com.bakdata.conquery.sql.translate.conversion.aggregation.ResolvedAggregationConverter;
import com.bakdata.conquery.sql.translate.ir.condition.InclusiveRangeCondition;
import com.bakdata.conquery.sql.translate.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.translate.query.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.translate.query.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.translate.query.concept.SqlFilters;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import org.jooq.Field;

final class BuiltInAggregationFilterConverters {

	private BuiltInAggregationFilterConverters() {
	}

	static List<Converter<? extends ResolvedFilter, SqlFilters, FilterConversionContext>> create(
			ResolvedAggregationConverter aggregationConverter
	) {
		return List.of(new Converter<BuiltInFilters.AggregationRange, SqlFilters, FilterConversionContext>() {
			@Override
			public Class<BuiltInFilters.AggregationRange> getConversionClass() {
				return BuiltInFilters.AggregationRange.class;
			}

			@Override
			public SqlFilters convert(BuiltInFilters.AggregationRange filter, FilterConversionContext context) {
				return convertAggregationRange(filter, context, aggregationConverter);
			}
		});
	}

	private static SqlFilters convertAggregationRange(
			BuiltInFilters.AggregationRange filter,
			FilterConversionContext context,
			ResolvedAggregationConverter aggregationConverter
	) {
		String alias = context.nameGenerator().filterName(filter);
		AggregationConversionContext aggregationContext = new AggregationConversionContext(
				context.dialect(),
				context.nameGenerator(),
				context.tables(),
				context.ids(),
				alias
		);
		CommonAggregationSelect<?> aggregation = aggregationConverter.convert(filter.aggregation(), aggregationContext);
		if (!Number.class.isAssignableFrom(aggregation.getGroupBy().select().getType())) {
			throw new IllegalArgumentException("Aggregation range filters require a numeric aggregation result");
		}

		String groupFilterSource = context.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		Field<BigDecimal> aggregationField = aggregation.getGroupBy()
				.qualify(groupFilterSource)
				.select()
				.coerce(BigDecimal.class);
		ConnectorSqlSelects.ConnectorSqlSelectsBuilder selects = ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregation.getRootSelects())
				.additionalPredecessor(aggregation.getAdditionalPredecessor());
		if (aggregation.getAdditionalPredecessor().isEmpty()) {
			selects.aggregationSelect(aggregation.getGroupBy());
		}

		return new SqlFilters(
				selects.build(),
				WhereClauses.builder()
						.groupFilter(new InclusiveRangeCondition<>(aggregationField, filter.range()))
						.build()
		);
	}
}
