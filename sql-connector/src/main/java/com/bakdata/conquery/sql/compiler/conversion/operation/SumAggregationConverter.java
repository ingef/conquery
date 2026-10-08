package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Builds sums, including the separate branch needed for distinct-by aggregation. */
@UtilityClass
final class SumAggregationConverter {

	private static final String ROW_NUMBER = "row_number";

	static CommonAggregationSelect<BigDecimal> convert(BuiltInAggregations.Sum aggregation, AggregationConversionContext context) {
		String root = context.tables().getRootTable();
		String source = context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		List<SingleColumnSqlSelect> roots = new ArrayList<>();
		roots.add(new ExtractingSqlSelect<>(root, aggregation.column().physicalName(), numberClass(aggregation.column())));
		// The existing distinct-sum path ignores subtraction.
		if (aggregation.distinctBy().isEmpty()) {
			aggregation.subtractColumn().ifPresent(column -> roots.add(new ExtractingSqlSelect<>(root, column.physicalName(), numberClass(column))));
		}
		aggregation.distinctBy().forEach(column -> roots.add(new ExtractingSqlSelect<>(root, column.physicalName(), Object.class)));
		// Distinct keys may also be the sum/subtract columns; project each physical column only once.
		List<SingleColumnSqlSelect> rootSelects = new ArrayList<>();
		for (SingleColumnSqlSelect select : roots) {
			if (rootSelects.stream().noneMatch(existing -> existing.select().getName().equals(select.select().getName()))) {
				rootSelects.add(select);
			}
		}

		if (aggregation.distinctBy().isEmpty()) {
			return CommonAggregationSelect.<BigDecimal>builder()
					.rootSelects(rootSelects)
					.groupBy(new FieldWrapper<>(DSL.sum(value(aggregation, source)).as(context.alias()),
							rootSelects.stream().map(select -> select.select().getName()).toArray(String[]::new)))
					.build();
		}

		SqlIdColumns ids = context.ids().qualify(source);
		List<Field<?>> partition = new ArrayList<>(ids.toFields());
		aggregation.distinctBy().stream().map(column -> DSL.field(DSL.name(source, column.physicalName()))).forEach(partition::add);
		FieldWrapper<Integer> rowNumber = new FieldWrapper<>(
				DSL.rowNumber().over(DSL.partitionBy(partition)).as(ROW_NUMBER),
				partition.stream().map(Field::getName).toArray(String[]::new)
		);
		List<SingleColumnSqlSelect> values = new ArrayList<>();
		values.add(new ExtractingSqlSelect<>(source, aggregation.column().physicalName(), numberClass(aggregation.column())));
		values.add(rowNumber);
		QueryStep numbered = QueryStep.builder()
				.cteName(context.nameGenerator().cteStepName(SumStep.ROW_NUMBER_ASSIGNED, context.alias()))
				.selects(Selects.builder().ids(ids).sqlSelects(values).build())
				.fromTable(QueryStep.toTableLike(source))
				.build();
		FieldWrapper<BigDecimal> sum = new FieldWrapper<>(
				DSL.sum(DSL.coalesce(field(aggregation.column(), numbered.getCteName()), DSL.inline(0))).as(context.alias())
		);
		SqlIdColumns numberedIds = ids.qualify(numbered.getCteName());
		QueryStep summed = QueryStep.builder()
				.cteName(context.nameGenerator().cteStepName(SumStep.ROW_NUMBER_FILTERED, context.alias()))
				.selects(Selects.builder().ids(numberedIds).sqlSelect(sum).build())
				.fromTable(QueryStep.toTableLike(numbered.getCteName()))
				.conditions(List.of(DSL.field(DSL.name(numbered.getCteName(), ROW_NUMBER), Integer.class).eq(DSL.inline(1))))
				.groupBy(numberedIds.toFields())
				.predecessor(numbered)
				.build();
		return CommonAggregationSelect.<BigDecimal>builder()
				.rootSelects(rootSelects)
				.groupBy(sum)
				.additionalPredecessor(summed)
				.build();
	}

	private static Field<? extends Number> value(BuiltInAggregations.Sum aggregation, String source) {
		Field<? extends Number> value = field(aggregation.column(), source);
		if (aggregation.subtractColumn().isEmpty()) {
			return value;
		}
		Field<? extends Number> subtract = field(aggregation.subtractColumn().orElseThrow(), source);
		// Preserve missing values when both inputs are null, but treat a single missing operand as zero.
		Field<? extends Number> zeroIfAnyNonNull = DSL.coalesce(value.multiply(0), subtract.multiply(0));
		return DSL.coalesce(value, zeroIfAnyNonNull).minus(DSL.coalesce(subtract, zeroIfAnyNonNull));
	}

	private static Field<? extends Number> field(ResolvedColumn column, String source) {
		return DSL.field(DSL.name(source, column.physicalName()), numberClass(column));
	}

	private static Class<? extends Number> numberClass(ResolvedColumn column) {
		return switch (column.type()) {
			case INTEGER -> Integer.class;
			case REAL, DECIMAL -> Double.class;
			case MONEY -> BigDecimal.class;
			default -> throw new IllegalArgumentException("Not a numeric column: " + column.type());
		};
	}

	private enum SumStep implements CteStep {
		ROW_NUMBER_ASSIGNED,
		ROW_NUMBER_FILTERED;

		@Override
		public String getSuffix() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
