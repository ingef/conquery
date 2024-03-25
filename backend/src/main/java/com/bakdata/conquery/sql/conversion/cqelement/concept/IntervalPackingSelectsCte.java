package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.google.common.base.Preconditions;

class IntervalPackingSelectsCte {

	public static QueryStep forConnector(QueryStep predecessor, CQTableContext cqTableContext) {
		return create(
				predecessor,
				cqTableContext.getSqlSelects(),
				cqTableContext.getConnectorTables(),
				cqTableContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

	public static QueryStep forConcept(
			QueryStep predecessor,
			SqlTables tables,
			List<SqlSelects> sqlSelects,
			ConversionContext conversionContext
	) {
		return create(
				predecessor,
				sqlSelects,
				tables,
				conversionContext.getSqlDialect().getFunctionProvider()
		);
	}

	/**
	 * @param predecessor The preceding query step which must contain an aggregated validity date.
	 * @param sqlSelects  {@link SqlSelects} who's interval packing selects will be part of the returned {@link QueryStep}.
	 * @return A {@link QueryStep} containing converted interval packing selects, like {@link EventDurationSumSelect}, {@link EventDateUnionSelect}, etc.
	 * Returns the given predecessor as is if there are no interval packing selects in the given list of {@link SqlSelects}.
	 */
	private static QueryStep create(
			QueryStep predecessor,
			List<SqlSelects> sqlSelects,
			SqlTables tables,
			SqlFunctionProvider functionProvider
	) {
		List<SqlSelect> intervalPackingSelects = sqlSelects.stream().flatMap(selects -> selects.getIntervalPackingSelects().stream()).toList();
		if (intervalPackingSelects.isEmpty()) {
			return predecessor;
		}

		Preconditions.checkArgument(
				predecessor.getQualifiedSelects().getValidityDate().isPresent(),
				"Can't create a IntervalPackingSelectsCte without a validity date present."
		);

		// we need an additional predecessor to unnest the validity date if it is a single column range
		List<QueryStep> predecessors = List.of();
		QueryStep actualPredecessor = predecessor;
		if (predecessor.getQualifiedSelects().getValidityDate().get().isSingleColumnRange()) {
			actualPredecessor = functionProvider.unnestValidityDate(predecessor, tables);
			predecessors = List.of(actualPredecessor);
		}

		Selects predecessorSelects = actualPredecessor.getQualifiedSelects();
		Selects selects = Selects.builder()
								 .ids(predecessorSelects.getIds())
								 .sqlSelects(intervalPackingSelects)
								 .build();

		return QueryStep.builder()
						.cteName(tables.cteName(ConceptCteStep.INTERVAL_PACKING_SELECTS))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(actualPredecessor.getCteName()))
						.groupBy(predecessorSelects.getIds().toFields())
						.predecessors(predecessors)
						.build();
	}

}
