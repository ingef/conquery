package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

class FinalConceptCte extends ConceptCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<ExplicitSelect> forFinalStep = conceptCteContext.getSelects().stream()
															 .flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
															 .distinct()
															 .collect(Collectors.toList());

		if (conceptCteContext.getValidityDate().isEmpty() || conceptCteContext.isExcludedFromDateAggregation()) {
			Selects finalConceptSelects = Selects.builder()
												 .primaryColumn(conceptCteContext.getPrimaryColumn())
												 .explicitSelects(forFinalStep)
												 .build();
			return QueryStep.builder().selects(finalConceptSelects);
		}

		return applyIntervalPacking(forFinalStep, conceptCteContext);
	}

	@Override
	protected ConceptCteStep cteStep() {
		return ConceptCteStep.FINAL;
	}

	private QueryStep.QueryStepBuilder applyIntervalPacking(List<ExplicitSelect> forFinalStep, ConceptCteContext conceptCteContext) {

		IntervalPackingContext intervalPackingContext = new IntervalPackingContext(
				conceptCteContext.getConceptLabel(),
				conceptCteContext.getPrimaryColumn(),
				conceptCteContext.getValidityDate().get(),
				conceptCteContext.getConceptTables(),
				conceptCteContext.getNameGenerator()
		);

		QueryStep finalIntervalPackingStep = conceptCteContext.getConversionContext()
															  .getSqlDialect()
															  .getIntervalPacker()
															  .createIntervalPackingSteps(intervalPackingContext);

		return joinSelectsAndFiltersWithIntervalPackingStep(forFinalStep, finalIntervalPackingStep, conceptCteContext);
	}

	private QueryStep.QueryStepBuilder joinSelectsAndFiltersWithIntervalPackingStep(
			List<ExplicitSelect> forFinalStep,
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

		Selects finalConceptSelects = Selects.builder()
											 .primaryColumn(primaryColumn)
											 .validityDate(validityDate)
											 .explicitSelects(forFinalStep)
											 .build();

		return QueryStep.builder()
						.selects(finalConceptSelects)
						.fromTable(joinedTable)
						.predecessors(List.of(finalSelectsAndFilterStep, finalIntervalPackingStep));
	}

}
