package com.bakdata.conquery.sql.compiler.ir.interval;

import java.util.List;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;

/** Builds the side branch that aggregates interval-based select expressions. */
@UtilityClass
public class IntervalPackingSelectCompiler {

	/**
	 * Compile one aggregation over an arbitrary interval-packed date range.
	 *
	 * <p>TODO Unify this legacy single-select path with {@link #compile(QueryStep, List, SqlTables)} once arbitrary
	 * date-range selects are represented by the common resolved model.</p>
	 */
	public static QueryStep compileArbitrarySelect(
			QueryStep predecessor,
			ColumnDateRange dateRange,
			SqlSelect select,
			SqlTables tables,
			CompilerDialect dialect
	) {
		List<QueryStep> predecessors = List.of(predecessor);
		QueryStep directPredecessor = predecessor;
		if (dialect.supportsSingleColumnRanges()) {
			directPredecessor = dialect.unnestDateRange(
					dateRange,
					predecessor,
					tables.cteName(ConceptCteStep.UNNEST_DATE)
			);
			predecessors = List.of(predecessor, directPredecessor);
		}

		Selects predecessorSelects = directPredecessor.getQualifiedSelects();
		Selects selects = Selects.builder()
				.ids(predecessorSelects.getIds())
				.sqlSelect(select)
				.build();

		return QueryStep.builder()
				.cteName(tables.cteName(ConceptCteStep.INTERVAL_PACKING_SELECTS))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(directPredecessor.getCteName()))
				.groupBy(predecessorSelects.getIds().toFields())
				.predecessors(predecessors)
				.build();
	}

	public static QueryStep compile(
			QueryStep predecessor,
			List<SqlSelect> intervalPackingSelects,
			SqlTables tables
	) {
		if (intervalPackingSelects.isEmpty()) {
			return predecessor;
		}

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		if (predecessorSelects.getValidityDate().isEmpty()) {
			throw new IllegalArgumentException("Cannot compile interval-packing selects without a validity date");
		}
		Selects selects = Selects.builder()
				.ids(predecessorSelects.getIds())
				.sqlSelects(intervalPackingSelects)
				.build();

		return QueryStep.builder()
				.cteName(tables.cteName(ConceptCteStep.INTERVAL_PACKING_SELECTS))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
				.groupBy(predecessorSelects.getIds().toFields())
				.predecessors(List.of())
				.build();
	}
}
