package com.bakdata.conquery.sql.translate.conversion.aggregation;

import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.model.operation.CountAggregation;
import com.bakdata.conquery.sql.model.operation.CountQuartersAggregation;
import com.bakdata.conquery.sql.model.operation.DurationSumAggregation;
import com.bakdata.conquery.sql.model.operation.FlagsAggregation;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.operation.SumAggregation;
import com.bakdata.conquery.sql.translate.conversion.Converter;
import com.bakdata.conquery.sql.translate.query.concept.CommonAggregationSelect;
import lombok.experimental.UtilityClass;

@UtilityClass
class BuiltInAggregationConverters {

	static List<Converter<? extends ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext>> create() {
		return List.of(
				converter(CountAggregation.class, CountAggregationConverter::convert),
				converter(SumAggregation.class, SumAggregationConverter::convert),
				converter(FlagsAggregation.class, FlagSqlAggregatorConverter::convert),
				converter(CountQuartersAggregation.class, DateAggregationConverters::countQuarters),
				converter(DurationSumAggregation.class, DateAggregationConverters::durationSum)
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
