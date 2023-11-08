package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

class FinalConceptCte extends ConceptCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<SqlSelect> forFinalStep = conceptCteContext.getSelects().stream()
														.flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
														.distinct()
														.toList();

		if (conceptCteContext.getValidityDate().isEmpty() || conceptCteContext.isExcludedFromDateAggregation()) {
			Selects finalConceptSelects = new Selects(conceptCteContext.getPrimaryColumn(), Optional.empty(), forFinalStep);
			return QueryStep.builder()
							.selects(finalConceptSelects);
		}

		return applyIntervalPacking(forFinalStep, conceptCteContext);
	}

	@Override
	protected ConceptStep cteStep() {
		return ConceptStep.FINAL;
	}

	private QueryStep.QueryStepBuilder applyIntervalPacking(List<SqlSelect> forFinalStep, ConceptCteContext conceptCteContext) {

		IntervalPackingContext intervalPackingContext = new IntervalPackingContext(
				conceptCteContext.getConceptLabel(),
				conceptCteContext.getPrimaryColumn(),
				conceptCteContext.getValidityDate().get(),
				conceptCteContext.getConceptTables()
		);

		QueryStep finalIntervalPackingStep = conceptCteContext.getConversionContext()
															  .getSqlDialect()
															  .getIntervalPacker()
															  .createIntervalPackingSteps(intervalPackingContext);

		return joinSelectsAndFiltersWithIntervalPackingStep(forFinalStep, finalIntervalPackingStep, conceptCteContext);
	}

	private QueryStep.QueryStepBuilder joinSelectsAndFiltersWithIntervalPackingStep(
			List<SqlSelect> forFinalStep,
			QueryStep finalIntervalPackingStep,
			ConceptCteContext conceptCteContext
	) {
		QueryStep finalSelectsAndFilterStep = conceptCteContext.getPrevious();
		Field<Object> primaryColumn = finalSelectsAndFilterStep.getQualifiedSelects().getPrimaryColumn();
		Optional<ColumnDateRange> validityDate = Optional.of(finalIntervalPackingStep.getQualifiedSelects().getValidityDate().get());

		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(
				List.of(finalSelectsAndFilterStep, finalIntervalPackingStep),
				LogicalOperation.AND,
				conceptCteContext.getConversionContext()
		);

		Selects finalConceptSelects = new Selects(primaryColumn, validityDate, forFinalStep);

		return QueryStep.builder()
						.selects(finalConceptSelects)
						.fromTable(joinedTable)
						.predecessors(List.of(finalSelectsAndFilterStep, finalIntervalPackingStep));
	}

}
