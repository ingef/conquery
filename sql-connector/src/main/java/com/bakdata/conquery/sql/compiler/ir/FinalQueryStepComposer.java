package com.bakdata.conquery.sql.compiler.ir;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Builds the final aggregated query-step projection from compiled connector IR. */
public final class FinalQueryStepComposer {

	private FinalQueryStepComposer() {
	}

	/**
	 * Compose a final, non-CTE query step.
	 *
	 * @param preFinalStep last step of the compiled query-node graph
	 * @param additionalStep optional step whose explicit selects are joined to the result by entity ID
	 * @param includeValidityDate whether the result should retain its aggregated validity date
	 * @param compilerDialect dialect capabilities used to aggregate explicit selects
	 */
	public static QueryStep compose(
			QueryStep preFinalStep,
			Optional<QueryStep> additionalStep,
			boolean includeValidityDate,
			CompilerDialect compilerDialect
	) {
		Selects preFinalSelects = combineSelects(preFinalStep, additionalStep);
		List<QueryStep> predecessors = Stream.concat(Stream.of(preFinalStep), additionalStep.stream()).toList();

		return QueryStep.builder()
				.cteName(null)
				.projectionMode(ProjectionMode.AGGREGATED)
				.selects(finalSelects(preFinalSelects, includeValidityDate, compilerDialect))
				.fromTable(finalTable(preFinalStep, additionalStep))
				.groupBy(preFinalSelects.getIds().toFields())
				.predecessors(predecessors)
				.build();
	}

	private static Selects combineSelects(QueryStep preFinalStep, Optional<QueryStep> additionalStep) {
		Selects preFinalSelects = preFinalStep.getQualifiedSelects();
		if (additionalStep.isEmpty()) {
			return preFinalSelects;
		}
		List<SqlSelect> combinedExplicitSelects = Stream.concat(
				preFinalSelects.getSqlSelects().stream(),
				additionalStep.orElseThrow().getQualifiedSelects().getSqlSelects().stream()
		).toList();
		return preFinalSelects.toBuilder()
				.clearSqlSelects()
				.sqlSelects(combinedExplicitSelects)
				.build();
	}

	private static TableLike<Record> finalTable(QueryStep preFinalStep, Optional<QueryStep> additionalStep) {
		if (additionalStep.isEmpty()) {
			return QueryStep.toTableLike(preFinalStep.getCteName());
		}
		return QueryStepJoiner.join(List.of(preFinalStep, additionalStep.orElseThrow()), JoinMode.INNER);
	}

	private static Selects finalSelects(
			Selects preFinalSelects,
			boolean includeValidityDate,
			CompilerDialect compilerDialect
	) {
		Optional<ColumnDateRange> validityDate = includeValidityDate
				? Optional.of(preFinalSelects.getValidityDate().orElseGet(compilerDialect::emptyDateRange))
				: Optional.empty();

		return Selects.builder()
				.ids(preFinalSelects.getIds())
				.validityDate(validityDate)
				.stratificationDate(preFinalSelects.getStratificationDate())
				.sqlSelects(aggregateExplicitSelects(preFinalSelects, compilerDialect))
				.build();
	}

	private static List<? extends FieldWrapper<?>> aggregateExplicitSelects(
			Selects finalSelects,
			CompilerDialect compilerDialect
	) {
		return finalSelects.getSqlSelects().stream()
				.flatMap(sqlSelect -> sqlSelect.aggregateForFinalQuery(compilerDialect).stream())
				.map(FinalQueryStepComposer::toFieldWrapper)
				.toList();
	}

	private static FieldWrapper<?> toFieldWrapper(Field<?> field) {
		return new FieldWrapper<>(field);
	}
}
