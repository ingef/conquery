package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.ConceptSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;

public class IntervalPackingSelectsCte {

	public static QueryStep forSelect(
			QueryStep withAggregatedDaterange,
			ColumnDateRange daterange,
			SqlSelect select,
			SqlTables tables,
			SqlDialect sqlDialect
	) {
		List<QueryStep> predecessors = List.of(withAggregatedDaterange);
		QueryStep directPredecessor = withAggregatedDaterange;

		// we need an additional predecessor to unnest the validity date if it is a single column range
		if (sqlDialect.supportsSingleColumnRanges()) {
			String unnestCteName = tables.cteName(ConceptCteStep.UNNEST_DATE);
			directPredecessor = sqlDialect.getFunctionProvider().unnestDaterange(daterange, withAggregatedDaterange, unnestCteName);
			predecessors = List.of(withAggregatedDaterange, directPredecessor);
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

	static QueryStep forConnector(QueryStep predecessor, CQTableContext cqTableContext) {
		return create(
				predecessor,
				cqTableContext.getSqlSelects().stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
				cqTableContext.getConnectorTables(),
				cqTableContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

	static QueryStep forConcept(
			QueryStep predecessor,
			SqlTables tables,
			List<ConceptSqlSelects> sqlSelects,
			ConversionContext conversionContext
	) {
		return create(
				predecessor,
				sqlSelects.stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
				tables,
				conversionContext.getSqlDialect().getFunctionProvider()
		);
	}

	/**
	 * @param predecessor            The preceding query step which must contain an aggregated validity date.
	 * @param intervalPackingSelects {@link SqlSelect}s which will be part of the returned {@link QueryStep}.
	 * @return A {@link QueryStep} containing converted interval packing selects, like {@link EventDurationSumSelect}, {@link EventDateUnionSelect}, etc.
	 * Returns the given predecessor as is if the given list of interval packing selects is empty.
	 */
	private static QueryStep create(
			QueryStep predecessor,
			List<SqlSelect> intervalPackingSelects,
			SqlTables tables,
			SqlFunctionProvider functionProvider
	) {
		if (intervalPackingSelects.isEmpty()) {
			return predecessor;
		}

		Optional<ColumnDateRange> validityDate = predecessor.getQualifiedSelects().getValidityDate();
		Preconditions.checkArgument(validityDate.isPresent(), "Can't create a IntervalPackingSelectsCte without a validity date present.");

		// we need an additional predecessor to unnest the validity date if it is a single column range
		List<QueryStep> predecessors = List.of();
		QueryStep directPredecessor = predecessor;
		if (validityDate.get().isSingleColumnRange()) {
			String unnestCteName = tables.cteName(ConceptCteStep.UNNEST_DATE);
			directPredecessor = functionProvider.unnestDaterange(validityDate.get(), predecessor, unnestCteName);
			predecessors = List.of(directPredecessor);
		}

		Selects predecessorSelects = directPredecessor.getQualifiedSelects();
		Selects selects = Selects.builder()
								 .ids(predecessorSelects.getIds())
								 .sqlSelects(intervalPackingSelects)
								 .build();

		return QueryStep.builder()
						.cteName(tables.cteName(ConceptCteStep.INTERVAL_PACKING_SELECTS))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(directPredecessor.getCteName()))
						.groupBy(predecessorSelects.getIds().toFields())
						.predecessors(predecessors)
						.build();
	}

}
