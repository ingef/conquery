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
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forFinalStep = tableContext.allSqlSelects().stream()
												   .flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
												   .distinct()
												   .toList();

		if (tableContext.getValidityDate().isEmpty() || tableContext.isExcludedFromDateAggregation()) {
			Selects finalConceptSelects = Selects.builder()
												 .primaryColumn(tableContext.getPrimaryColumn())
												 .sqlSelects(forFinalStep)
												 .build();
			return QueryStep.builder()
							.selects(finalConceptSelects);
		}

		return applyIntervalPacking(forFinalStep, tableContext);
	}

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.FINAL;
	}

	private QueryStep.QueryStepBuilder applyIntervalPacking(List<SqlSelect> forFinalStep, CQTableContext tableContext) {

		String conceptLabel = tableContext.getConceptLabel();
		IntervalPackingTables intervalPackingTables =
				IntervalPackingTables.forConcept(conceptLabel, tableContext.getConceptTables(), tableContext.getNameGenerator());

		IntervalPackingContext intervalPackingContext =
				IntervalPackingContext.builder()
									  .nodeLabel(conceptLabel)
									  .primaryColumn(tableContext.getPrimaryColumn())
									  .validityDate(tableContext.getValidityDate().get())
									  .intervalPackingTables(intervalPackingTables)
									  .build();

		QueryStep finalIntervalPackingStep = tableContext.getConversionContext()
														 .getSqlDialect()
														 .getIntervalPacker()
														 .createIntervalPackingSteps(intervalPackingContext);

		return joinSelectsAndFiltersWithIntervalPackingStep(forFinalStep, finalIntervalPackingStep, tableContext);
	}

	private QueryStep.QueryStepBuilder joinSelectsAndFiltersWithIntervalPackingStep(
			List<SqlSelect> forFinalStep,
			QueryStep finalIntervalPackingStep,
			CQTableContext tableContext
	) {
		QueryStep finalSelectsAndFilterStep = tableContext.getPrevious();
		Field<Object> primaryColumn = finalSelectsAndFilterStep.getQualifiedSelects().getPrimaryColumn();
		Optional<ColumnDateRange> validityDate = Optional.of(finalIntervalPackingStep.getQualifiedSelects().getValidityDate().get());

		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(
				List.of(finalSelectsAndFilterStep, finalIntervalPackingStep),
				LogicalOperation.AND,
				tableContext.getConversionContext()
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
