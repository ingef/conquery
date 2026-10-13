package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import org.jooq.Field;
import org.jooq.impl.DSL;

final class BuiltInAggregationConverters {

	private BuiltInAggregationConverters() {
	}

	static List<Converter<? extends ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext>> create() {
		return List.of(
				converter(BuiltInAggregations.Count.class, BuiltInAggregationConverters::convertCount),
				converter(BuiltInAggregations.Sum.class, SumAggregationConverter::convert),
				converter(BuiltInAggregations.Flags.class, FlagSqlAggregator::convert),
				converter(BuiltInAggregations.CountQuarters.class, DateAggregationConverters::countQuarters),
				converter(BuiltInAggregations.DurationSum.class, DateAggregationConverters::durationSum)
		);
	}

	// TODO Why is this not in a separate file?
	private static CommonAggregationSelect<Integer> convertCount(
			BuiltInAggregations.Count aggregation,
			AggregationConversionContext context
	) {
		ExtractingSqlSelect<Object> countSelect = rootSelect(aggregation.column(), context);
		List<ExtractingSqlSelect<Object>> distinctSelects = aggregation.distinctBy().stream()
				.map(column -> rootSelect(column, context))
				.toList();
		List<SingleColumnSqlSelect> rootSelects = Stream.concat(Stream.of(countSelect), distinctSelects.stream())
				.distinct()
				.map(SingleColumnSqlSelect.class::cast)
				.toList();

		String aggregationSource = context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		Field<Integer> count = distinctSelects.isEmpty()
				? DSL.count(countSelect.qualify(aggregationSource).select())
				: DSL.countDistinct(distinctSelects.stream()
						.map(select -> select.qualify(aggregationSource).select())
						.toArray(Field<?>[]::new));
		String[] requiredColumns = rootSelects.stream()
				.flatMap(select -> select.requiredColumns().stream())
				.distinct()
				.toArray(String[]::new);
		FieldWrapper<Integer> groupedCount = new FieldWrapper<>(
				DSL.nullif(count, 0).as(context.alias()),
				requiredColumns
		);

		return CommonAggregationSelect.<Integer>builder()
				.rootSelects(rootSelects)
				.groupBy(groupedCount)
				.build();
	}

	private static ExtractingSqlSelect<Object> rootSelect(
			ResolvedColumn column,
			AggregationConversionContext context
	) {
		return new ExtractingSqlSelect<>(context.tables().getRootTable(), column.physicalName(), Object.class);
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
