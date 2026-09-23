package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.sql.compiler.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import org.jooq.Field;

/** Dispatches resolved output operations to their SQL helpers. */
public final class ResolvedSelectConverter {

	private final ConversionDispatcher<ResolvedSelect, ConnectorSqlSelects, SelectConversionContext> dispatcher;

	public ResolvedSelectConverter() {
		this(List.of());
	}

	public ResolvedSelectConverter(List<? extends Converter<? extends ResolvedSelect, ConnectorSqlSelects, SelectConversionContext>> extensions) {
		List<Converter<? extends ResolvedSelect, ConnectorSqlSelects, SelectConversionContext>> converters = new ArrayList<>(List.of(
				converter(BuiltInSelects.Values.class, ResolvedSelectConverter::values),
				converter(BuiltInSelects.ConceptValues.class, new ConceptColumnSelectConverter()::connectorSelect),
				converter(BuiltInSelects.DateUnion.class, (select, context) -> DaterangeSelectUtil.createForSelect(select.dates(),
						(dates, alias, dialect) -> new FieldWrapper<>(dialect.aggregateDateRanges(dates.getStart(), dates.getEnd()).as(alias)), context)),
				converter(BuiltInSelects.DateDistance.class, DateDistanceSqlAggregator::connectorSelect),
				converter(BuiltInSelects.EventDateUnion.class, new EventDateUnionSelectConverter()::connectorSelect),
				converter(BuiltInSelects.EventDurationSum.class, new EventDurationSumSelectConverter()::connectorSelect),
				converter(BuiltInSelects.Aggregation.class, ResolvedSelectConverter::aggregation),
				converter(BuiltInSelects.Exists.class, (select, context) -> ConnectorSqlSelects.builder().finalSelect(ExistsSqlSelect.withAlias(context.alias())).build())
		));
		converters.addAll(extensions);
		dispatcher = new ConversionDispatcher<>(converters);
	}

	public ConnectorSqlSelects convert(ResolvedSelect select, SelectConversionContext context) {
		return dispatcher.convert(select, context);
	}

	public ConceptSqlSelects conceptSelect(ResolvedSelect select, SelectConversionContext context) {
		if (select instanceof BuiltInSelects.ConceptValues values) {
			return new ConceptColumnSelectConverter().conceptSelect(values, context);
		}
		if (select instanceof BuiltInSelects.EventDateUnion union) {
			return new EventDateUnionSelectConverter().conceptSelect(union, context);
		}
		if (select instanceof BuiltInSelects.EventDurationSum duration) {
			return new EventDurationSumSelectConverter().conceptSelect(duration, context);
		}
		if (select instanceof BuiltInSelects.Exists) {
			return ConceptSqlSelects.builder().finalSelect(ExistsSqlSelect.withAlias(context.alias())).build();
		}
		throw new IllegalStateException("No concept select converter found for " + select);
	}

	private static ConnectorSqlSelects values(BuiltInSelects.Values select, SelectConversionContext context) {
		return switch (select.operation()) {
			case FIRST -> ValueSelectUtil.createValueSelect(select.column(), context.alias(), Field::asc, select.substring(), context);
			case LAST -> ValueSelectUtil.createValueSelect(select.column(), context.alias(), Field::desc, select.substring(), context);
			case RANDOM -> RandomValueSelectConverter.connectorSelect(select, context);
			case DISTINCT -> context.dialect().distinctSelect(select, context);
		};
	}

	private static ConnectorSqlSelects aggregation(BuiltInSelects.Aggregation select, SelectConversionContext context) {
		var aggregation = new ResolvedAggregationConverter().convert(select.aggregation(), new AggregationConversionContext(
				context.dialect(), context.nameGenerator(), context.tables(), context.ids(), context.alias()));
		var result = ConnectorSqlSelects.builder().preprocessingSelects(aggregation.getRootSelects())
				.additionalPredecessor(aggregation.getAdditionalPredecessor())
				.finalSelect(aggregation.getGroupBy().qualify(context.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER)));
		if (aggregation.getAdditionalPredecessor().isEmpty()) {
			result.aggregationSelect(aggregation.getGroupBy());
		}
		return result.build();
	}

	private static <I extends ResolvedSelect> Converter<I, ConnectorSqlSelects, SelectConversionContext> converter(
			Class<I> type, BiFunction<I, SelectConversionContext, ConnectorSqlSelects> conversion) {
		return new Converter<>() {
			@Override public Class<I> getConversionClass() { return type; }
			@Override public ConnectorSqlSelects convert(I input, SelectConversionContext context) { return conversion.apply(input, context); }
		};
	}
}
