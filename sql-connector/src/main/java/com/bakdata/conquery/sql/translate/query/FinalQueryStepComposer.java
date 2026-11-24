package com.bakdata.conquery.sql.translate.query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.ProjectionMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Builds the final aggregated query-step projection from compiled connector IR. */
@UtilityClass
public final class FinalQueryStepComposer {

	/**
	 * Compose a final, non-CTE query step.
	 *
	 * @param preFinalStep last step of the compiled query-node graph
	 * @param additionalStep optional step whose explicit selects are joined to the result by entity ID
	 * @param includeValidityDate whether the result should retain its aggregated validity date
	 * @param translatorDialect dialect capabilities used to aggregate explicit selects
	 */
	public static QueryStep compose(
			QueryStep preFinalStep,
			Optional<QueryStep> additionalStep,
			boolean includeValidityDate,
			TranslatorDialect translatorDialect
	) {
		return compose(preFinalStep, additionalStep, includeValidityDate, translatorDialect, null);
	}

	/**
	 * Compose a named intermediate relation for use as another query's source.
	 *
	 * <p>Unlike a public final projection, validity remains represented by two physical date columns.</p>
	 */
	public static QueryStep composeReusable(
			QueryStep preFinalStep,
			Optional<QueryStep> additionalStep,
			boolean includeValidityDate,
			TranslatorDialect translatorDialect,
			String cteName
	) {
		Selects combined = combineSelects(preFinalStep, additionalStep);
		Optional<ColumnDateRange> validityDate = includeValidityDate
				? Optional.of(combined.getValidityDate().orElseGet(translatorDialect::emptyDateRange)
					.asValidityDateRange(cteName))
				: Optional.empty();
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(combined.getIds().getPrimaryColumn()))
				.validityDate(validityDate)
				.build();
		List<QueryStep> predecessors = Stream.concat(Stream.of(preFinalStep), additionalStep.stream()).toList();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(finalTable(preFinalStep, additionalStep))
				.selectDistinct(true)
				.predecessors(predecessors)
				.build();
	}

	/** Compose a final query step that can optionally be reused as a named CTE. */
	public static QueryStep compose(
			QueryStep preFinalStep,
			Optional<QueryStep> additionalStep,
			boolean includeValidityDate,
			TranslatorDialect translatorDialect,
			String cteName
	) {
		Selects preFinalSelects = combineSelects(preFinalStep, additionalStep);
		List<QueryStep> predecessors = Stream.concat(Stream.of(preFinalStep), additionalStep.stream()).toList();

		return QueryStep.builder()
				.cteName(cteName)
				.projectionMode(ProjectionMode.AGGREGATED)
				.selects(finalSelects(preFinalSelects, includeValidityDate, translatorDialect))
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
			TranslatorDialect translatorDialect
	) {
		Optional<ColumnDateRange> validityDate = includeValidityDate
				? Optional.of(preFinalSelects.getValidityDate().orElseGet(translatorDialect::emptyDateRange))
				: Optional.empty();

		return Selects.builder()
				.ids(preFinalSelects.getIds())
				.validityDate(validityDate)
				.stratificationDate(preFinalSelects.getStratificationDate())
				.sqlSelects(aggregateExplicitSelects(preFinalSelects, translatorDialect))
				.build();
	}

	private static List<? extends FieldWrapper<?>> aggregateExplicitSelects(
			Selects finalSelects,
			TranslatorDialect translatorDialect
	) {
		return finalSelects.getSqlSelects().stream()
				.flatMap(sqlSelect -> sqlSelect.aggregateForFinalQuery(translatorDialect).stream())
				.map(FinalQueryStepComposer::toFieldWrapper)
				.toList();
	}

	private static FieldWrapper<?> toFieldWrapper(Field<?> field) {
		return new FieldWrapper<>(field);
	}
}
