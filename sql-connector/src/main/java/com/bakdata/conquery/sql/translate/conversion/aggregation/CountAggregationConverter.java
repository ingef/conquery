package com.bakdata.conquery.sql.translate.conversion.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.model.operation.aggregation.CountAggregation;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.translate.ir.CteStep;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.translate.query.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import org.jooq.Field;
import org.jooq.impl.DSL;

final class CountAggregationConverter {

	static CommonAggregationSelect<Integer> convert(
			CountAggregation aggregation,
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
		String[] requiredColumns = rootSelects.stream()
				.flatMap(select -> select.requiredColumns().stream())
				.distinct()
				.toArray(String[]::new);
		if (distinctSelects.isEmpty()) {
			final FieldWrapper<Integer> groupedCount = new FieldWrapper<>(
					DSL.nullif(DSL.count(countSelect.qualify(aggregationSource).select()), 0).as(context.alias()),
					requiredColumns);
			return CommonAggregationSelect.<Integer>builder()
					.rootSelects(rootSelects)
					.groupBy(groupedCount)
					.build();
		}

		final SqlIdColumns sourceIds = context.ids().qualify(aggregationSource);
		final List<Field<?>> partition = new ArrayList<>(sourceIds.toFields());
		distinctSelects.stream().map(select -> select.qualify(aggregationSource).select()).forEach(partition::add);
		final FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partition)).as("row_number"), requiredColumns);
		final List<SingleColumnSqlSelect> numberedValues = Stream.concat(
				rootSelects.stream().map(select -> select.qualify(aggregationSource)), Stream.of(rowNumber)).toList();
		final QueryStep numbered = QueryStep.builder()
				.cteName(context.nameGenerator().cteStepName(CountStep.ROW_NUMBER_ASSIGNED, context.alias()))
				.selects(Selects.builder().ids(sourceIds).sqlSelects(numberedValues).build())
				.fromTable(QueryStep.toTableLike(aggregationSource))
				.build();

		final Field<Integer> qualifiedRowNumber = DSL.field(DSL.name(numbered.getCteName(), "row_number"), Integer.class);
		final Field<?> qualifiedCount = DSL.field(DSL.name(numbered.getCteName(), aggregation.column().physicalName()));
		final org.jooq.Condition anyKeyPresent = aggregation.distinctBy().stream()
				.map(column -> DSL.field(DSL.name(numbered.getCteName(), column.physicalName())).isNotNull())
				.reduce(DSL.falseCondition(), org.jooq.Condition::or);
		final Field<?> countValue = DSL.when(qualifiedRowNumber.eq(1).and(anyKeyPresent), qualifiedCount);
		final FieldWrapper<Integer> groupedCount = new FieldWrapper<>(
				DSL.nullif(DSL.count(countValue), 0).as(context.alias()));
		final SqlIdColumns numberedIds = sourceIds.qualify(numbered.getCteName());
		final QueryStep counted = QueryStep.builder()
				.cteName(context.nameGenerator().cteStepName(CountStep.DISTINCT_COUNTED, context.alias()))
				.selects(Selects.builder().ids(numberedIds).sqlSelect(groupedCount).build())
				.fromTable(QueryStep.toTableLike(numbered.getCteName()))
				.groupBy(numberedIds.toFields())
				.predecessor(numbered)
				.build();

		return CommonAggregationSelect.<Integer>builder()
				.rootSelects(rootSelects)
				.groupBy(groupedCount)
				.additionalPredecessor(counted)
				.build();
	}


	private static ExtractingSqlSelect<Object> rootSelect(
			ResolvedColumn column,
			AggregationConversionContext context
	) {
		return new ExtractingSqlSelect<>(context.tables().getRootTable(), column.physicalName(), Object.class);
	}

	private enum CountStep implements CteStep {
		ROW_NUMBER_ASSIGNED,
		DISTINCT_COUNTED;

		@Override
		public String getSuffix() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
