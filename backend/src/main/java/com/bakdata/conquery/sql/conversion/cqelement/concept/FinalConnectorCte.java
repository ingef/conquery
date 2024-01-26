package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

class FinalConnectorCte extends ConnectorCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext CQTableContext) {

		List<SqlSelect> forFinalStep = CQTableContext.getSelects().stream()
													 .flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
													 .distinct()
													 .toList();

		if (CQTableContext.getValidityDate().isEmpty() || CQTableContext.isExcludedFromDateAggregation()) {
			Selects finalConceptSelects = Selects.builder()
												 .primaryColumn(CQTableContext.getPrimaryColumn())
												 .sqlSelects(forFinalStep)
												 .build();
			return QueryStep.builder()
							.selects(finalConceptSelects);
		}

		return applyIntervalPacking(forFinalStep, CQTableContext);
	}

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.FINAL;
	}

	private QueryStep.QueryStepBuilder applyIntervalPacking(List<SqlSelect> forFinalStep, CQTableContext CQTableContext) {

		String conceptLabel = CQTableContext.getConceptLabel();
		IntervalPackingTables intervalPackingTables =
				IntervalPackingTables.forConcept(conceptLabel, CQTableContext.getConceptTables(), CQTableContext.getNameGenerator());

		IntervalPackingContext intervalPackingContext =
				IntervalPackingContext.builder()
									  .nodeLabel(conceptLabel)
									  .primaryColumn(CQTableContext.getPrimaryColumn())
									  .validityDate(CQTableContext.getValidityDate().get())
									  .intervalPackingTables(intervalPackingTables)
									  .build();

		QueryStep finalIntervalPackingStep = CQTableContext.getConversionContext()
														   .getSqlDialect()
														   .getIntervalPacker()
														   .createIntervalPackingSteps(intervalPackingContext);

		return joinSelectsAndFiltersWithIntervalPackingStep(forFinalStep, finalIntervalPackingStep, CQTableContext);
	}

	private QueryStep.QueryStepBuilder joinSelectsAndFiltersWithIntervalPackingStep(
			List<SqlSelect> forFinalStep,
			QueryStep finalIntervalPackingStep,
			CQTableContext CQTableContext
	) {
		QueryStep finalSelectsAndFilterStep = CQTableContext.getPrevious();
		Field<Object> primaryColumn = finalSelectsAndFilterStep.getQualifiedSelects().getPrimaryColumn();
		Optional<ColumnDateRange> validityDate = Optional.of(finalIntervalPackingStep.getQualifiedSelects().getValidityDate().get());

		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(
				List.of(finalSelectsAndFilterStep, finalIntervalPackingStep),
				LogicalOperation.AND,
				CQTableContext.getConversionContext()
		);

		Selects finalConceptSelects = Selects.builder()
											 .primaryColumn(primaryColumn)
											 .validityDate(validityDate)
											 .sqlSelects(forFinalStep)
											 .build();

		return QueryStep.builder()
						.selects(finalConceptSelects)
						.fromTable(joinedTable)
						.predecessors(List.of(finalSelectsAndFilterStep, finalIntervalPackingStep));
	}

}
