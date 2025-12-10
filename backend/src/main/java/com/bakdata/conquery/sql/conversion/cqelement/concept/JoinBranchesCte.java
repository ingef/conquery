package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Record;
import org.jooq.TableLike;

/**
 * Joins the {@link ConceptCteStep#AGGREGATION_SELECT} with the interval packing branch for the aggregated validity date and optional validity date selects
 * {@link IntervalPackingSelectsCte} as well as optional additional predecessors.
 * <p>
 * Joining is optional - if a validity date is not present, the node is excluded from time aggregation or if there is no additional predecessor, no join will
 * take place. See {@link SumSqlAggregator} with distinct-by columns for an example of additional predecessors.
 *
 * <pre>
 *     {@code
 *     "join_branches" as (
 *  	  select
 *  	    coalesce("group_select"."pid", "interval_complete"."pid", "interval_packing_selects"."pid", "row_number_filtered"."pid") as "pid",
 *  	    "interval_complete"."concept_concept-1_validity_date",
 *  	    "interval_packing_selects"."event_duration_sum",
 *  	    "row_number_filtered"."sum_distinct-1"
 *  	  from "group_select"
 *  	    join "interval_complete"
 *  	      on "group_select"."pid" = "interval_complete"."pid"
 *  	    join "interval_packing_selects"
 *    	      on "group_select"."pid" = "interval_packing_selects"."pid"
 *  	    join "row_number_filtered"
 *  	      on "interval_complete"."pid" = "row_number_filtered"."pid"
 *  	)
 *     }
 * </pre>
 */
class JoinBranchesCte extends ConnectorCte {

	@Override
	protected ConceptCteStep cteStep() {
		return ConceptCteStep.JOIN_BRANCHES;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(tableContext.getPrevious());

		// validity date aggregation
		Optional<ColumnDateRange> validityDate;
		if (!tableContext.getConnectorTables().isWithIntervalPacking()) {
			validityDate = Optional.empty();
		}
		else {
			IntervalPackingContext intervalPackingContext = createIntervalPackingContext(tableContext);
			IntervalPacker intervalPacker = tableContext.getConversionContext().getSqlDialect().getIntervalPacker();
			QueryStep lastIntervalPackingStep = intervalPacker.aggregateAsValidityDate(intervalPackingContext);
			queriesToJoin.add(lastIntervalPackingStep);
			validityDate = lastIntervalPackingStep.getQualifiedSelects().getValidityDate();

			QueryStep intervalPackingSelectsStep = IntervalPackingSelectsCte.forConnector(lastIntervalPackingStep, tableContext);
			if (intervalPackingSelectsStep != lastIntervalPackingStep) {
				queriesToJoin.add(intervalPackingSelectsStep);
			}
		}

		// additional preceding tables
		tableContext.allSqlSelects().stream()
					.flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessor().stream())
					.forEach(queriesToJoin::add);

		Selects selects = collectSelects(validityDate, queriesToJoin, tableContext);
		TableLike<Record> fromTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, ConqueryJoinType.OUTER_JOIN, tableContext.getConversionContext());

		return QueryStep.builder()
						.selects(selects)
						.fromTable(fromTable)
						.predecessors(queriesToJoin);
	}

	private static IntervalPackingContext createIntervalPackingContext(CQTableContext tableContext) {
		Selects predcessorSelects = tableContext.getPrevious().getQualifiedSelects();
		return IntervalPackingContext.builder()
									 .ids(predcessorSelects.getIds())
									 .daterange(tableContext.getValidityDate().get())
									 .tables(tableContext.getConnectorTables())
									 .build();
	}

	private static Selects collectSelects(Optional<ColumnDateRange> validityDate, List<QueryStep> queriesToJoin, CQTableContext tableContext) {

		SqlIdColumns ids = QueryStepJoiner.coalesceIds(queriesToJoin);
		List<SqlSelect> mergedSqlSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		Optional<ColumnDateRange> stratificationDate = tableContext.getPrevious().getQualifiedSelects().getStratificationDate();

		return Selects.builder()
					  .ids(ids)
					  .stratificationDate(stratificationDate)
					  .validityDate(validityDate)
					  .sqlSelects(mergedSqlSelects)
					  .build();
	}

}
