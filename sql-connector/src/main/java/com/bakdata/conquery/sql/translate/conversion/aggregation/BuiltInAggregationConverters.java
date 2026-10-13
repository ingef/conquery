package com.bakdata.conquery.sql.translate.conversion.aggregation;

import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.query.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;

final class BuiltInAggregationConverters {

	private BuiltInAggregationConverters() {
	}

	static List<Converter<? extends ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext>> create() {
		return List.of(
				converter(BuiltInAggregations.Count.class, CountAggregationConverter::convert),
				converter(BuiltInAggregations.Sum.class, SumAggregationConverter::convert),
				converter(BuiltInAggregations.Flags.class, FlagSqlAggregatorConverter::convert),
				converter(BuiltInAggregations.CountQuarters.class, DateAggregationConverters::countQuarters),
				converter(BuiltInAggregations.DurationSum.class, DateAggregationConverters::durationSum)
		);
	}

	private static <I extends ResolvedAggregation> Converter<I, CommonAggregationSelect<?>, AggregationConversionContext> converter(
			Class<I> type,
			BiFunction<I, AggregationConversionContext, CommonAggregationSelect<?>> conversion
	) {
		return new Converter<>() {
			@Override
			public Class<I> getConversionClass() {
				return type;
			}

			@Override
			public CommonAggregationSelect<?> convert(I input, AggregationConversionContext context) {
				return conversion.apply(input, context);
			}
		};
	}
}
